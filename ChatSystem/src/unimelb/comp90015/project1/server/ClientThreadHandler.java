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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author kliu2
 * A handler to receive and response clients
 */
public class ClientThreadHandler implements Runnable {
	private Socket socket;
	private ClientThread _client;
	private MainHall mainHall;
	private BufferedReader in;
	private OutputStreamWriter outputStream;

	private boolean isFirstLog;

	private ArrayList<String> formerId;

	/**
	 * Constructor
	 * @param socket
	 * @param client
	 * @param mainHall
	 */
	public ClientThreadHandler(Socket socket, ClientThread client,
			MainHall mainHall) {
		this.socket = socket;
		this._client = client;
		this.mainHall = mainHall;

		try {
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "UTF-8"));
			outputStream = new OutputStreamWriter(socket.getOutputStream(),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.isFirstLog = true;
		this.formerId = new ArrayList<String>();
	}

	@Override
	public void run() {
		try {
			try {
				while (!socket.isClosed()) {
					if (this.isFirstLog) {
						this.isFirstLog = false;
						sendFirstId();
					}
					String msg = in.readLine();
					System.out.println("receive message from "
							+ _client.getClientName() + ": " + msg);

					String type = decodeRequestJSON(msg);

					if (type == null || type.equals("quit")) {
						break;
					}
				}
				interruptThread();
			} catch (EOFException e) {
				interruptThread();
				System.out.println("Client disconnected in EOFException");
				e.printStackTrace();
			} catch (SocketException s) {
				interruptThread();
				System.out.println("Client disconnected in SocketException");
				s.printStackTrace();
			}
		} catch (IOException e) {
			interruptThread();
			System.out.println("Client disconnected in IOException");
			e.printStackTrace();
		}

		// TODO A thread finishes if run method finishes
	}

