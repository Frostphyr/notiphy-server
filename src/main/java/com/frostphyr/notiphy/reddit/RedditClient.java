package com.frostphyr.notiphy.reddit;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.firebase.messaging.Message;

import com.frostphyr.notiphy.EntryClient;
import com.frostphyr.notiphy.MessageDispatcher;
import com.frostphyr.notiphy.Transformer;
import com.frostphyr.notiphy.User;
import com.frostphyr.notiphy.UserEntry;
import com.frostphyr.notiphy.util.FixedLinkedStack;
import com.frostphyr.notiphy.util.IOUtils;
import com.frostphyr.notiphy.util.TextUtils;
import com.frostphyr.notiphy.util.URLBuilder;

import jakarta.servlet.ServletContext;

public class RedditClient implements EntryClient<RedditEntry> {

	private static final Logger logger = LoggerFactory.getLogger(RedditClient.class);
	
	private static final Transformer<List<RedditPost>, String> POSTS_DECODER = new RedditPostsDecoder();
	private static final Transformer<Message.Builder, RedditPost> MESSAGE_ENCODER = new RedditMessageEncoder();
	
	private static final String REDDIT_DOMAIN = "https://oauth.reddit.com";
	private static final String SEARCH_PATH = "/subreddits/search.json";
	private static final String DELETED_TEXT = "[deleted]";
	private static final String REMOVED_TEXT = "[removed]";
	private static final String INITIAL_LIMIT_SUBREDDIT = "1";
	private static final String INITIAL_LIMIT_USER = "5";
	private static final double EXPIRES_IN_MULTIPLIER = 0.9;
	private static final long DELAY_MS = 60 * 1000;
	private static final int LATEST_POSTS_CAPACITY = 100;
	private static final int POST_LIMIT = 100;
	private static final int NO_RESULT_COUNT_LIMIT = 3;
	
	private final Map<String, Container> userEntries = new HashMap<>();
	private final Map<String, Container> subredditEntries = new HashMap<>();
	
	private ScheduledExecutorService executor;
	private Future<?> executorFuture;
	private MessageDispatcher messageDispatcher;
	
	private String authCredentials;
	private String accessToken;
	private long nextRefresh;
	
	@Override
	public String getStatus() {
		return new StringBuilder()
				.append(executorFuture == null || executorFuture.isDone() ? "Stopped" : "Running")
				.append("; Users: ")
				.append(userEntries.size())
				.append("; Subreddits: ")
				.append(subredditEntries.size())
				.toString();
	}

	@Override
	public void init(ServletContext context, ExecutorService mainExecutor, MessageDispatcher messageDispatcher) throws SAXException, IOException, ParserConfigurationException {
		this.messageDispatcher = messageDispatcher;
		executor = Executors.newSingleThreadScheduledExecutor();
		
		Document doc = IOUtils.parseDocument(context.getResourceAsStream("/WEB-INF/reddit_tokens.xml"));
		String clientId = doc.getElementsByTagName("clientId").item(0).getTextContent();
		String secret = doc.getElementsByTagName("secret").item(0).getTextContent();
		authCredentials = Base64.getEncoder().encodeToString((clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));
	}
	
	@Override
	public void shutdown() {
		executor.shutdownNow();
		executorFuture = null;
		userEntries.clear();
		subredditEntries.clear();
	}
	
	@Override
	public void clear() {
		userEntries.forEach((k, v) -> v.entries.clear());
		subredditEntries.forEach((k, v) -> v.entries.clear());
	}
	
	@Override
	public void update() {
		if ((executorFuture == null || executorFuture.isDone()) && 
				(userEntries.size() > 0 || subredditEntries.size() > 0)) {
			executorFuture = executor.scheduleWithFixedDelay(executorLoop, 0, DELAY_MS, TimeUnit.MILLISECONDS);
		} else if (executorFuture != null && !executorFuture.isDone() && 
				userEntries.size() == 0 && subredditEntries.size() == 0) {
			executorFuture.cancel(true);
			executorFuture = null;
		}
	}
	
