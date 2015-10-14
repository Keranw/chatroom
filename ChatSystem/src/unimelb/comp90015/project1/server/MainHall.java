package unimelb.comp90015.project1.server;

import java.util.ArrayList;

/**
 * @author kliu2 
 * A special chat room which have some particular attributes
 */
public class MainHall extends ChatRoom {
	private static ArrayList<ChatRoom> rooms;

	public MainHall(String name) {
		super(name);
		this.rooms = new ArrayList<ChatRoom>();
	}

	public ArrayList<ChatRoom> getRooms() {
		return rooms;
	}

	public void setRooms(ArrayList<ChatRoom> rooms) {
		this.rooms = rooms;
	}

	/**
	 * Add a room into main hall
	 * @param room
	 */
	public void addRoom(ChatRoom room) {
		this.rooms.add(room);
	}

	/**
	 * Remove a specific room from main hall
	 * @param room
	 */
	public void removeRoom(ChatRoom room) {
		this.rooms.remove(room);
	}

	/**
	 * Get A specific room by room name
	 * @param roomId
	 * @return room if roomId exists
	 * @return null if non-exist
	 */
	public ChatRoom getRoomById(String roomId) {
		if (roomId.equalsIgnoreCase("mainhall")) {
			return this;
		} else {
			for (ChatRoom room : this.rooms) {
				if (room.getRoomName().equalsIgnoreCase(roomId)) {
					return room;
				}
			}
		}
		return null;
	}

	/**
	 * Get All Clients on line
	 * @return allClients A list of Clients
	 */
	public ArrayList<ClientThread> getAllClients() {
		ArrayList<ClientThread> allClients = new ArrayList<ClientThread>();
		allClients.addAll(this.getClients());
		for (ChatRoom room : this.getRooms()) {
			allClients.addAll(room.getClients());
		}
		return allClients;
	}
}
