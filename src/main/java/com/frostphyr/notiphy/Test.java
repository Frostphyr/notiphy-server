package com.frostphyr.notiphy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;

import com.frostphyr.notiphy.reddit.RedditMessageEncoder;
import com.frostphyr.notiphy.reddit.RedditPost;
import com.frostphyr.notiphy.reddit.RedditPostsDecoder;
import com.frostphyr.notiphy.util.IOUtils;

public class Test {

	public static void main(String[] args) throws FirebaseMessagingException, MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL("https://api.reddit.com/api/info/?id=t3_wybf17").openConnection();
		connection.setRequestProperty("User-Agent", "Notiphy/1.0");
		connection.setDoOutput(true);
		List<RedditPost> posts = new RedditPostsDecoder().transform(IOUtils.readString(connection.getInputStream()));
		//System.out.println(posts.get(0).getText());
		
		String token = "fZ2GxhN4RYOyumORFU0UKU:APA91bE-fLQwh1QtnZeeHEEizTqtiXOF8IBmhDKxSL7ezTVF6ISRS6FTMUc_AoTerCNcssCBepbU_K36_bIXp4CySVlQgKp9a8HE7BwoGkSq2WGflcUa1-Q0Z9KnWlJkw1ItXJCam3g_";
		try {
			FirebaseApp.initializeApp(FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.getApplicationDefault())
					.build());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		FirebaseMessaging.getInstance().send(new RedditMessageEncoder().transform(posts.get(0)).setToken(token).build());
		
	}

}
