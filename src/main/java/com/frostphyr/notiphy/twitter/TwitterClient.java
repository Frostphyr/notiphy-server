package com.frostphyr.notiphy.twitter;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.frostphyr.notiphy.EntryClient;
import com.frostphyr.notiphy.MessageDecoder;
import com.frostphyr.notiphy.MessageEncoder;
import com.frostphyr.notiphy.util.IOUtils;
import com.google.common.base.Joiner;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

public class TwitterClient extends EntryClient {

	private static final Logger logger = LogManager.getLogger(TwitterClient.class);
	
	private static final long MESSAGE_QUEUE_TIMEOUT_MS = 1000;
	
	private static final String MESSAGE_RESTART = "NotiphyRestart";
	private static final String MESSAGE_SHUTDOWN = "NotiphyShutdown";
	
	private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
	private ExecutorService executor;
	private Authentication auth;
	private TwitterEntryCollection entries;
	
	public TwitterClient(MessageDecoder<TwitterMessage> messageDecoder, MessageEncoder<TwitterMessage> messageEncoder,
			TwitterEntryCollection entries) {
		super(messageDecoder, messageEncoder, entries);
		
		this.entries = entries;
	}
	
	@Override
	public boolean init() {
		entries.addListener(() -> {
			if (executor != null && !executor.isShutdown()) {
				messageQueue.offer(MESSAGE_RESTART);
			} else if (entries.getCount() > 0) {
				start();
			} else {
				messageQueue.offer(MESSAGE_SHUTDOWN);
			}
		});
		
		Document doc;
		try {
			doc = IOUtils.parseDocument("WebContent/WEB-INF/twitter_tokens.xml");
		} catch (SAXException | IOException | ParserConfigurationException e) {
			logger.error(e);
			return false;
		}
		auth = new OAuth1(doc.getElementsByTagName("consumerKey").item(0).getTextContent(),
				doc.getElementsByTagName("consumerSecret").item(0).getTextContent(),
				doc.getElementsByTagName("accessToken").item(0).getTextContent(),
				doc.getElementsByTagName("accessTokenSecret").item(0).getTextContent());
		return true;
	}
	
	private void start() {
		executor = Executors.newSingleThreadExecutor();
		executor.execute(executorLoop);
	}
	
	private Client createClient() {
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
		synchronized (entries) {
			endpoint.addPostParameter(Constants.FOLLOW_PARAM, Joiner.on(',').join(entries.getUsers()));
		}
		
		return new ClientBuilder()
				.hosts(new HttpHosts(Constants.STREAM_HOST))
				.authentication(auth)
				.endpoint(endpoint)
				.processor(new StringDelimitedProcessor(messageQueue))
				.build();
	}
	
	private Runnable executorLoop = () -> {
		while (true) {
			Client client = createClient();
			client.connect();
			while (!client.isDone()) {
				try {
					String message = messageQueue.poll(MESSAGE_QUEUE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
					if (message != null) {
						if (message.equals(MESSAGE_RESTART)) {
							client.stop();
							client = createClient();
							client.connect();
						} else if (message.equals(MESSAGE_SHUTDOWN)) {
							synchronized (entries) {
								if (messageQueue.size() == 0) {
									client.stop();
									executor.shutdown();
								}
							}
							return;
						} else {
							processEncodedMessage(message);
						}
					}
				} catch (InterruptedException e) {
				}
			}
		}
	};

}
