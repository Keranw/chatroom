package unimelb.comp90015.project1.server;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Kun Liu
 *
 */
public class ClientThreadHandler {
	private Socket socket;
	private ClientThread client;
	private MainHall mainHall;
	private OutputStreamWriter out;
	private ArrayList<String> formerId;

	public ClientThreadHandler(Socket socket, ClientThread client,
			MainHall mainHall, OutputStreamWriter out) {
		this.socket = socket;
		this.client = client;
		this.mainHall = mainHall;
		this.out = out;
		this.formerId = new ArrayList<String>();
	}

	public void sendFirstId() {
		try {
			String outMsg = null;
			System.out.println("first run");
			outMsg = this.generateNewIdentity("", this.client.getClientName());
			out.write(outMsg + "\n");
			out.flush();
			// server join client to mainhall
			joinRoom("mainhall");
			fetchRoomInfo("mainhall");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
			generateRoomListMsg(out);
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

	private boolean checkArgs(JSONObject o, String arg) throws IOException {
		if (o.get(arg) == null) {
			generateErrorMsg(String.format("argument %s is not provided", arg));
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

	@SuppressWarnings("unchecked")
	private synchronized void generateNewId(JSONObject obj, String identity)
			throws IOException {

		// this.formerNames.add(this.client.getClientName());
		if (checkValidId(identity)) {
			// update the ownership
			if (this.client.getOwnerRooms().size() > 0) {
				for (ChatRoom room : this.client.getOwnerRooms()) {
					room.setOwnerId(identity);
				}
			}
			obj.put("type", "newidentity");
			obj.put("former", this.client.getClientName());
			obj.put("identity", identity);
			this.client.setClientName(identity);
			// broad change information to all clients online
			for (ClientThread clientThread : this.mainHall.getAllClients()) {
				OutputStreamWriter clientOut = new OutputStreamWriter(
						clientThread.getSocket().getOutputStream(), "UTF-8");
				clientOut.write(obj.toJSONString() + "\n");
				clientOut.flush();
			}
		}
	}

	private synchronized boolean checkValidId(String identity)
			throws IOException {
		for (ClientThread client : this.mainHall.getAllClients()) {
			/**
			 * The requested identity must be an alphanumeric string starting
			 * with an upper or lower case character,i.e. upper and lower case
			 * characters only and digits. The identity must be at least 3
			 * characters and no more than 16 characters
			 */
			String idPattern = "^[A-Za-z0-9]{3,16}$";
			Pattern id = Pattern.compile(idPattern);
			Matcher idMatcher = id.matcher(identity);
			if (!idMatcher.find()) {
				generateErrorMsg(String
						.format("%s should be alphanumeric string and the length should be [3, 16]",
								identity));
				return false;
			}

			if (identity.equals(client.getClientId())
					|| identity.equals(client.getClientName())) {
				// response invalid identity
				generateErrorMsg(String.format("%s is now %s",
						client.getClientId(), identity));
				return false;
			}
		}
		return true;
	}

	private synchronized void joinRoom(String roomId) throws IOException {
		ChatRoom currentRoom = this.client.getCurrentRoom();
		ChatRoom requestedRoom = this.mainHall.getRoomById(roomId);
		if (requestedRoom == null) {
			// generate roomchange msg to individual
			if (currentRoom != null) {
				generateRoomChangeMsg(currentRoom.getRoomName(),
						currentRoom.getRoomName(), this.client.getClientName(),
						out);
			} else {
				generateRoomChangeMsg("", "", this.client.getClientName(), out);
			}
			generateErrorMsg("roomId is invalid or non existent");
		} else if (currentRoom.getRoomId() == null) {
			if (forbidClientToConnect(requestedRoom) > 0) {
				requestedRoom.getClients().add(client);
				this.client.setCurrentRoom(requestedRoom);
				broadcastToClients(requestedRoom, "",
						requestedRoom.getRoomName(),
						this.client.getClientName());
			}
		} else {
			if (forbidClientToConnect(requestedRoom) > 0) {
				currentRoom.removeClient(client);
				// if currentRoom has no owner and empty, remove it
				if (currentRoom.getClients().size() == 0
						&& !currentRoom.getRoomId().equals("mainhall")) {
					deleteRoom(currentRoom.getRoomId());
				}

				requestedRoom.addClient(client);
				this.client.setCurrentRoom(requestedRoom);
				broadcastToClients(currentRoom, currentRoom.getRoomName(),
						requestedRoom.getRoomName(),
						this.client.getClientName());
				broadcastToClients(requestedRoom, currentRoom.getRoomName(),
						requestedRoom.getRoomName(),
						this.client.getClientName());
			}
		}

		if (roomId.equalsIgnoreCase("mainhall")) {
			// response roomlist msg
			generateRoomListMsg(out);
		}
	}

	private int forbidClientToConnect(ChatRoom room) {
		HashMap<String, String> kickedUser = room.getKickedClients();
		String futureTime = kickedUser.get(this.client.getClientName());
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

	@SuppressWarnings("static-access")
	private synchronized void createRoom(String roomId) throws IOException {
		ChatRoom requestedRoom = this.mainHall.getRoomById(roomId);
		if (requestedRoom == null) {
			ChatRoom newRoom = new ChatRoom(roomId);
			newRoom.setOwnerId(this.client.getClientName());
			this.client.addRoom(newRoom);
			this.mainHall.addRoom(newRoom);
			generateRoomListMsg(out);
			generateSuccessMsg(String.format("%s created", roomId));
		} else {
			generateErrorMsg(String.format("%s is invalid or already in use",
					roomId));
		}
	}

	@SuppressWarnings("static-access")
	private synchronized void fetchRoomInfo(String roomId) throws IOException {
		ChatRoom room = this.mainHall.getRoomById(roomId);
		generateRoomContentMsg(room, out);
	}

	/**
	 * Used when
	 * 
	 * @param former
	 * @param current
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private void generateRoomChangeMsg(String former, String current,
			String identity, OutputStreamWriter out) {
		JSONObject obj = new JSONObject();
		obj.put("type", "roomchange");
		obj.put("identity", identity);
		obj.put("former", former);
		obj.put("roomid", current);

		try {
			out.write(obj.toJSONString() + "\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Used when
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
			out.write(obj.toJSONString() + "\n");
			out.flush();
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
	private void generateRoomListMsg(OutputStreamWriter out) throws IOException {
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
		out.write(obj.toJSONString() + "\n");
		out.flush();
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
			OutputStreamWriter broadOut = new OutputStreamWriter(client
					.getSocket().getOutputStream(), "UTF-8");
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
			OutputStreamWriter broadOut = new OutputStreamWriter(client
					.getSocket().getOutputStream(), "UTF-8");
			client.getHandler().generateRoomChangeMsg(former, current,
					client.getClientName(), broadOut);
		}
	}

	private synchronized void broadMessage(String message) throws IOException {
		if (this.client.getCurrentRoom().getClients() == null) {
			return;
		}
		JSONObject obj = new JSONObject();
		obj.put("type", "message");
		obj.put("content", message);
		obj.put("identity", this.client.getClientName());
		for (ClientThread client : this.client.getCurrentRoom().getClients()) {
			OutputStreamWriter broadOut = new OutputStreamWriter(client
					.getSocket().getOutputStream(), "UTF-8");
			broadOut.write(obj.toJSONString() + "\n");
			broadOut.flush();
		}
	}

	@SuppressWarnings("unchecked")
	private String generateNewIdentity(String former, String identity) {
		JSONObject obj = new JSONObject();
		obj.put("type", "newidentity");
		obj.put("former", former);
		obj.put("identity", identity);
		return obj.toJSONString();
	}

	@SuppressWarnings("static-access")
	private synchronized void deleteRoom(String roomId) throws IOException {
		ChatRoom room = this.mainHall.getRoomById(roomId);
		ArrayList<ChatRoom> rooms = this.client.getOwnerRooms();
		if (room != null && rooms.size() > 0
				&& this.client.getOwnerRooms().contains(room)) {
			// broadcast msgs to mainhall
			broadcastToMainHall(room, roomId, "mainhall");
			// all clients are moved to mainhall
			for (ClientThread client : room.getClients()) {
				client.setCurrentRoom(mainHall);
				this.mainHall.addClient(client);
			}
			// remove room from client's ownerRooms
			this.client.getOwnerRooms().remove(room);
			// remove room from roomlist
			this.mainHall.removeRoom(room);
			// reply RoomList message to the owner
			generateRoomListMsg(out);
		} else {
			// return error response
			generateErrorMsg(String.format(
					"%s is a invalid ID or %s has no right to delete", roomId,
					this.client.getClientName()));
		}
	}

	private synchronized void kickClientFromRoom(String roomId,
			String clientId, Integer time) throws IOException {
		ChatRoom room = this.mainHall.getRoomById(roomId);
		ClientThread client = room.findClient(clientId);
		ArrayList<ChatRoom> rooms = this.client.getOwnerRooms();
		if (room != null && rooms.size() > 0
				&& room.getOwnerId().equals(this.client.getClientName())
				&& client != null) {

			room.getKickedClients().put(clientId, calculateKickedTime(time));

			// remove client from the room and send a response message
			client.getHandler().joinRoom("mainhall");
		} else {
			// return error response, room is invalid or user is invalid
			generateErrorMsg("invalid roomId or UserId");
		}
	}

	private String calculateKickedTime(Integer kickedTime) {
		Long currentTime = System.currentTimeMillis();
		Long futureTime = currentTime + 1000 * kickedTime;
		return futureTime.toString();
	}

	@SuppressWarnings("unchecked")
	private void generateErrorMsg(String msg) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("type", "error");
		obj.put("content", msg);
		out.write(obj.toJSONString() + "\n");
		out.flush();
	}

	@SuppressWarnings("unchecked")
	private void generateSuccessMsg(String msg) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("type", "success");
		obj.put("content", msg);
		out.write(obj.toJSONString() + "\n");
		out.flush();
	}

	/**
	 * The following information should be deleted in order: 1. remove the
	 * client from current room 2. If the own room has no content, delete the
	 * room 3. clear the ownership of the client
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void quit() throws IOException {
		this.client.getCurrentRoom().removeClient(client);
		for (ChatRoom room : this.client.getOwnerRooms()) {
			if (room.getClients().size() == 0) {
				this.mainHall.removeRoom(room);
			}
			room.setOwnerId("");
		}
		// add clientId to reused id
		this.formerId.add(this.client.getClientId());

		// inform all users
		JSONObject obj = new JSONObject();
		obj.put("type", "quit");
		obj.put("identity", this.client.getClientName());
		broadMsgToAll(obj.toJSONString());
		out.write(obj.toJSONString() + "\n");
		out.flush();
	}

	private void broadMsgToAll(String jsonStr)
			throws UnsupportedEncodingException, IOException {
		System.err.println(jsonStr);
		for (ClientThread client : this.client.getCurrentRoom().getClients()) {
			OutputStreamWriter broadOut = new OutputStreamWriter(client
					.getSocket().getOutputStream(), "UTF-8");
			broadOut.write(jsonStr + "\n");
			broadOut.flush();
		}
	}

	public ArrayList<String> getFormerId() {
		return formerId;
	}
}
