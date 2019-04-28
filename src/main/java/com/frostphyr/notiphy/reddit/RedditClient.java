package com.frostphyr.notiphy.reddit;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.frostphyr.notiphy.EntryClient;
import com.frostphyr.notiphy.MessageDecoder;
import com.frostphyr.notiphy.MessageEncoder;
import com.frostphyr.notiphy.URLBuilder;
import com.frostphyr.notiphy.util.FixedLinkedStack;
import com.frostphyr.notiphy.util.IOUtils;

public class RedditClient extends EntryClient {

	private static final Logger logger = LogManager.getLogger(RedditClient.class);
	
	private static final String REDDIT_DOMAIN = "https://oauth.reddit.com";
	private static final String DELETED_SELFTEXT = "[deleted]";
	private static final String INITIAL_LIMIT_SUBREDDIT = "1";
	private static final String INITIAL_LIMIT_USER = "5";
	private static final double EXPIRES_IN_MULTIPLIER = 0.9;
	private static final long SLEEP_DURATION_MILLIS = 60 * 1000;
	private static final int POST_LIMIT = 100;
	private static final int NO_RESULT_COUNT_LIMIT = 3;
	
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	private Future<?> executorFuture;
	
	private RedditEntryCollection entries;
	private String authCredentials;
	private String accessToken;
	private String refreshToken;
	private long nextRefresh;
	
	public RedditClient(MessageDecoder<RedditMessage> messageDecoder, MessageEncoder<RedditMessage.Post> messageEncoder,
			RedditEntryCollection entries) {
		super(messageDecoder, messageEncoder, entries);
		
		this.entries = entries;
	}

