package unimelb.comp90015.project1.client;

import java.util.ArrayList;

import unimelb.comp90015.project1.server.ChatRoom;

public class Client {
	private String clientId;
	private String clientName;
	private ChatRoom currentRoom;
	private ArrayList<ChatRoom> ownerRooms;
	
	public Client(String id){
		this.clientId = id;
		this.clientName = id;
		this.currentRoom = new ChatRoom();
		this.ownerRooms = new ArrayList<ChatRoom>();
	}
	
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	public ChatRoom getCurrentRoom() {
		return currentRoom;
	}
	public void setCurrentRoom(ChatRoom currentRoom) {
		this.currentRoom = currentRoom;
	}
	public ArrayList getOwnerRooms() {
		return ownerRooms;
	}
	public void setOwnerRooms(ArrayList ownerRooms) {
		this.ownerRooms = ownerRooms;
	}
	
}
