package com.frostphyr.notiphy.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.frostphyr.notiphy.EntryType;
import com.frostphyr.notiphy.NotiphyServer;

@WebServlet("/manager")
@ServletSecurity(
	value = @HttpConstraint(rolesAllowed = {"manager"})
)
public class ManagerServlet extends HttpServlet {

	private static final long serialVersionUID = 5303326990507750765L;
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		List<StatTracker> trackers = new ArrayList<>();
		trackers.add(NotiphyServer.getHeartbeatManager().getSessionTracker());
		for (EntryType t : EntryType.values()) {
			for (StatTracker s : t.getClient().getEntries().getTrackers()) {
				trackers.add(s);
			}
		}
		
		request.setAttribute("trackers", trackers);
		request.setAttribute("log", NotiphyServer.getLog());
		getServletContext().getRequestDispatcher("/manager.jsp").forward(request, response);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		doGet(request, response);
	}

}
