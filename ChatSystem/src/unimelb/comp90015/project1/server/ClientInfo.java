package unimelb.comp90015.project1.server;

import java.util.ArrayList;

public class ClientInfo {
	private String clientName;
	private ChatRoom currentRoom;
	private ArrayList<ChatRoom> ownerRooms;
	private ArrayList<String> formerId;
	
	public ClientInfo() {
		currentRoom = new ChatRoom();
		ownerRooms = new ArrayList<ChatRoom>();
		formerId = new ArrayList<String>();
	}
	
	public ChatRoom getCurrentRoom() {
		return currentRoom;
	}

	public void setCurrentRoom(ChatRoom _currentRoom) {
		currentRoom = _currentRoom;
	}

	public ArrayList<ChatRoom> getOwnerRooms() {
		return ownerRooms;
	}

	public void setOwnerRooms(ArrayList<ChatRoom> _ownerRooms) {
		ownerRooms = _ownerRooms;
	}

	public void addRoom(ChatRoom room) {
		ownerRooms.add(room);
	}
	
	public String getClientName() {
		return clientName;
	}

	public void setClientName(String _clientName) {
		clientName = _clientName;
	}
	
	public ArrayList<String> getFormerId() {
		return formerId;
	}
}