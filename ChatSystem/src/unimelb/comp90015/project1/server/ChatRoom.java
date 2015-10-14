package unimelb.comp90015.project1.server;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author kliu2 
 * An Object to store chatroom information, including room name,
 *         owner, clients and kickedClients
 */
public class ChatRoom {
	private String roomId;
	private String roomName;
	private ArrayList<ClientThread> clients;
	private String ownerId;
	private HashMap<String, String> kickedClients;

	// Constructors
	public ChatRoom() {

	}

	public ChatRoom(String name) {
		this.roomId = name;
		this.roomName = name;
		this.clients = new ArrayList<ClientThread>();
		this.setKickedClients(new HashMap<String, String>());
	}
	
///////////////////////////
///     				 //
/// Getters and Setters  //
///						 //	
///////////////////////////

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public ArrayList<ClientThread> getClients() {
		return clients;
	}

	public void setClients(ArrayList<ClientThread> clients) {
		this.clients = clients;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public ClientThread findClient(String clientId) {
		for (ClientThread client : this.clients) {
			if (client.getClientName().equals(clientId)) {
				return client;
			}
		}
		return null;
	}

	public void addClient(ClientThread newClient) {
		this.clients.add(newClient);
	}

	public void removeClient(ClientThread client) {
		this.clients.remove(client);
	}

	public HashMap<String, String> getKickedClients() {
		return kickedClients;
	}

	public void setKickedClients(HashMap<String, String> kickedClients) {
		this.kickedClients = kickedClients;
	}
}
