package com.frostphyr.notiphy;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

public class MessageDispatcher {
	
	private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);
	
	private static final int DELAY_MS = 60 * 1000;
	
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final List<UserMessage> messages = new LinkedList<>();
	private final UserManager userManager;
	private Future<?> future;
	
	public MessageDispatcher(UserManager userManager) {
		this.userManager = userManager;
	}
	
	private void start() {
		future = executor.scheduleWithFixedDelay(() -> {
			synchronized (messages) {
				FirebaseMessaging messaging = FirebaseMessaging.getInstance();
				for (ListIterator<UserMessage> it = messages.listIterator(); it.hasNext(); ) {
					if (attemptDispatch(messaging, it.next())) {
						logger.error("Successfully resent message");
						it.remove();
					}
				}
				
				if (messages.size() == 0) {
					future.cancel(false);
				}
			}
		}, 0, DELAY_MS, TimeUnit.MILLISECONDS);
	}
	
	public void shutdown() {
		executor.shutdownNow();
	}
	
	public void dispatch(UserMessage message) {
		if (!attemptDispatch(FirebaseMessaging.getInstance(), message)) {
			synchronized (messages) {
				messages.add(message);
				if (future == null || future.isDone()) {
					start();
				}
			}
		}
	}
	
	private boolean attemptDispatch(FirebaseMessaging messaging, UserMessage message) {
		String token = message.getUser().getToken();
		if (token == null) {
			return true;
		}
		
		try {
			messaging.send(message.builder.setToken(token).build());
			return true;
		} catch (FirebaseMessagingException e) {
			logger.error("Failed sending message: " + e.getMessage());
			logger.error(message.getData());
			if (e.getHttpResponse() != null) {
				int statusCode = e.getHttpResponse().getStatusCode();
				if (statusCode == 400 || statusCode == 404) {
					userManager.handleInvalidToken(message.getUser().getUid(), token);
					return true;
				} else if (statusCode != 500) {
					logger.error("Error sending Firebase Message", e);
				}
			} else {
				logger.error("Error sending Firebase Message", e);
			}
		}
		return false;
	}
	
	public static class UserMessage {
		
		private User user;
		private Message.Builder builder;
		private String data;
		
		public UserMessage(User user, Message.Builder builder, String data) {
			this.user = user;
			this.builder = builder;
			this.data = data;
		}
		
		public User getUser() {
			return user;
		}
		
		public Message.Builder getBuilder() {
			return builder;
		}
		
		public String getData() {
			return data;
		}
		
	}

}
