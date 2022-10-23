package com.frostphyr.notiphy;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class NotiphyContextListener implements ServletContextListener  {
	
	private static final Logger logger = LoggerFactory.getLogger(NotiphyContextListener.class);
	
	private UserManager userManager;
	private MessageDispatcher messageDispatcher;
	
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "notiphy-main");
		if (thread.isDaemon()) {
			thread.setDaemon(false);
		}
		if (thread.getPriority() != Thread.NORM_PRIORITY) {
			thread.setPriority(Thread.NORM_PRIORITY);
		}
		return thread;
	});
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			FirebaseApp.initializeApp(FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.getApplicationDefault())
					.build());
		} catch (IOException e) {
			logger.error("Error initializing Firebase", e);
			throw new RuntimeException(e);
		}
		
		userManager = new UserManager(executor);
		messageDispatcher = new MessageDispatcher(userManager);
		for (EntryType t : EntryType.values()) {
			try {
				t.getClient().init(sce.getServletContext(), executor, messageDispatcher);
			} catch (Exception e) {
				logger.error("Error initializing " + t.getClient().getClass().getSimpleName(), e);
				throw new RuntimeException(e);
			}
		}
		userManager.init();
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		userManager.shutdown();
		messageDispatcher.shutdown();
		for (EntryType t : EntryType.values()) {
			t.getClient().shutdown();
		}
		FirebaseApp.getInstance().delete();
	}

}
