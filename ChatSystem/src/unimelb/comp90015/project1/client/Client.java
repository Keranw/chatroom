package unimelb.comp90015.project1.client;

import java.util.ArrayList;

import unimelb.comp90015.project1.server.ChatRoom;

/**
 * @author kliu2 
 * Local Client Model to store client local status including
 * current client, current room that client stays, the rooms client owns
 */
public class Client {
	private String clientId;
	private String clientName;
	private ChatRoom currentRoom;
	private ArrayList<ChatRoom> ownerRooms;

	/**
	 * Constructor
	 * 
	 * @param id
	 */
	public Client(String id) {
		this.clientId = id;
		this.clientName = id;
		this.currentRoom = new ChatRoom();
		this.ownerRooms = new ArrayList<ChatRoom>();
	}
	
	///////////////////////////
	///     				 //
	/// Getters and Setters  //
	///						 //	
	///////////////////////////

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

	public ArrayList<ChatRoom> getOwnerRooms() {
		return ownerRooms;
	}

	public void setOwnerRooms(ArrayList<ChatRoom> ownerRooms) {
		this.ownerRooms = ownerRooms;
	}

}