	/**
	 * stop current thread
	 */
	private void interruptThread() {
		try {
			quit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("thread is interrupted");
		ClientThread.interruptThread();
	}

	/**
	 * response a new identity when connection
	 */
	public void sendFirstId() {
		try {
			String outMsg = null;
			System.out.println("first run");
			outMsg = this.generateNewIdentity("", this._client.getClientName());
			outFlush(outputStream, outMsg);
			// server join client to mainhall
			joinRoom("mainhall");
			fetchRoomInfo("mainhall");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * decode json from clients
	 * @param jsonStr
	 * @return
	 * @throws IOException
	 */
	public String decodeRequestJSON(String jsonStr) throws IOException {
		if (jsonStr == null)
			return null;

		String type = null;
		JSONParser parser = new JSONParser();
		JSONObject object = null;
		try {
			object = (JSONObject) parser.parse(jsonStr);
			if (object.get("type") == null) {
				return null;
			}
			type = object.get("type").toString();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		String roomId = null;
		switch (type) {
		case "identitychange":
			if (checkArgs(object, "identity")) {
				String identity = object.get("identity").toString();
				changeId(type, identity);
			}
			break;
		case "join":
			if (checkArgs(object, "roomid")) {
				roomId = object.get("roomid").toString();
				joinRoom(roomId);
			}
			break;
		case "who":
			if (checkArgs(object, "roomid")) {
				roomId = object.get("roomid").toString();
				fetchRoomInfo(roomId);
			}
			break;
		case "list":
			generateRoomListMsg(outputStream);
			break;
		case "createroom":
			if (checkArgs(object, "roomid")) {
				roomId = object.get("roomid").toString();
				createRoom(roomId);
			}
			break;
		case "kick":
			if (checkArgs(object, "identity") && checkArgs(object, "roomid")
					&& checkArgs(object, "time")) {
				String user = object.get("identity").toString();
				roomId = object.get("roomid").toString();
				Integer time = Integer.valueOf(object.get("time").toString());
				kickClientFromRoom(roomId, user, time);
			}
			break;
		case "delete":
			if (checkArgs(object, "roomid")) {
				roomId = object.get("roomid").toString();
				deleteRoom(roomId);
			}
			break;
		case "message":
			String content = object.get("content").toString();
			broadMessage(content);
			break;
		case "quit":
			quit();
			break;
		}

		return type;
	}

	/**
	 * prevent the args are null
	 * @param o
	 * @param arg
	 * @return
	 * @throws IOException
	 */
	private boolean checkArgs(JSONObject o, String arg) throws IOException {
		if (o.get(arg) == null) {
			generateSystemMsg(String.format("argument %s is not provided", arg));
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private synchronized void changeId(String type, String identity)
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
	private synchronized void generateNewId(JSONObject obj, String identity)
			throws IOException {

		// this.formerNames.add(this.client.getClientName());
		if (checkValidId(identity)) {
			// update the ownership
			if (this._client.getOwnerRooms().size() > 0) {
				for (ChatRoom room : this._client.getOwnerRooms()) {
					room.setOwnerId(identity);
				}
			}
			obj.put("type", "newidentity");
			obj.put("former", this._client.getClientName());
			obj.put("identity", identity);
			// add clientId to reused id
			this.formerId.add(this._client.getClientName());
			this._client.setClientName(identity);
			// broad change information to all clients online
			for (ClientThread clientThread : this.mainHall.getAllClients()) {
				OutputStreamWriter clientOut = clientThread.getHandler()
						.getOutputStream();
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
		for (ClientThread client : this.mainHall.getAllClients()) {
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
	private synchronized void joinRoom(String roomId) throws IOException {
		ChatRoom currentRoom = this._client.getCurrentRoom();
		ChatRoom requestedRoom = this.mainHall.getRoomById(roomId);
		if (requestedRoom == null) {
			// generate roomchange msg to individual
			if (currentRoom != null) {
				generateRoomChangeMsg(currentRoom.getRoomName(),
						currentRoom.getRoomName(),
						this._client.getClientName(), outputStream);
			} else {
				generateRoomChangeMsg("", "", this._client.getClientName(),
						outputStream);
			}
			generateSystemMsg("roomId is invalid or non existent");
		} else if (currentRoom.getRoomName() == null) {
			if (forbidClientToConnect(requestedRoom) > 0) {
				requestedRoom.getClients().add(_client);
				this._client.setCurrentRoom(requestedRoom);
				broadcastToClients(requestedRoom, "",
						requestedRoom.getRoomName(),
						this._client.getClientName());
			}
		} else {
			if (forbidClientToConnect(requestedRoom) > 0) {
				currentRoom.removeClient(_client);
				removeRoomFromMainhall(currentRoom);
				requestedRoom.addClient(_client);
				this._client.setCurrentRoom(requestedRoom);
				broadcastToClients(currentRoom, currentRoom.getRoomName(),
						requestedRoom.getRoomName(),
						this._client.getClientName());
				broadcastToClients(requestedRoom, currentRoom.getRoomName(),
						requestedRoom.getRoomName(),
						this._client.getClientName());
			}
		}

		if (roomId.equalsIgnoreCase("mainhall")) {
			// response roomlist msg
			generateRoomListMsg(outputStream);
		}
	}

	/**
	 * Validate the kiced time when client tries to re-join the room
	 * @param room
	 * @return
	 */
	private synchronized int forbidClientToConnect(ChatRoom room) {
		HashMap<String, String> kickedUser = room.getKickedClients();
		String futureTime = kickedUser.get(this._client.getClientName());
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
	private synchronized void createRoom(String roomId) throws IOException {
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
			newRoom.setOwnerId(this._client.getClientName());
			this._client.addRoom(newRoom);
			this.mainHall.addRoom(newRoom);
			generateRoomListMsg(outputStream);
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
	private synchronized void fetchRoomInfo(String roomId) throws IOException {
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
	private synchronized void generateRoomChangeMsg(String former,
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
	private synchronized void generateRoomContentMsg(ChatRoom room,
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
	private synchronized void generateRoomListMsg(OutputStreamWriter out)
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
		outFlush(out, obj.toJSONString());
	}

	/**
	 * send roomchange msg to all the clients to current room or requested room
	 * 
	 * @param room
	 * @param roomChangeMsg
	 * @throws IOException
	 */
	private synchronized void broadcastToClients(ChatRoom room, String former,
			String current, String identity) throws IOException {
		for (ClientThread client : room.getClients()) {
			System.out.println(room.getClients().size());
			OutputStreamWriter broadOut = client.getHandler().getOutputStream();
			client.getHandler().generateRoomChangeMsg(former, current,
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
	private synchronized void broadcastToMainHall(ChatRoom room, String former,
			String current) throws IOException {
		for (ClientThread client : room.getClients()) {
			OutputStreamWriter broadOut = client.getHandler().getOutputStream();
			client.getHandler().generateRoomChangeMsg(former, current,
					client.getClientName(), broadOut);
		}
	}

	/**
	 * broadcast room content message
	 * @param message
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private synchronized void broadMessage(String message) throws IOException {
		if (this._client.getCurrentRoom().getClients() == null) {
			return;
		}
		JSONObject obj = new JSONObject();
		obj.put("type", "message");
		obj.put("content", message);
		obj.put("identity", this._client.getClientName());
		for (ClientThread client : this._client.getCurrentRoom().getClients()) {
			OutputStreamWriter broadOut = client.getHandler().getOutputStream();
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
	private synchronized String generateNewIdentity(String former,
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
	private synchronized void deleteRoom(String roomId) throws IOException {
		ChatRoom room = this.mainHall.getRoomById(roomId);
		ArrayList<ChatRoom> rooms = this._client.getOwnerRooms();
		if (room != null && rooms.size() > 0
				&& this._client.getOwnerRooms().contains(room)) {
			// broadcast msgs to mainhall
			broadcastToMainHall(room, roomId, "mainhall");
			// all clients are moved to mainhall
			for (ClientThread client : room.getClients()) {
				client.setCurrentRoom(mainHall);
				this.mainHall.addClient(client);
			}
			// remove room from client's ownerRooms
			this._client.getOwnerRooms().remove(room);
			// remove room from roomlist
			this.mainHall.removeRoom(room);
			// reply RoomList message to the owner
			generateRoomListMsg(outputStream);
		} else if (room != null && room.getClients().size() == 0
				&& room.getOwnerId() == "") {
			this.mainHall.removeRoom(room);
		} else {
			// return error response
			generateSystemMsg(String.format(
					"%s is a invalid ID or %s has no right to delete", roomId,
					this._client.getClientName()));
		}
	}

	/**
	 * Remove room from mainhall if the room has no owner and no other users
	 * @param currentRoom
	 * @throws IOException
	 */
	private synchronized void removeRoomFromMainhall(ChatRoom currentRoom)
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
	private synchronized void kickClientFromRoom(String roomId,
			String clientId, Integer time) throws IOException {
		ChatRoom room = this.mainHall.getRoomById(roomId);
		ClientThread client = room.findClient(clientId);
		ArrayList<ChatRoom> rooms = this._client.getOwnerRooms();
		if (room != null && rooms.size() > 0
				&& room.getOwnerId().equals(this._client.getClientName())
				&& client != null) {

			room.getKickedClients().put(clientId, calculateKickedTime(time));

			// remove client from the room and send a response message
			client.getHandler().joinRoom("mainhall");
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
	@SuppressWarnings("unchecked")
	public synchronized void quit() throws IOException {
		this._client.getCurrentRoom().removeClient(_client);
		removeRoomFromMainhall(this._client.getCurrentRoom());
		for (ChatRoom room : this._client.getOwnerRooms()) {
			if (room.getClients().size() == 0) {
				System.out.println("Removed room: " + room.getRoomName());
				this.mainHall.removeRoom(room);
			}
			room.setOwnerId("");
		}
		// add clientId to reused id
		this.formerId.add(this._client.getClientName());

		// inform all users
		JSONObject obj = new JSONObject();
		obj.put("type", "quit");
		obj.put("identity", this._client.getClientName());
		broadMsgToAll(obj.toJSONString());
		outFlush(outputStream, obj.toJSONString());
	}

	/**
	 * BroadCast message to all the clients in the same room
	 * 
	 * @param jsonStr
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private synchronized void broadMsgToAll(String jsonStr)
			throws UnsupportedEncodingException, IOException {
		System.err.println(jsonStr);
		for (ClientThread client : this._client.getCurrentRoom().getClients()) {
			OutputStreamWriter broadOut = client.getHandler().getOutputStream();
			outFlush(broadOut, jsonStr);
		}
	}

	private void outFlush(OutputStreamWriter _out, String str)
			throws IOException {
		_out.write(str + "\n");
		_out.flush();
	}

	public ArrayList<String> getFormerId() {
		return formerId;
	}

	public OutputStreamWriter getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(OutputStreamWriter outputStream) {
		this.outputStream = outputStream;
	}
}