	@Override
	public void add(UserEntry<RedditEntry> entry) {
		Map<String, Container> entryMap = getEntryMap(entry.getEntry().getEntryType());
		Container container = entryMap.get(entry.getEntry().getValue());
		if (container == null) {
			container = new Container();
			entryMap.put(entry.getEntry().getValue(), container);
		}
		container.entries.add(entry);
	}

	@Override
	public void remove(UserEntry<RedditEntry> entry) {
		Map<String, Container> entryMap = getEntryMap(entry.getEntry().getEntryType());
		Container container = entryMap.get(entry.getEntry().getValue());
		if (container != null) {
			container.entries.remove(entry);
			if (container.entries.size() == 0) {
				entryMap.remove(entry.getEntry().getValue());
			}
		}
	}
	
	private Map<String, Container> getEntryMap(RedditEntryType entryType) {
		return entryType == RedditEntryType.USER ? userEntries : subredditEntries;
	}
	
	private void logHttpError(HttpURLConnection connection, String message) {
		try {
			logger.error(new StringBuilder()
					.append("HTTP error (")
					.append(connection.getResponseCode())
					.append(' ')
					.append(connection.getResponseMessage())
					.append(") - ")
					.append(message)
					.toString());
		} catch (IOException e) {
			logger.error("Error logging HTTP error - " + message, e);
		}
	}
	
	private boolean verifyAuthentication() {
		if (accessToken == null || nextRefresh != 0 && nextRefresh <= System.currentTimeMillis()) {
			return authenticate();
		}
		return true;
	}
	
