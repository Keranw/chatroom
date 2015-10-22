package unimelb.comp90015.project1.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
	
	public void updateSaltInDisk(String newIdentity, String oldIdentity) {
		String salt = getSaltFromDisk(oldIdentity);
		if(salt != null) {
			deleteSalt(oldIdentity);
			storeSaltinDisk(newIdentity, salt);
		}
	}
	
	public String getSaltFromDisk(String identity) {
		Map<String, String> ldapContent = new HashMap<String, String>();
		Properties properties = new Properties();
		String filename = identity + ".properties";
		File file = new File(filename);
		
		try {
			if(file.exists()) {
				properties.load(new FileInputStream(filename));
			} else {
				return null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String key : properties.stringPropertyNames()) {
		   ldapContent.put(key, properties.get(key).toString());
		}
		return ldapContent.get(identity).toString();
	}
	
	public void storeSaltinDisk(String identity, String saltStr) {
		Map<String, String> ldapContent = new HashMap<String, String>();
		ldapContent.put(identity, saltStr);
		Properties properties = new Properties();
		String filename = identity + ".properties";
		
		for (Map.Entry<String,String> entry : ldapContent.entrySet()) {
		    properties.put(entry.getKey(), entry.getValue());
		}

		try {
			properties.store(new FileOutputStream(filename), null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deleteSalt(String identity) {
		String filename = identity + ".properties";
		File file = new File(filename);
		
		if(file.exists()) {
			file.delete();
		}
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
