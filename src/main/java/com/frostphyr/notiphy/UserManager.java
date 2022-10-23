package com.frostphyr.notiphy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;

public class UserManager {
	
	private static final Logger logger = LoggerFactory.getLogger(UserManager.class);
	
	private static final int MAX_ENTRIES = 25;
	private static final long STALE_TOKEN_WINDOW_MS = 5184000000L;
	private static final long STALE_TOKEN_CHECK_DELAY_DAYS = 30;
	
	private Map<String, User> users = new HashMap<>();
	private ListenerRegistration infoListenerRegistration;
	private ListenerRegistration entryListenerRegistration;
	private ScheduledExecutorService verificationExecutor;
	private ExecutorService executor;
	
	public UserManager(ExecutorService executor) {
		this.executor = executor;
	}
	
	public void init() {
		addListeners();
		scheduleVerification();
	}
	
	public void shutdown() {
		infoListenerRegistration.remove();
		entryListenerRegistration.remove();
		verificationExecutor.shutdownNow();
	}
	
	public void handleInvalidToken(String uid, String token) {
		logger.error("Invalid token: " + uid);
		Firestore db = FirestoreClient.getFirestore();
		DocumentReference docRef = db.collection("users").document(uid).collection("info").document("token");
		ApiFuture<Void> future = db.runTransaction((Transaction.Function<Void>)(transaction) -> {
			DocumentSnapshot doc = transaction.get(docRef).get();
			if (token.equals(doc.getString("value"))) {
				transaction.delete(docRef);
			} else {
				throw new RuntimeException();
			}
			return null;
		});
		
		ApiFutures.addCallback(future, new ApiFutureCallback<Void>() {
			
			@Override
			public void onSuccess(Void result) {
				User user = users.get(uid);
				if (user != null && token.equals(user.getToken())) {
					removeUser(uid);
					updateClients();
				}
			}

			@Override
			public void onFailure(Throwable t) {
			}
			
		}, executor);
	}
	
	private void addListeners() {
		Firestore db = FirestoreClient.getFirestore();
		infoListenerRegistration = db.collectionGroup("info").addSnapshotListener(executor, (snapshots, e) -> {
			if (e != null) {
				logger.error("Error in info snapshot listener", e);
				reset();
				return;
			}

			long currentTime = System.currentTimeMillis();
			for (DocumentChange dc : snapshots.getDocumentChanges()) {
				DocumentSnapshot doc = dc.getDocument();
				if (doc.getId().equals("token")) {
					String uid = parseUid(doc.getReference().getPath());
					String token = getString(doc, "value");
					Long timestamp = getLong(doc, "timestamp");
					if (token != null && timestamp != null) {
						switch (dc.getType()) {
							case ADDED:
							case MODIFIED:
								if (!addOrUpdateUser(uid, token, timestamp, currentTime)) {
									doc.getReference().delete();
								}
								break;
							case REMOVED:
								removeUser(uid);
								break;
						}
					}
				}
			}
			updateClients();
		});

		entryListenerRegistration = db.collectionGroup("entries").addSnapshotListener(executor, (snapshots, e) -> {
			if (e != null) {
				logger.error("Error in entries snapshot listener", e);
				reset();
				return;
			}

			String userUid = null;
			User user = null;
			for (DocumentChange dc : snapshots.getDocumentChanges()) {
				DocumentSnapshot doc = dc.getDocument();
				String newUserUid = parseUid(doc.getReference().getPath());
				if (!newUserUid.equals(userUid)) {
					userUid = newUserUid;
					user = users.get(userUid);
				}
				
				if (user != null) {
					switch (dc.getType()) {
						case ADDED:
							addEntry(doc, user);
							break;
						case MODIFIED:
							updateEntry(doc, user);
							break;
						case REMOVED:
							removeEntry(doc, user);
							break;
					}
				}
			}
			updateClients();
		});
	}
	
	private void scheduleVerification() {
		verificationExecutor = Executors.newScheduledThreadPool(1);
		verificationExecutor.scheduleAtFixedRate(() -> executor.submit(() -> {
			FirebaseAuth auth = FirebaseAuth.getInstance();
			Firestore db = FirestoreClient.getFirestore();
			long currentTime = System.currentTimeMillis();
			for (Iterator<Map.Entry<String, User>> it = users.entrySet().iterator(); it.hasNext(); ) {
				User user = it.next().getValue();
				try {
					auth.getUser(user.getUid());
					if (user.getTimestamp() != -1 && currentTime - user.getTimestamp() >= STALE_TOKEN_WINDOW_MS) {
						removeUser(user);
						it.remove();
					}
				} catch (FirebaseAuthException e) {
					db.recursiveDelete(db.document("users/" + user.getUid()));
					removeUser(user);
					it.remove();
				}
				
			}
			updateClients();
		}), STALE_TOKEN_CHECK_DELAY_DAYS, STALE_TOKEN_CHECK_DELAY_DAYS, TimeUnit.DAYS);
	}
	