	private boolean authenticate() {
		try {
			HttpURLConnection connection = createAuthConnection();
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("grant_type", "client_credentials"));
			IOUtils.writeBodyParameters(connection, params);
			
			connection.connect();
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				processAuthBody(connection);
				checkRateLimit(connection);
				return true;
			} else {
				logHttpError(connection, "Authenticating");
			}
		} catch (IOException | JsonException e) {
			logger.error("Error authenticating", e);
		}
		return false;
	}
	
	private void processAuthBody(HttpURLConnection connection) throws IOException {
		JsonObject obj = Json.createReader(connection.getInputStream()).readObject();
		accessToken = obj.getString("access_token");
		nextRefresh = (long) (obj.getInt("expires_in") * EXPIRES_IN_MULTIPLIER * 1000 + System.currentTimeMillis());
	}
	
	private HttpURLConnection createConnection(String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestProperty("User-Agent", "Notiphy/1.0");
		connection.setDoOutput(true);
		return connection;
	}
	
	private HttpURLConnection createAuthConnection() throws IOException {
		HttpURLConnection connection = createConnection("https://www.reddit.com/api/v1/access_token");
		connection.setRequestProperty("Authorization", "Basic " + authCredentials);
		connection.setRequestMethod("POST");
		return connection;
	}
	
	private void checkRateLimit(HttpURLConnection connection) {
		if (Objects.equals(connection.getHeaderField("X-Ratelimit-Remaining"), "0")) {
			try {
				Thread.sleep(Long.parseLong(connection.getHeaderField("X-Ratelimit-Reset")) * 1000);
			} catch (NumberFormatException e) {
				//Ignore
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
	
	private void processNewPosts(RedditEntryType entryType, Container container, String initialLimit, URLBuilder builder) {
		try {
			RedditPostIdentifier latestPost = container.latestPosts.peek();
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
				
				if (!connection.getURL().getPath().equals(SEARCH_PATH)) {
					List<RedditPost> posts = POSTS_DECODER.transform(IOUtils.readString(connection.getInputStream()));
					if (posts == null || posts.size() == 0) {
						if (latestPost != null) {
							if (++container.noResultCount >= NO_RESULT_COUNT_LIMIT) {
								if (validateLatestFullname(container.latestPosts)) {
									container.noResultCount = 0;
								} else {
									container.noResultCount = 0;
									processNewPosts(entryType, container, initialLimit, builder
											.removeParameter("limit")
											.removeParameter("before"));
								}
							} else {
								container.noResultCount++;
							}
						} else {
							container.noResultCount = 0;
						}
					} else {
						if (latestPost != null) {
							for (ListIterator<RedditPost> it = posts.listIterator(posts.size()); it.hasPrevious(); ) {
								RedditPost post = it.previous();
								if (!post.isPinned()) {
									container.latestPosts.push(post.getIdentifier());
									processPost(entryType, post);
								}
							}
							
							if (posts.size() == POST_LIMIT) {
								processNewPosts(entryType, container, initialLimit, builder);
							}
						} else {
							for (RedditPost p : posts) {
								if (!p.isPinned()) {
									container.latestPosts.push(p.getIdentifier());
									break;
								}
							}
						}
						container.noResultCount = 0;
					}
				}
			} else {
				logHttpError(connection, "Processing new posts: " + connection.getURL());
			}
		} catch (IOException e) {
			logger.error("Error processing new posts", e);
		}
		container.noResultCount++;
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
				
				if (!connection.getURL().getPath().equals(SEARCH_PATH)) {
					List<RedditPost> posts = POSTS_DECODER.transform(IOUtils.readString(connection.getInputStream()));
					
					if (posts != null && posts.size() > 0) {
						RedditPost post = posts.get(0);
						if (post.isPinned() || !post.isRobotIndexable() ||
								DELETED_TEXT.equals(post.getText()) || REMOVED_TEXT.equals(post.getText())) {
							latestPosts.pop();
						} else {
							return true;
						}
					}
				}
			} else {
				logHttpError(connection, "Validating latest fullname: " + connection.getURL());
			}
		}
		return false;
	}
	
	private void processPost(RedditEntryType entryType, RedditPost post) {
		Set<User> users = findMatches(entryType, post);
		if (users != null && users.size() > 0) {
			Message.Builder builder = MESSAGE_ENCODER.transform(post);
			for (User user : users) {
				messageDispatcher.dispatch(new MessageDispatcher.UserMessage(user, builder, post.toString()));
			}
		}
	}
	
	private Set<User> findMatches(RedditEntryType entryType, RedditPost post) {
		Map<String, Container> entries = getEntryMap(entryType);
		Container container = entries.get(entryType == RedditEntryType.USER ?
				post.getUser().toLowerCase() : post.getIdentifier().getSubreddit().toLowerCase());
		if (container != null) {
			Set<User> users = new HashSet<>();
			for (UserEntry<RedditEntry> entry : container.entries) {
				if (isMatch(entry.getEntry(), post) && entry.getUser().getToken() != null) {
					users.add(entry.getUser());
				}
			}
			return users;
		}
		return null;
	}
	
	private boolean isMatch(RedditEntry entry, RedditPost post) {
		return TextUtils.contains(new String[] {post.getTitle(), post.getText()}, entry.getPhrases()) &&
				isPostTypeMatch(entry.getPostType(), post);
	}
	
	private boolean isPostTypeMatch(RedditPostType postType, RedditPost post) {
		return postType == RedditPostType.ANY || 
				(postType == RedditPostType.TEXT && post.getText() != null) ||
				(postType == RedditPostType.LINK && post.getMedia() != null);
	}
	
	private final Runnable executorLoop = () -> {
		try {
			if (verifyAuthentication()) {
				synchronized (userEntries) {
					if (userEntries.size() > 0) {
						for (Map.Entry<String, Container> e : userEntries.entrySet()) {
							processNewPosts(RedditEntryType.USER, e.getValue(), INITIAL_LIMIT_USER, new URLBuilder()
									.setPath("/user/" + e.getKey() + "/submitted.json")
									.addParameter("sort", "new")
									.addParameter("raw_json", "1"));
						}
						
					}
				}
				synchronized (subredditEntries) {
					if (subredditEntries.size() > 0) {
						for (Map.Entry<String, Container> e : subredditEntries.entrySet()) {
							processNewPosts(RedditEntryType.SUBREDDIT, e.getValue(), INITIAL_LIMIT_SUBREDDIT, new URLBuilder()
									.setPath("/r/" + e.getKey() + "/new.json")
									.addParameter("raw_json", "1"));
						}
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Error in Reddit executor loop", t);
		}
	};
	
	private static class Container {
		
		private Set<UserEntry<RedditEntry>> entries = new HashSet<>();
		private FixedLinkedStack<RedditPostIdentifier> latestPosts = new FixedLinkedStack<>(LATEST_POSTS_CAPACITY);
		private int noResultCount;
		
	}

}
