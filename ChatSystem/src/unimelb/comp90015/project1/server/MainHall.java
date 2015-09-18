package unimelb.comp90015.project1.server;

import java.util.ArrayList;

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
	
	public void addRoom(ChatRoom room) {
		this.rooms.add(room);
	}
	
	public void removeRoom(ChatRoom room) {
		this.rooms.remove(room);
	}
	
	public ChatRoom getRoomById(String roomId) {
		if(roomId.equalsIgnoreCase("mainhall")) {
			return this;
		}
		else {
			for(ChatRoom room : this.rooms) {
				if(room.getRoomName().equalsIgnoreCase(roomId)){
					return room;
				}
			}
		}
		return null;
	}
	
	public ArrayList<ClientThread> getAllClients() {
		ArrayList<ClientThread> allClients = new ArrayList<ClientThread>();
		allClients.addAll(this.getClients());
		for(ChatRoom room : this.getRooms()) {
			allClients.addAll(room.getClients());
		}
		return allClients;
	}
}