	private void reset() {
		infoListenerRegistration.remove();
		entryListenerRegistration.remove();
		users.clear();
		for (EntryType t : EntryType.values()) {
			t.getClient().clear();
		}
		addListeners();
	}
	
	private void fetchEntries(String uid) {
		Firestore db = FirestoreClient.getFirestore();
		ApiFuture<QuerySnapshot> future = db.collection("users").document(uid).collection("entries").get();
		ApiFutures.addCallback(future, new ApiFutureCallback<QuerySnapshot>() {
			
			@Override
			public void onSuccess(QuerySnapshot result) {
				User user = users.get(uid);
				if (user != null) {
					result.forEach(doc -> addEntry(doc, user));
					updateClients();
				}
			}

			@Override
			public void onFailure(Throwable t) {
			}
			
		}, executor);
	}

	private boolean addOrUpdateUser(String uid, String token, long timestamp, long currentTime) {
		if (currentTime - timestamp >= STALE_TOKEN_WINDOW_MS) {
			removeUser(uid);
			return false;
		}
		
		User user = users.putIfAbsent(uid, new User(uid, token, timestamp));
		if (user != null) {
			user.updateToken(token, timestamp);
		} else {
			fetchEntries(uid);
		}
		return true;
	}
	
	private void removeUser(String uid) {
		User user = users.remove(uid);
		if (user != null) {
			removeUser(user);
		}
	}
	
	private void removeUser(User user) {
		user.invalidateToken();
		for (UserEntry<?> e : user.getEntries().values()) {
			removeEntry(e.getEntry().getType().getClient(), e);
		}
	}

	private void addEntry(DocumentSnapshot doc, User user) {
		if (Boolean.TRUE.equals(getBoolean(doc, "active"))) {
			String entryUid = doc.getId();
			UserEntry<?> oldEntry = user.getEntries().get(entryUid);
			if (oldEntry != null) {
				Entry entry = parseEntry(doc);
				if (entry != null) {
					if (!entry.equals(oldEntry.getEntry())) {
						UserEntry<?> userEntry = new UserEntry<>(user, entry, entryUid);
						user.getEntries().put(entryUid, userEntry);
						addEntry(entry.getType().getClient(), userEntry);
						removeEntry(entry.getType().getClient(), userEntry);
					}
				} else {
					doc.getReference().delete();
				}
			} else {
				if (user.getEntries().size() >= MAX_ENTRIES) {
					doc.getReference().delete();
				} else {
					Entry entry = parseEntry(doc);
					if (entry != null) {
						UserEntry<?> userEntry = new UserEntry<>(user, entry, entryUid);
						user.getEntries().put(entryUid, userEntry);
						addEntry(entry.getType().getClient(), userEntry);
					} else {
						doc.getReference().delete();
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends Entry> void addEntry(EntryClient<T> client, UserEntry<?> entry) {
		client.add((UserEntry<T>) entry);
	}

	private void updateEntry(DocumentSnapshot doc, User user) {
		String entryUid = doc.getId();
		UserEntry<?> oldUserEntry = user.getEntries().get(entryUid);
		if (oldUserEntry != null) {
			if (Boolean.FALSE.equals(getBoolean(doc, "active"))) {
				removeEntry(doc, user);
			} else {
				Entry entry = parseEntry(doc);
				if (entry != null) {
					UserEntry<?> userEntry = new UserEntry<>(user, entry, entryUid);
					user.getEntries().put(entryUid, userEntry);
					removeEntry(entry.getType().getClient(), oldUserEntry);
					addEntry(entry.getType().getClient(), userEntry);
				} else {
					doc.getReference().delete();
				}
			}
		} else {
			addEntry(doc, user);
		}
	}

	private void removeEntry(DocumentSnapshot doc, User user) {
		UserEntry<?> userEntry = user.getEntries().remove(doc.getId());
		if (userEntry != null) {
			removeEntry(userEntry.getEntry().getType().getClient(), userEntry);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private static <T extends Entry> void removeEntry(EntryClient<T> client, UserEntry<?> entry) {
		client.remove((UserEntry<T>) entry);
	}
	private void updateClients() {
		for (EntryType t : EntryType.values()) {
			t.getClient().update();
		}
	}

	private static Entry parseEntry(DocumentSnapshot doc) {
		String type = getString(doc, "type");
		if (type != null) {
			try {
				Entry entry = doc.toObject(EntryType.valueOf(type).getEntryClass());
				return entry != null && entry.validate() ? entry : null;
			} catch (IllegalArgumentException e) {
				//Ignore
			}
		}
		return null;
	}

	private static String getString(DocumentSnapshot doc, String field) {
		try {
			return doc.getString(field);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static Boolean getBoolean(DocumentSnapshot doc, String field) {
		try {
			return doc.getBoolean(field);
		} catch (RuntimeException e) {
			return null;
		}
	}
	
	private static Long getLong(DocumentSnapshot doc, String field) {
		try {
			return doc.getLong(field);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static String parseUid(String path) {
		return path.substring(6, path.indexOf("/", 6));
	}

}
