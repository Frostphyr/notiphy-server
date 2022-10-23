package com.frostphyr.notiphy;

import java.io.IOException;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/status")
@ServletSecurity(
	value = @HttpConstraint(rolesAllowed = {"notiphy-status"})
)
public class StatusServlet extends HttpServlet {

	private static final long serialVersionUID = 7532595185841308151L;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		addInfo(request);
		addUser(request);
		getServletContext().getRequestDispatcher("/status.jsp").forward(request, response);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		doGet(request, response);
	}
	
	private void addInfo(HttpServletRequest request) {
		long totalMemory = Runtime.getRuntime().totalMemory();
		request.setAttribute("totalMemory", FileUtils.byteCountToDisplaySize(totalMemory));
		request.setAttribute("memory", FileUtils.byteCountToDisplaySize(totalMemory - Runtime.getRuntime().freeMemory()));
		EntryStatus[] statuses = new EntryStatus[EntryType.values().length]; 
		for (EntryType t : EntryType.values()) {
			statuses[t.ordinal()] = EntryStatus.forType(t);
		}
		request.setAttribute("statuses", statuses);
	}
	
	private void addUser(HttpServletRequest request) {
		String email = request.getParameter("email");
		if (email != null) {
			try {
				UserRecord user = FirebaseAuth.getInstance().getUserByEmail(email);
				request.setAttribute("userEmail", email);
				StringJoiner providerJoiner = new StringJoiner(", ");
				for (UserInfo info : user.getProviderData()) {
					providerJoiner.add(info.getProviderId());
				}
				request.setAttribute("userProviders", providerJoiner.toString());
				DocumentReference userDoc = FirestoreClient.getFirestore().document("users/" + user.getUid());
				DocumentSnapshot tokenDoc = userDoc.collection("info").document("token").get().get();
				request.setAttribute("userToken", tokenDoc.exists() ? tokenDoc.getString("value") : "X");
				QuerySnapshot entryCollection = userDoc.collection("entries").get().get();
				StringJoiner entryJoiner = new StringJoiner("\n");
				if (!entryCollection.isEmpty()) {
					for (QueryDocumentSnapshot entry : entryCollection) {
						entryJoiner.add(entry.getData().toString());
					}
				}
				request.setAttribute("userEntries", entryJoiner.toString());
			} catch (FirebaseAuthException e) {
				request.setAttribute("userError", e.getAuthErrorCode().toString());
				e.printStackTrace();
			} catch (InterruptedException | ExecutionException e) {
				request.setAttribute("userError", e.toString());
				e.printStackTrace();
			}
		}
	}
	
	public static class EntryStatus {
		
		public static EntryStatus forType(EntryType type) {
			EntryStatus status = new EntryStatus();
			status.name = type.name();
			status.status = type.getClient().getStatus();
			return status;
		}
		
		private String name;
		private String status;
		
		public String getName() {
			return name;
		}
		
		public String getStatus() {
			return status;
		}
		
	}

}
