package com.frostphyr.notiphy.twitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.firebase.messaging.Message;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import com.frostphyr.notiphy.EntryClient;
import com.frostphyr.notiphy.MediaType;
import com.frostphyr.notiphy.MessageDispatcher;
import com.frostphyr.notiphy.Transformer;
import com.frostphyr.notiphy.User;
import com.frostphyr.notiphy.UserEntry;
import com.frostphyr.notiphy.util.IOUtils;
import com.frostphyr.notiphy.util.TextUtils;

import jakarta.servlet.ServletContext;

public class TwitterClient implements EntryClient<TwitterEntry> {

	private static final Logger logger = LoggerFactory.getLogger(TwitterClient.class);
	
	private static final Transformer<Tweet, String> TWEET_DECODER = new TweetDecoder();
	private static final Transformer<Message.Builder, Tweet> MESSAGE_ENCODER = new TwitterMessageEncoder();
	
	private static final long MESSAGE_QUEUE_TIMEOUT_MS = 1000;
	
	private final Map<String, Set<UserEntry<TwitterEntry>>> entries = new HashMap<>();
	private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
	private ExecutorService pollExecutor;
	private ExecutorService mainExecutor;
	private Future<?> pollExecutorFuture;
	private MessageDispatcher messageDispatcher;
	private BasicClient client;
	private Authentication auth;
	private boolean modified;
	
	@Override
	public String getStatus() {
		return new StringBuilder()
				.append(client == null || client.isDone() ? "Stopped" : "Running")
				.append("; Users: ")
				.append(entries.size())
				.append("/")
				.append("5000")
				.append("; Queue size: ")
				.append(messageQueue.size())
				.toString();
	}
	
	@Override
	public void init(ServletContext context, ExecutorService mainExecutor, MessageDispatcher messageDispatcher) throws SAXException, IOException, ParserConfigurationException {
		this.mainExecutor = mainExecutor;
		this.messageDispatcher = messageDispatcher;
		pollExecutor = Executors.newSingleThreadExecutor();
		
		Document doc = IOUtils.parseDocument(context.getResourceAsStream("/WEB-INF/twitter_tokens.xml"));
		auth = new OAuth1(doc.getElementsByTagName("consumerKey").item(0).getTextContent(),
				doc.getElementsByTagName("consumerSecret").item(0).getTextContent(),
				doc.getElementsByTagName("accessToken").item(0).getTextContent(),
				doc.getElementsByTagName("accessTokenSecret").item(0).getTextContent());
	}
	
	@Override
	public void shutdown() {
		stop();
		pollExecutor.shutdownNow();
	}
	
	@Override
	public void clear() {
		synchronized (entries) {
			entries.clear();
		}
		modified = true;
	}
	
	@Override
	public void update() {
		if (modified) {
			modified = false;
			synchronized (entries) {
				if (entries.size() > 0) {
					reloadClient();
					if (pollExecutorFuture == null || pollExecutorFuture.isDone()) {
						pollExecutorFuture = pollExecutor.submit(pollLoop);
					}
				} else  {
					stop();
				}
			}
		}
	}
	
	private void stop() {
		if (pollExecutorFuture != null) {
			pollExecutorFuture.cancel(true);
			pollExecutorFuture = null;
		}
		
		if (client != null && !client.isDone()) {
			client.stop();
			client = null;
		}
		
		messageQueue.clear();
	}
	
	@Override
	public void add(UserEntry<TwitterEntry> entry) {
		synchronized (entries) {
			if (entries.computeIfAbsent(entry.getEntry().getUserId(), k -> new HashSet<>()).add(entry)) {
				modified = true;
			}
		}
	}

	@Override
	public void remove(UserEntry<TwitterEntry> entry) {
		synchronized (entries) {
			Set<UserEntry<TwitterEntry>> userEntries = entries.get(entry.getEntry().getUserId());
			if (userEntries != null) {
				if (userEntries.remove(entry)) {
					modified = true;
					if (userEntries.size() == 0) {
						entries.remove(entry.getEntry().getUserId());
					}
				}
			}
		}
	}
	
	private void reloadClient() {
		if (client != null && !client.isDone()) {
			client.stop();
		}
		
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		endpoint.addPostParameter(Constants.FOLLOW_PARAM, String.join(",", entries.keySet()));
		client = new ClientBuilder()
				.hosts(new HttpHosts(Constants.STREAM_HOST))
				.authentication(auth)
				.endpoint(endpoint)
				.processor(new StringDelimitedProcessor(messageQueue))
				.build();
		client.connect();
	}
	
	private void handleError() {
		synchronized (entries) {
			if (client != null && client.isDone() && entries.size() > 0) {
				logger.error("Client closed connection unexpectedly: " + client.getExitEvent().getMessage());
				reloadClient();
			}
		}
	}

	private void processMessage(String message) {
		Tweet tweet = TWEET_DECODER.transform(message);
		if (tweet != null) {
			Set<User> users = findMatches(tweet);
			if (users != null && users.size() > 0) {
				Message.Builder builder = MESSAGE_ENCODER.transform(tweet);
				for (User user : users) {
					messageDispatcher.dispatch(new MessageDispatcher.UserMessage(user, builder, tweet.toString()));
				}
			}
		}
	}

	private Set<User> findMatches(Tweet tweet) {
		Set<UserEntry<TwitterEntry>> userEntries = entries.get(tweet.getUserId());
		if (userEntries != null) {
			Set<User> users = new HashSet<>();
			for (UserEntry<TwitterEntry> e : userEntries) {
				if (isMatch(e.getEntry(), tweet)) {
					users.add(e.getUser());
				}
			}
			return users;
		}
		return null;
	}

	private boolean isMatch(TwitterEntry entry, Tweet tweet) {
		return TextUtils.contains(tweet.getText(), entry.getPhrases()) &&
				entry.getMediaType().isValidFor(tweet.getMedia() != null ? tweet.getMedia().getType() : MediaType.NONE);
	}
	
	private final Runnable pollLoop = () -> {
		while (true) {
			try {
				String message = messageQueue.poll(MESSAGE_QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
				synchronized (entries) {
					if (message != null) {
						processMessage(message);
					}
					
					if (client != null && client.isDone()) {
						mainExecutor.submit(() -> handleError());
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	};

}
