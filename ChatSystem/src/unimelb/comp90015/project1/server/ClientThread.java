package unimelb.comp90015.project1.server;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author kliu2
 * The Client Model stored in server
 */
public class ClientThread {
	private Socket socket;

	// Client Info
//	private String clientId;
	private String clientName;
	private ChatRoom currentRoom;
	private ArrayList<ChatRoom> ownerRooms;
	private HashMap<String, String> identitiesHash;
	private ArrayList<String> formerId;

	private static MainHall mainHall;
	private ClientThreadHandler handler;
	private Thread handlerThread;
	
	private OutputStreamWriter outputStream;

	/**
	 * Constructor
	 * start a thread to listen this client, receiving and sending messages
	 * @param socket
	 * @param id
	 * @param _mainHall
	 */
	public ClientThread(Socket socket, String id, MainHall _mainHall, HashMap<String, String> identitiesHash) {
		this.socket = socket;

//		clientId = id;
		clientName = id;
		currentRoom = new ChatRoom();
		ownerRooms = new ArrayList<ChatRoom>();
		formerId = new ArrayList<String>();

		mainHall = _mainHall;
		
		try {
			outputStream = new OutputStreamWriter(socket.getOutputStream(),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.handler = new ClientThreadHandler(socket, this);
		this.identitiesHash = identitiesHash;
		handlerThread = new Thread(this.handler);
		handlerThread.setDaemon(true);
		handlerThread.start();
	}

	// stop this thread
	public void interruptThread() {
		handlerThread.interrupt();
	}
	
	///////////////////////////
	///     				 //
	/// Getters and Setters  //
	///						 //	
	///////////////////////////

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

	public ClientThreadHandler getHandler() {
		return handler;
	}

	public void setHandler(ClientThreadHandler handler) {
		this.handler = handler;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String _clientName) {
		clientName = _clientName;
		// store the thread in identitiesHash
//		this.identitiesHash.put("thread", this);
	}
	
	///////////////////////////
	///     				 //
	/// 		Logic		 //
	///						 //	
	///////////////////////////
	/**
	 * response a new identity when connection
	 */
	public void sendFirstId() {
		try {
			String outMsg = null;
			System.out.println("first run");
			outMsg = this.generateNewIdentity("", this.getClientName());
			outFlush(outputStream, outMsg);
			// server join client to mainhall
			joinRoom("mainhall");
			fetchRoomInfo("mainhall");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Using SHA1 to generate a hash of password
	 * to authenticate the identity in server
	 * @param password
	 * @return a hash of password
	 */
	public static String encyptPassword(String password) {
		return DigestUtils.sha1Hex(password);
	}
	
	/**
	 * prevent the args are null
	 * @param o
	 * @param arg
	 * @return
	 * @throws IOException
	 */
	public boolean checkArgs(JSONObject o, String arg) throws IOException {
		if (o.get(arg) == null) {
			generateSystemMsg(String.format("argument %s is not provided", arg));
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public void changeId(String identity)
			throws IOException {
		JSONObject obj = new JSONObject();
		// response New Identity Message
		generateNewId(obj, identity);
	}

	/**
	 * @param obj
	 * @param identity
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void generateNewId(JSONObject obj, String identity)
			throws IOException {

		// this.formerNames.add(this.client.getClientName());
		if (checkValidId(identity)) {
			// update the ownership
			if (this.getOwnerRooms().size() > 0) {
				for (ChatRoom room : this.getOwnerRooms()) {
					room.setOwnerId(identity);
				}
			}
			obj.put("type", "newidentity");
			obj.put("former", this.getClientName());
			obj.put("identity", identity);
			// add clientId to reused id
			this.formerId.add(this.getClientName());
			this.setClientName(identity);
			// broad change information to all clients online
			for (ClientThread clientThread : this.mainHall.getAllClients()) {
				OutputStreamWriter clientOut = clientThread.getOutputStream();
				outFlush(clientOut, obj.toJSONString());
			}
		}
	}

	/**
	 * User cannot change their ID to an existent ID or invalid ID format
	 * @param identity
	 * @return
	 * @throws IOException
	 */
	private boolean checkValidId(String identity) throws IOException {
		/**
		 * The requested identity must be an alphanumeric string starting
		 * with an upper or lower case character,i.e. upper and lower case
		 * characters only and digits. The identity must be at least 3
		 * characters and no more than 16 characters
		 */
		String idPattern = "^[a-zA-Z][A-Za-z0-9]{2,15}$";
		Pattern id = Pattern.compile(idPattern);
		Matcher idMatcher = id.matcher(identity.trim());
		if (!idMatcher.find()) {
			generateSystemMsg(String
					.format("%s should be alphanumeric string and the length should be [3, 16]",
							identity));
			return false;
		}
		
		for (ClientThread client : this.mainHall.getAllClients()) {
			if (identity.equals(client.getClientName())
					|| identity.equals(client.getClientName())) {
				// response invalid identity
				generateSystemMsg(String.format("%s is now %s",
						client.getClientName(), identity));
				return false;
			}
		}
		return true;
	}

	/**
	 * Join a room
	 * @param roomId
	 * @throws IOException
	 */
	public void joinRoom(String roomId) throws IOException {
		ChatRoom currentRoom = this.getCurrentRoom();
		ChatRoom requestedRoom = this.mainHall.getRoomById(roomId);
		if (requestedRoom == null) {
			// generate roomchange msg to individual
			if (currentRoom != null) {
				generateRoomChangeMsg(currentRoom.getRoomName(),
						currentRoom.getRoomName(),
						this.getClientName(), outputStream);
			} else {
				generateRoomChangeMsg("", "", this.getClientName(),
						outputStream);
			}
			generateSystemMsg("roomId is invalid or non existent");
		} else if (currentRoom.getRoomName() == null) {
			if (forbidClientToConnect(requestedRoom) > 0) {
				requestedRoom.getClients().add(this);
				this.setCurrentRoom(requestedRoom);
				broadcastToClients(requestedRoom, "",
						requestedRoom.getRoomName(),
						this.getClientName());
			}
		} else {
			if (forbidClientToConnect(requestedRoom) > 0) {
				currentRoom.removeClient(this);
				removeRoomFromMainhall(currentRoom);
				requestedRoom.addClient(this);
				this.setCurrentRoom(requestedRoom);
				broadcastToClients(currentRoom, currentRoom.getRoomName(),
						requestedRoom.getRoomName(),
						this.getClientName());
				broadcastToClients(requestedRoom, currentRoom.getRoomName(),
						requestedRoom.getRoomName(),
						this.getClientName());
			}
		}

		if (roomId.equalsIgnoreCase("mainhall")) {
			// response roomlist msg
			generateRoomListMsg();
		}
	}

	/**
	 * Validate the kiced time when client tries to re-join the room
	 * @param room
	 * @return
	 */
	private int forbidClientToConnect(ChatRoom room) {
		HashMap<String, String> kickedUser = room.getKickedClients();
		String futureTime = kickedUser.get(this.getClientName());
		if (futureTime != null) {
			Long time = Long.parseLong(futureTime);
			Long currentTime = System.currentTimeMillis();
			System.out.println("currentTime: " + time.toString());
			System.out.println("kickedTime: " + currentTime.toString());
			return currentTime.compareTo(time);
		} else {
			return 1;
		}
	}

	/**
	 * create a room by given name
	 * @param roomId
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public void createRoom(String roomId) throws IOException {
		/**
		 * The room name must contain alphanumeric characters only, start with
		 * an upper or lower case letter, have at least 3 characters and at most
		 * 32 characters.
		 */
		String roomPattern = "^[A-Za-z0-9]{3,32}$";
		Pattern id = Pattern.compile(roomPattern);
		Matcher idMatcher = id.matcher(roomId.trim());
		if (!idMatcher.find()) {
			generateSystemMsg(String
					.format("%s should be alphanumeric string and the length should be [3, 32]",
							roomId));
			return;
		}

		ChatRoom requestedRoom = this.mainHall.getRoomById(roomId);
		if (requestedRoom == null) {
			ChatRoom newRoom = new ChatRoom(roomId);
			newRoom.setOwnerId(this.getClientName());
			this.addRoom(newRoom);
			this.mainHall.addRoom(newRoom);
			generateRoomListMsg();
			generateSystemMsg(String.format("%s created", roomId));
		} else {
			generateSystemMsg(String.format("%s is invalid or already in use",
					roomId));
		}
	}

	/**
	 * fetch specific room info
	 * @param roomId
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public void fetchRoomInfo(String roomId) throws IOException {
		ChatRoom room = this.mainHall.getRoomById(roomId);
		generateRoomContentMsg(room, outputStream);
	}

	/**
	 * generate room changement message
	 * 
	 * @param former
	 * @param current
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public void generateRoomChangeMsg(String former,
			String current, String identity, OutputStreamWriter _out) {
		JSONObject obj = new JSONObject();
		obj.put("type", "roomchange");
		obj.put("identity", identity);
		obj.put("former", former);
		obj.put("roomid", current);

		try {
			outFlush(_out, obj.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generate a room content message
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public void generateRoomContentMsg(ChatRoom room,
			OutputStreamWriter out) {
		if (room == null) {
			return;
		}

		JSONObject obj = new JSONObject();
		JSONArray clients = new JSONArray();
		for (ClientThread client : room.getClients()) {
			String clientName = client.getClientName();
			if (client.getClientName().equals(room.getOwnerId())) {
				clientName = clientName + "*";
			}
			clients.add(clientName);
		}

		obj.put("type", "roomcontents");
		obj.put("roomid", room.getRoomName());
		obj.put("identities", clients);
		obj.put("owner", room.getOwnerId());
		try {
			outFlush(out, obj.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Used when the client sends a "list" command or then client connects to
	 * the server
	 * 
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void generateRoomListMsg()
			throws IOException {
		JSONObject obj = new JSONObject();
		JSONArray roomlist = new JSONArray();
		obj.put("type", "roomlist");

		JSONObject roomObj = new JSONObject();
		roomObj.put("roomid", this.mainHall.getRoomName());
		roomObj.put("count", this.mainHall.getClients().size());
		roomlist.add(roomObj);
		for (ChatRoom room : this.mainHall.getRooms()) {
			JSONObject roomO = new JSONObject();
			roomO.put("roomid", room.getRoomName());
			roomO.put("count", room.getClients().size());
			roomlist.add(roomO);
		}
		obj.put("rooms", roomlist);
		outFlush(outputStream, obj.toJSONString());
	}

	/**
	 * send roomchange msg to all the clients to current room or requested room
	 * 
	 * @param room
	 * @param roomChangeMsg
	 * @throws IOException
	 */
	public void broadcastToClients(ChatRoom room, String former,
			String current, String identity) throws IOException {
		for (ClientThread client : room.getClients()) {
			System.out.println(room.getClients().size());
			OutputStreamWriter broadOut = client.getOutputStream();
			client.generateRoomChangeMsg(former, current,
					identity, broadOut);
		}
	}

	/**
	 * send roomchange msg to mainhall for all the clients in current room
	 * 
	 * @param room
	 * @param roomChangeMsg
	 * @throws IOException
	 */
	private void broadcastToMainHall(ChatRoom room, String former,
			String current) throws IOException {
		for (ClientThread client : room.getClients()) {
			OutputStreamWriter broadOut = client.getOutputStream();
			client.generateRoomChangeMsg(former, current,
					client.getClientName(), broadOut);
		}
	}

	/**
	 * broadcast room content message
	 * @param message
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void broadMessage(String message) throws IOException {
		if (this.getCurrentRoom().getClients() == null) {
			return;
		}
		JSONObject obj = new JSONObject();
		obj.put("type", "message");
		obj.put("content", message);
		obj.put("identity", this.getClientName());
		for (ClientThread client : this.getCurrentRoom().getClients()) {
			OutputStreamWriter broadOut = client.getOutputStream();
			outFlush(broadOut, obj.toJSONString());
		}
	}

	/**
	 * Generate a new Identity for a user
	 * @param former
	 * @param identity
	 * @return A JSON value contains former and current identity info
	 */
	@SuppressWarnings("unchecked")
	private String generateNewIdentity(String former,
			String identity) {
		JSONObject obj = new JSONObject();
		obj.put("type", "newidentity");
		obj.put("former", former);
		obj.put("identity", identity);
		return obj.toJSONString();
	}

	/**
	 * Delete a specific room 
	 * @param roomId
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public void deleteRoom(String roomId) throws IOException {
		ChatRoom room = this.mainHall.getRoomById(roomId);
		ArrayList<ChatRoom> rooms = this.getOwnerRooms();
		if (room != null && rooms.size() > 0
				&& this.getOwnerRooms().contains(room)) {
			// broadcast msgs to mainhall
			broadcastToMainHall(room, roomId, "mainhall");
			// all clients are moved to mainhall
			for (ClientThread client : room.getClients()) {
				client.setCurrentRoom(mainHall);
				this.mainHall.addClient(client);
			}
			// remove room from client's ownerRooms
			this.getOwnerRooms().remove(room);
			// remove room from roomlist
			this.mainHall.removeRoom(room);
			// reply RoomList message to the owner
			generateRoomListMsg();
		} else if (room != null && room.getClients().size() == 0
				&& room.getOwnerId() == "") {
			this.mainHall.removeRoom(room);
		} else {
			// return error response
			generateSystemMsg(String.format(
					"%s is a invalid ID or %s has no right to delete", roomId,
					this.getClientName()));
		}
	}

	/**
	 * Remove room from mainhall if the room has no owner and no other users
	 * @param currentRoom
	 * @throws IOException
	 */
	private void removeRoomFromMainhall(ChatRoom currentRoom)
			throws IOException {
		// if currentRoom has no owner and empty, remove it
		if (currentRoom.getClients().size() == 0
				&& !currentRoom.getRoomName().equals("mainhall")
				&& currentRoom.getOwnerId() == "") {
			deleteRoom(currentRoom.getRoomName());
		}

	}

	/**
	 * Kick Client from Room in specific time
	 * @param roomId
	 * @param clientId
	 * @param time
	 * @throws IOException
	 */
	public void kickClientFromRoom(String roomId,
			String clientId, Integer time) throws IOException {
		ChatRoom room = this.mainHall.getRoomById(roomId);
		ClientThread client = room.findClient(clientId);
		ArrayList<ChatRoom> rooms = this.getOwnerRooms();
		if (room != null && rooms.size() > 0
				&& room.getOwnerId().equals(this.getClientName())
				&& client != null) {

			room.getKickedClients().put(clientId, calculateKickedTime(time));

			// remove client from the room and send a response message
			client.joinRoom("mainhall");
		} else {
			// return error response, room is invalid or user is invalid
			generateSystemMsg("invalid roomId or UserId");
		}
	}

	/**
	 * Calculate the certain future expired time
	 * @param kickedTime
	 * @return
	 */
	private String calculateKickedTime(Integer kickedTime) {
		Long currentTime = System.currentTimeMillis();
		Long futureTime = currentTime + 1000 * kickedTime;
		return futureTime.toString();
	}

	/**
	 * Send a system message to clients
	 * @param msg
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void generateSystemMsg(String msg) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("type", "message");
		obj.put("identity", "SYSTEM");
		obj.put("content", msg);
		outFlush(outputStream, obj.toJSONString());
	}

	/**
	 * The following information should be deleted in order: 
	 * 1. remove the client from current room 
	 * 2. If the own room has no content, delete the room 
	 * 3. clear the ownership of the client
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void quit() throws IOException {
//		this._client.getCurrentRoom().removeClient(_client);
//		removeRoomFromMainhall(this._client.getCurrentRoom());
//		for (ChatRoom room : this._client.getOwnerRooms()) {
//			if (room.getClients().size() == 0) {
//				System.out.println("Removed room: " + room.getRoomName());
//				this.mainHall.removeRoom(room);
//			}
//			room.setOwnerId("");
//		}
//		// add clientId to reused id
//		this.formerId.add(this._client.getClientName());

		// inform all users
		JSONObject obj = new JSONObject();
		obj.put("type", "quit");
		obj.put("identity", this.getClientName());
		broadMsgToAll(obj.toJSONString());
		outFlush(outputStream, obj.toJSONString());
		
		// TODO suspend this thread until the user re-login
	}

	/**
	 * BroadCast message to all the clients in the same room
	 * 
	 * @param jsonStr
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private void broadMsgToAll(String jsonStr)
			throws UnsupportedEncodingException, IOException {
		System.err.println(jsonStr);
		for (ClientThread client : this.getCurrentRoom().getClients()) {
			OutputStreamWriter broadOut = client.getOutputStream();
			outFlush(broadOut, jsonStr);
		}
	}

	private synchronized void outFlush(OutputStreamWriter _out, String str)
			throws IOException {
		_out.write(str + "\n");
		_out.flush();
	}

	public ArrayList<String> getFormerId() {
		return formerId;
	}

	private OutputStreamWriter getOutputStream() {
		return outputStream;
	}

	private void setOutputStream(OutputStreamWriter outputStream) {
		this.outputStream = outputStream;
	}

}