	@Override
	public boolean init() {
		entries.addListener(() -> {
			if ((executorFuture == null || executorFuture.isDone()) && entries.getCount() > 0) {
				start();
			}
		});
		
		Document doc;
		try {
			doc = IOUtils.parseDocument("WebContent/WEB-INF/reddit_tokens.xml");
		} catch (SAXException | IOException | ParserConfigurationException e) {
			logger.error(e);
			return false;
		}
		
		String clientId = doc.getElementsByTagName("clientId").item(0).getTextContent();
		String secret = doc.getElementsByTagName("secret").item(0).getTextContent();
		try {
			authCredentials = Base64.getEncoder().encodeToString((clientId + ":" + secret).getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error(e);
			return false;
		}
		return true;
	}
	
	private void logHttpError(HttpURLConnection connection, String action) {
		try {
			logger.error("Failed " + action + " to Reddit server: " + connection.getResponseCode() + " " + connection.getResponseMessage());
		} catch (IOException e) {
		}
	}
	
	private void start() {
		executorFuture = executor.submit(executorLoop);
	}
	
	private boolean verifyAuthentication() {
		if (accessToken == null) {
			return authenticate();
		} else if (nextRefresh <= System.currentTimeMillis()) {
			 return refreshAccess();
		}
		return true;
	}
	
	private boolean authenticate() {
		try {
			HttpURLConnection connection = createAuthConnection();
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("grant_type", "client_credentials"));
			params.add(new BasicNameValuePair("duration", "permanent"));
			IOUtils.writeBodyParameters(connection, params);
			
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				processAuthBody(connection);
				checkRateLimit(connection);
				return true;
			} else {
				logHttpError(connection, "authenticating");
			}
		} catch (IOException e) {
			logger.error(e);
		}
		return false;
	}
	
	private boolean refreshAccess() {
		try {
			HttpURLConnection connection = createAuthConnection();
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("grant_type", "refresh_token"));
			params.add(new BasicNameValuePair("refresh_token", refreshToken));
			IOUtils.writeBodyParameters(connection, params);
			
			connection.connect();
			switch (connection.getResponseCode()) {
				case HttpURLConnection.HTTP_OK:
					processAuthBody(connection);
					checkRateLimit(connection);
					return true;
				case HttpURLConnection.HTTP_UNAUTHORIZED:
					return authenticate();
				default:
					logHttpError(connection, "refreshing");
			}
		} catch (IOException | JsonException e) {
			logger.error(e);
		}
		return false;
	}
	
	private void processAuthBody(HttpURLConnection connection) throws IOException {
		JsonObject obj = Json.createReader(connection.getInputStream()).readObject();
		accessToken = obj.getString("access_token");
		nextRefresh = (long) (obj.getInt("expires_in") * EXPIRES_IN_MULTIPLIER * 1000 + System.currentTimeMillis());
		if (obj.containsKey("refresh_token")) {
			refreshToken = obj.getString("refresh_token");
		}
	}
	
	private HttpURLConnection createConnection(String url) throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestProperty("User-Agent", "Notiphy/1.0");
		connection.setDoOutput(true);
		return connection;
	}
	
	private HttpURLConnection createAuthConnection() throws MalformedURLException, IOException {
		HttpURLConnection connection = createConnection("https://www.reddit.com/api/v1/access_token");
		connection.setRequestProperty("Authorization", "Basic " + authCredentials);
		connection.setRequestMethod("POST");
		return connection;
	}
	
	private void checkRateLimit(HttpURLConnection connection) {
		if (Objects.equals(connection.getHeaderField("X-Ratelimit-Remaining"), "0")) {
			try {
				Thread.sleep(Integer.parseInt(connection.getHeaderField("X-Ratelimit-Reset")) * 1000);
			} catch (NumberFormatException e) {
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
	
	private boolean processNewPosts(FixedLinkedStack<RedditPostIdentifier> latestPosts, int noResultCount, String initialLimit, URLBuilder builder) {
		try {
			RedditPostIdentifier latestPost = latestPosts.peek();
			builder.setDomain(REDDIT_DOMAIN)
					.addParameter("limit", latestPost != null ? Integer.toString(POST_LIMIT) : initialLimit);
			if (latestPost != null) {
				builder.addParameter("before", latestPost.getFullname());
			}
			HttpURLConnection connection = createConnection(builder.build());
			connection.addRequestProperty("Authorization", "bearer " + accessToken);
			connection.connect();
			
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				checkRateLimit(connection);
				
				RedditMessage message = decode(IOUtils.readString(connection.getInputStream()));
				if (message == null || message.getPosts().size() == 0) {
					if (latestPost != null) {
						if (++noResultCount >= NO_RESULT_COUNT_LIMIT) {
							if (validateLatestFullname(latestPosts)) {
								return true;
							} else {
								return processNewPosts(latestPosts, 0, initialLimit, builder
										.removeParameter("limit")
										.removeParameter("before"));
							}
						}
						return false;
					} else {
						return true;
					}
				} else {
					if (latestPost != null) {
						for (ListIterator<RedditMessage.Post> it = message.getPosts().listIterator(message.getPosts().size()); it.hasPrevious(); ) {
							RedditMessage.Post post = it.previous();
							if (!post.isPinned()) {
								latestPosts.push(post.getIdentifier());
								processMessage(post);
							}
						}
						
						if (message.getPosts().size() == POST_LIMIT) {
							processNewPosts(latestPosts, noResultCount, initialLimit, builder);
						}
					} else {
						for (RedditMessage.Post p : message.getPosts()) {
							if (!p.isPinned()) {
								latestPosts.push(p.getIdentifier());
								break;
							}
						}
					}
					return true;
				}
			}
		} catch (IOException e) {
		}
		return false;
	}
	
	private boolean validateLatestFullname(FixedLinkedStack<RedditPostIdentifier> latestPosts) throws IOException {
		RedditPostIdentifier postId;
		while ((postId = latestPosts.peek()) != null) {
			HttpURLConnection connection = createConnection(new URLBuilder()
					.setDomain(REDDIT_DOMAIN)
					.setPath("/r/" + postId.getSubreddit() + "/api/info.json")
					.addParameter("id", postId.getFullname())
					.build());
			connection.addRequestProperty("Authorization", "bearer " + accessToken);
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				checkRateLimit(connection);
				
				RedditMessage message = decode(IOUtils.readString(connection.getInputStream()));
				if (message != null && message.getPosts().size() > 0) {
					RedditMessage.Post post = message.getPosts().get(0);
					if (post.isPinned() || DELETED_SELFTEXT.equals(post.getText())) {
						latestPosts.pop();
					} else {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private Runnable executorLoop = () -> {
		while (verifyAuthentication()) {
			synchronized (entries) {
				if (entries.getCount() > 0) {
					entries.forEachUser((user, latestPosts, noResultCount) -> {
						return processNewPosts(latestPosts, noResultCount, INITIAL_LIMIT_USER, new URLBuilder()
								.setPath("/user/" + user + "/submitted.json")
								.addParameter("sort", "new"));
					});
					entries.forEachSubreddit((subreddit, latestPosts, noResultCount) -> {
						return processNewPosts(latestPosts, noResultCount, INITIAL_LIMIT_SUBREDDIT, new URLBuilder()
								.setPath("/r/" + subreddit + "/new.json"));
					});
				} else {
					Thread.currentThread().interrupt();
					return;
				}
			}
			
			try {
				Thread.sleep(SLEEP_DURATION_MILLIS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	};

}
