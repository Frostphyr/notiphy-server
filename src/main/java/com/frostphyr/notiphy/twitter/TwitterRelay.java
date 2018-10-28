package com.frostphyr.notiphy.twitter;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.websocket.Session;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.frostphyr.notiphy.Entry;
import com.frostphyr.notiphy.EntryOperation;
import com.frostphyr.notiphy.EntryRelay;
import com.frostphyr.notiphy.EntryType;
import com.frostphyr.notiphy.IOUtils;
import com.frostphyr.notiphy.MediaType;
import com.google.common.base.Joiner;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class TwitterRelay implements EntryRelay {
	
	private static final Object LOCK = new Object();
	
	private static final String MESSAGE_RESTART = "NotiphyRestart";
	
	private static final Logger logger = LogManager.getLogger(TwitterRelay.class);
	
	private Map<String, Set<TwitterEntry>> sessionEntries = new HashMap<>();
	private Map<String, Set<SessionEntry>> userEntries = new HashMap<>();
	
	private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
	private ExecutorService executor;
	
	private Authentication auth;
	
	@Override
	public boolean init() {
		Document doc;
		try {
			doc = IOUtils.parseDocument("WebContent/WEB-INF/twitter_tokens.xml");
		} catch (SAXException | IOException | ParserConfigurationException e) {
			logger.error(e);
			return false;
		}
		String consumerKey = doc.getElementsByTagName("consumerKey").item(0).getTextContent();
		String consumerSecret = doc.getElementsByTagName("consumerSecret").item(0).getTextContent();
		String accessToken = doc.getElementsByTagName("accessToken").item(0).getTextContent();
		String accessTokenSecret = doc.getElementsByTagName("accessTokenSecret").item(0).getTextContent();
		auth = new OAuth1(consumerKey, consumerSecret, accessToken, accessTokenSecret);
		return true;
	}

	@Override
	public void performOperations(Session session, EntryOperation[] operations) {
		synchronized (LOCK) {
			boolean addModified = false;
			boolean removeModified = false;
			if (operations[EntryOperation.REMOVE] != null) {
				removeModified = remove(session, operations[EntryOperation.REMOVE].getEntries());
			}
			if (operations[EntryOperation.ADD] != null) {
				addModified = add(session, operations[EntryOperation.ADD].getEntries());
			}
			
			if (addModified && executor == null) {
				start();
			} else if (addModified || removeModified) {
				messageQueue.offer(MESSAGE_RESTART);
			}
		}
	}

	@Override
	public void removeAll(Session session) {
		synchronized (LOCK) {
			Set<TwitterEntry> entries = sessionEntries.get(session.getId());
			if (entries != null) {
				boolean modified = false;
				for (TwitterEntry e : entries) {
					TwitterEntry te = (TwitterEntry) e;
					if (userEntries.get(te.getUserId()).remove(new SessionEntry(session, te))) {
						modified = true;
					}
				}
				sessionEntries.remove(session.getId());
				
				if (modified) {
					messageQueue.offer(MESSAGE_RESTART);
				}
			}
		}
	}
	
	private boolean add(Session session, List<Entry> entries) {
		Set<TwitterEntry> se = getOrCreate(sessionEntries, session.getId());
		boolean modified = false;
		for (Entry e : entries) {
			TwitterEntry te = (TwitterEntry) e;
			if (se.add(te)) {
				Set<SessionEntry> ue = getOrCreate(userEntries, te.getUserId());
				ue.add(new SessionEntry(session, te));
				modified = true;
			}
		}
		return modified;
	}

	private boolean remove(Session session, List<Entry> entries) {
		Set<TwitterEntry> entrySet = sessionEntries.get(session.getId());
		if (entrySet != null) {
			boolean modified = false;
			for (Entry e : entries) {
				if (entrySet.remove(e)) {
					TwitterEntry te = (TwitterEntry) e;
					if (userEntries.get(te.getUserId()).remove(new SessionEntry(session, te))) {
						modified = true;
					}
				}
			}
			return modified;
		}
		return false;
	}
	
	private void start() {
		executor = Executors.newSingleThreadExecutor();
		executor.execute(executorLoop);
	}
	
	private Client createClient() {
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		synchronized (LOCK) {
			endpoint.addPostParameter(Constants.FOLLOW_PARAM, Joiner.on(',').join(userEntries.keySet()));
		}
		
		return new ClientBuilder()
				.hosts(new HttpHosts(Constants.STREAM_HOST))
				.authentication(auth)
				.endpoint(endpoint)
				.processor(new StringDelimitedProcessor(messageQueue))
				.build();
	}
	
	private void processMessage(String encodedMessage) {
		Message message = decodeMessage(encodedMessage);
		if (message != null) {
			Set<SessionEntry> entries = userEntries.get(message.userId);
			String forward = null;
			for (SessionEntry e : entries) {
				if (contains(message.text, e.entry.getPhrases()) && e.entry.getMediaType().isValidFor(message.mediaType)) {
					if (forward == null) {
						forward = createForwardMessage(message);
					}
					
					try {
						e.session.getBasicRemote().sendText(forward);
					} catch (IOException ex) {
					}
				}
			}
		}
	}
	
	private Message decodeMessage(String encodedMessage) {
		try {
			Message message = new Message();
			JsonReader reader = Json.createReader(new StringReader(encodedMessage));
			JsonObject obj = reader.readObject();
			message.id = obj.getString("id_str");
			JsonObject user = obj.getJsonObject("user");
			message.userId = user.getString("id_str");
			message.createdAt = obj.getString("created_at");
			message.username = user.getString("screen_name");
			if (obj.containsKey("extended_tweet")) {
				message.text = getDisplayText("full_text", obj.getJsonObject("extended_tweet"));
			} else {
				message.text = getDisplayText("text", obj);
			}
			
			message.mediaType = MediaType.NONE;
			if (obj.containsKey("extended_entities")) {
				JsonObject entities = obj.getJsonObject("extended_entities");
				if (entities.containsKey("media")) {
					JsonArray mediaArray = entities.getJsonArray("media");
					message.media = new Media[mediaArray.size()];
					for (int i = 0; i < mediaArray.size(); i++) {
						JsonObject media = mediaArray.getJsonObject(i);
						message.media[i] = new Media();
						message.media[i].thumbnailUrl = media.getString("media_url");
						message.media[i].type = getMediaType(media.getString("type"));
						if (message.media[i].type == MediaType.IMAGE) {
							if (message.mediaType == MediaType.NONE) {
								message.mediaType = MediaType.IMAGE;
							} else if (message.mediaType == MediaType.VIDEO) {
								message.mediaType = MediaType.ANY;
							}
						} else if (message.media[i].type == MediaType.VIDEO) {
							if (message.mediaType == MediaType.NONE) {
								message.mediaType = MediaType.VIDEO;
							} else if (message.mediaType == MediaType.IMAGE) {
								message.mediaType = MediaType.ANY;
							}
							
							JsonArray variants = media.getJsonObject("video_info").getJsonArray("variants");
							int maxBitrate = -1;
							for (int j = 0; j < variants.size(); j++) {
								JsonObject variant = variants.getJsonObject(j);
								if (variant.containsKey("bitrate")) {
									int bitrate = variant.getInt("bitrate");
									if (bitrate > maxBitrate) {
										message.media[i].url = variant.getString("url");
									}
								}
							}
						}
					}
				}
			}
			return message;
		} catch (JsonException | NullPointerException e) {
			logger.error(e.getMessage() + ":" + encodedMessage);
			return null;
		}
	}
	
	private static String getDisplayText(String textName, JsonObject obj) {
		String text = obj.getString(textName);
		if (obj.containsKey("display_text_range")) {
			JsonArray range = obj.getJsonArray("display_text_range");
			text.substring(range.getInt(0), range.getInt(1));
		}
		return text;
	}
	
	private static String createForwardMessage(Message message) {
		JsonObjectBuilder builder = Json.createObjectBuilder()
				.add("type", EntryType.TWITTER.toString())
				.add("id", message.id)
				.add("createdAt", message.createdAt)
				.add("username", message.username)
				.add("text", message.text);
				
		if (message.media != null) {
			JsonArrayBuilder mediaArrayBuilder = Json.createArrayBuilder();
			for (Media m : message.media) {
				JsonObjectBuilder mediaBuilder = Json.createObjectBuilder()
						.add("url", m.url)
						.add("type", m.type.toString());
				if (m.thumbnailUrl != null) {
					mediaBuilder.add("thumbnailUrl", m.thumbnailUrl);
				}
				
				mediaArrayBuilder.add(mediaBuilder);
			}
			builder.add("media", mediaArrayBuilder);
		}
		
		return builder.build().toString();
	}
	
	private static boolean contains(String text, String[] phrases) {
		for (String s : phrases) {
			if (!StringUtils.containsIgnoreCase(text, s)) {
				return false;
			}
		}
		return true;
	}
	
	private static MediaType getMediaType(String type) {
		switch (type) {
			case "photo":
				return MediaType.IMAGE;
			case "video":
			case "animated_gif":
				return MediaType.VIDEO;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	private static <K, T> Set<T> getOrCreate(Map<K, Set<T>> map, K key) {
		Set<T> set = map.get(key);
		if (set == null) {
			set = new HashSet<>();
			map.put(key, set);
		}
		return set;
	}
	
	private static class SessionEntry {
		
		private Session session;
		private TwitterEntry entry;
		
		public SessionEntry(Session session, TwitterEntry entry) {
			this.session = session;
			this.entry = entry;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof SessionEntry) {
				SessionEntry e = (SessionEntry) o;
				return e.session.getId().equals(session.getId()) &&
						e.entry.equals(entry);
			}
			return false;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(session.getId(), entry);
		}
		
	}
	
	private class Message {
		
		private String id;
		private String userId;
		private String createdAt;
		private String username;
		private String text;
		private MediaType mediaType;
		private Media[] media;
		
	}
	
	private class Media {
		
		private MediaType type;
		private String url;
		private String thumbnailUrl;
		
	}
	
	private Runnable executorLoop = new Runnable() {

		@Override
		public void run() {
			while (true) {
				Client client = createClient();
				client.connect();
				while (!client.isDone()) {
					try {
						String message = messageQueue.take();
						if (message.equals(MESSAGE_RESTART)) {
							client.stop();
							client = createClient();
							client.connect();
						} else {
							processMessage(message);
						}
					} catch (InterruptedException e) {
					}
				}
			}
		}
		
	};

}
