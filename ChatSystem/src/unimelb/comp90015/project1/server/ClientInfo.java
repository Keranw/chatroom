package unimelb.comp90015.project1.server;

import java.util.ArrayList;

public class ClientInfo {
	private String clientName;
	private String password;
	private ChatRoom currentRoom;
	private ArrayList<ChatRoom> ownerRooms;
	private ArrayList<String> formerIds;

	public ClientInfo() {
		currentRoom = new ChatRoom();
		ownerRooms = new ArrayList<ChatRoom>();
		formerIds = new ArrayList<String>();
	}

	public void restoreClientInfo(ClientInfo oldClientInfo) {
		this.clientName = oldClientInfo.getClientName();
		this.password = oldClientInfo.getPassword();
		this.ownerRooms.addAll(oldClientInfo.getOwnerRooms());
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

	public ArrayList<String> getFormerIds() {
		return this.formerIds;
	}
	
	public void clearFormerIds() {
		this.formerIds.clear();
	}
	
	public void addFormerId(String id) {
		if(this.formerIds.indexOf(id) == -1) {
			this.formerIds.add(id);
		}
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
