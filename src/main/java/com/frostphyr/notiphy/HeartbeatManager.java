package com.frostphyr.notiphy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.websocket.PongMessage;
import javax.websocket.Session;

import com.frostphyr.notiphy.manager.MapStatTracker;
import com.frostphyr.notiphy.manager.StatTracker;

public class HeartbeatManager {
	
	private static final ByteBuffer DATA = ByteBuffer.wrap("heartbeat".getBytes());
	private static final long HEARTBEAT_DELAY_MS = 60 * 1000;
	private static final long MISSED_PONG_LIMIT = 3;
	
	private final Object LOCK = new Object();
	
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	private Map<String, Heartbeat> sessionHeartbeats = new HashMap<String, Heartbeat>();
	private StatTracker sessionTracker = new MapStatTracker("Sessions", sessionHeartbeats);
	
	public void start(Session session) {
		Heartbeat heartbeat = new Heartbeat(session);
		heartbeat.future = executor.scheduleAtFixedRate(heartbeat, HEARTBEAT_DELAY_MS, HEARTBEAT_DELAY_MS, TimeUnit.MILLISECONDS);
		synchronized (LOCK) {
			Heartbeat oldHeartbeat = sessionHeartbeats.put(session.getId(), heartbeat);
			if (oldHeartbeat != null) {
				oldHeartbeat.future.cancel(true);
			}
			sessionTracker.update();
		}
	}
	
	public void stop(Session session) {
		synchronized (LOCK) {
			Heartbeat heartbeat = sessionHeartbeats.remove(session.getId());
			if (heartbeat != null) {
				heartbeat.future.cancel(true);
			}
			sessionTracker.update();
		}
	}
	
	public void onPong(Session session, PongMessage message) {
		Heartbeat heartbeat;
		synchronized (LOCK) {
			heartbeat = sessionHeartbeats.get(session.getId());
		}
		
		if (heartbeat != null) {
			heartbeat.onPong(message);
		}
	}
	
	public StatTracker getSessionTracker() {
		return sessionTracker;
	}
	
	private class Heartbeat implements Runnable {
		
		private final Object LOCK = new Object();
		
		private ScheduledFuture<?> future;
		private Session session;
		private int missedPongs;
		private boolean pong = true;
		
		public Heartbeat(Session session) {
			this.session = session;
		}
		
		public void onPong(PongMessage message) {
			if (Arrays.equals(DATA.array(), message.getApplicationData().array())) {
				synchronized (LOCK) {
					pong = true;
					missedPongs = 0;
				}
			}
		}

		@Override
		public void run() {
			synchronized (LOCK) {
				if (!pong && ++missedPongs >= MISSED_PONG_LIMIT) {
					try {
						session.close();
					} catch (IOException e) {
					}
				} else {
					pong = false;
					try {
						session.getBasicRemote().sendPing(DATA);
					} catch (IOException e) {
					}
				}
			}
		}
		
	}

}
