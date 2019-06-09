package com.frostphyr.notiphy.ui;

import java.awt.CardLayout;
import java.awt.Dimension;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.frostphyr.notiphy.EntryCollection;
import com.frostphyr.notiphy.EntryType;

import net.miginfocom.swing.MigLayout;

public class ServerExplorer {
	
	private static final String SESSION_LIST_PANEL_NAME = "SessionList";
	private static final String ENTRY_LIST_PANEL_NAME = "EntryList";
	
	private JFrame frame = new JFrame("Notiphy Server Explorer");
	private JTextArea logTextArea = new JTextArea(15, 50);
	private JScrollPane logScrollPane = new JScrollPane(logTextArea);
	private JList<String> sessionList = new JList<>(new DefaultListModel<String>());
	private JList<String> entryList = new JList<>(new DefaultListModel<String>());
	private JPanel sidePanel = new JPanel(new CardLayout());
	
	public ServerExplorer() {
		init();
	}
	
	private void init() {
		SwingUtilities.invokeLater(() -> {
			JButton viewButton = new JButton("View");
			JButton exitButton = new JButton("Exit");
			sidePanel.add(createSidePanel(sessionList, viewButton, exitButton), SESSION_LIST_PANEL_NAME);
			
			JButton backButton = new JButton("Back");
			JButton refreshButton = new JButton("Refresh");
			sidePanel.add(createSidePanel(entryList, backButton, refreshButton), ENTRY_LIST_PANEL_NAME);
			
			backButton.addActionListener((e) -> showSessions());
			exitButton.addActionListener((e) -> System.exit(0));
			viewButton.addActionListener((e) -> {
				String id = sessionList.getSelectedValue();
				if (id != null) {
					showEntries(id);
				}
			});
			refreshButton.addActionListener((e) -> {
				String id = sessionList.getSelectedValue();
				if (id != null) {
					populateEntryList(id);
				}
			});
			
			frame.setLayout(new MigLayout());
			logTextArea.setEditable(false);
			frame.add(logScrollPane);
			frame.add(sidePanel, "grow, push");
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setLocationByPlatform(true);
		});
	}
	
	private JPanel createSidePanel(JList<?> list, JButton button1, JButton button2) {
		JPanel panel = new JPanel(new MigLayout());
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScrollPane = new JScrollPane(list);
		listScrollPane.setPreferredSize(new Dimension(0, 0));
		panel.add(listScrollPane, "grow, push, wrap");
		panel.add(button1, "split 2, growx");
		panel.add(button2, "growx");
		return panel;
	}
	
	public void show() {
		SwingUtilities.invokeLater(() -> {
			frame.setVisible(true);
		});
	}
	
	public void dispose() {
		SwingUtilities.invokeLater(() -> {
			frame.dispose();
		});
	}
	
	public void addSession(String id) {
		SwingUtilities.invokeLater(() -> {
			((DefaultListModel<String>) sessionList.getModel()).addElement(id);
		});
	}
	
	public void removeSession(String id) {
		SwingUtilities.invokeLater(() -> {
			((DefaultListModel<String>) sessionList.getModel()).removeElement(id);
		});
	}
	
	public void append(String message) {
		Document document = logTextArea.getDocument();
		try {
			document.insertString(document.getLength(), message, null);
			JScrollBar vertical = logScrollPane.getVerticalScrollBar();
			vertical.setValue(vertical.getMaximum());
		} catch (BadLocationException e) {
		}
	}
	
	private void showEntries(String id) {
		populateEntryList(id);
		((CardLayout) sidePanel.getLayout()).show(sidePanel, ENTRY_LIST_PANEL_NAME);
	}
	
	private void populateEntryList(String id) {
		DefaultListModel<String> model = (DefaultListModel<String>) entryList.getModel();
		model.clear();
		for (EntryType t : EntryType.values()) {
			EntryCollection<?, ?> entries = t.getClient().getEntries();
			synchronized (entries) {
				entries.forEachEntry(id, (e) -> model.addElement(e.toString()));
			}
		}
	}
	
	private void showSessions() {
		((CardLayout) sidePanel.getLayout()).show(sidePanel, SESSION_LIST_PANEL_NAME);
	}

}
