package unimelb.comp90015.project1.server;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.comp90015.project1.cypt.Crypto;

/**
 * @author kliu2
 * The Client Model stored in server
 */
public class ClientThread {
	private Socket socket;

	// Client Info
	private ClientInfo clientInfo;

//	private HashMap<String, String> identitiesHash;
	private HashMap<String, ClientInfo> clientInfoHash;

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
	 * @throws ParseException 
	 */
	public ClientThread(Socket socket, String id, MainHall _mainHall, 
			HashMap<String, ClientInfo> clientInfoHash) {
		this.socket = socket;

		clientInfo = new ClientInfo();
		clientInfo.setClientName(id);

		mainHall = _mainHall;
		
		try {
			outputStream = new OutputStreamWriter(socket.getOutputStream(),
					StandardCharsets.UTF_8);
///////////////////DH exchange////////////////////////////////
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			BigInteger base = generateRandom(3);
			BigInteger modulo = generateRandom(2048);
			BigInteger privateKey = generateRandom(2048);
			BigInteger temp = modExp(base, privateKey, modulo);
			JSONObject ExRequest = new JSONObject();
			ExRequest.put("base", base.toString());
			ExRequest.put("modulo", modulo.toString());
			ExRequest.put("temp", temp.toString());
			outFlush(outputStream, (ExRequest.toJSONString()));
			String ans = in.readLine();
			JSONParser parsor = new JSONParser();
			JSONObject result = (JSONObject)parsor.parse(ans);
			BigInteger cTemp = new BigInteger(result.get("cTemp").toString());
			BigInteger sharedKey = modExp(cTemp, privateKey, modulo);
			System.out.println(sharedKey);
///////////////////////////////////////////////////////////////////
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		this.identitiesHash = identitiesHash;
		this.clientInfoHash = clientInfoHash;
		
		this.handler = new ClientThreadHandler(socket, this);
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
	/// 	DH exchange		 //
	///						 //	
	///////////////////////////
	private BigInteger generateRandom(int size){
		Random rnd = new Random();
		BigInteger result = new BigInteger(size, rnd);
		return result;
	}
	
	private BigInteger modExp(BigInteger base, BigInteger exp, BigInteger modulo) {
		BigInteger two = new BigInteger("2");
		if (exp.equals(BigInteger.ZERO)) {
			return BigInteger.ONE;
		} else {
			BigInteger temp = modExp(base, exp.divide(two), modulo);
			BigInteger result = (temp.multiply(temp)).mod(modulo);
			if (exp.mod(two).equals(BigInteger.ONE)) {
				result = (result.multiply(base)).mod(modulo);
			}
			return result;
		}
	}
	
	
	///////////////////////////
	///     				 //
	/// Getters and Setters  //
	///						 //	
	///////////////////////////
	public ClientInfo getClientInfo() {
		return clientInfo;
	}

	public void setClientInfo(ClientInfo clientInfo) {
		this.clientInfo = clientInfo;
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
			outMsg = this.generateNewIdentity("", clientInfo.getClientName());
			outFlush(outputStream, outMsg);
			// server join client to mainhall
			joinRoom("mainhall");
			fetchRoomInfo("mainhall");
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		// this.formerNames.add(this.client.getClientName());
		if (checkValidId(identity)) {
			informIdentity(identity);
		}
	}
	
	/**
	 * store identity: password in a hash map
	 * @param identity
	 * @param password
	 * @throws IOException
	 */
	public void storeIdentity(String identity, String password) throws IOException {
		if(checkAuthenticated(identity)) {
			generateSystemMsg(identity + " has logged in");
			return;
		}
		
		changeId(identity);
		clientInfo.setPassword(password);
		this.clientInfoHash.put(identity, clientInfo);
	}
	
	
	/**
	 * Match the password record with the hash received from client to verify the identity
	 * change the name after passing the verification
	 * return system error message to client if password is invalid
	 * @param identity
	 * @param password
	 * @throws IOException
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchAlgorithmException 
	 */
	public void verifyIdentity(String identity, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		if(!checkAuthenticated(identity)) {
			generateSystemMsg(identity + " hasn't registered yet");
			return;
		}
		
		String passwordInServer = clientInfoHash.get(identity).getPassword();
		if (password.equals(passwordInServer)) {
			informIdentity(identity);
			// restore client's info
			this.restoreClientInfo(identity);
		} else {
			// password is invalid, return error msg
			generateSystemMsg("password is invalid");
		}
	}
	
	/**
	 * check the identity is authenticated or not
	 * @param identity
	 * @return true if the identity is authenticated
	 * @return false if not
	 * @throws IOException 
	 */
	private boolean checkAuthenticated(String identity) throws IOException {
		if (this.clientInfoHash.get(identity) != null) {
			return true;
		}
		generateSystemMsg("client is not authenticated");
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private void informIdentity(String identity) throws IOException {
		// update the ownership
		if (clientInfo.getOwnerRooms().size() > 0) {
			for (ChatRoom room : clientInfo.getOwnerRooms()) {
				room.setOwnerId(identity);
			}
		}
		JSONObject obj = new JSONObject();
		obj.put("type", "newidentity");
		obj.put("former", clientInfo.getClientName());
		obj.put("identity", identity);
		// add clientId to reused id
		clientInfo.getFormerId().add(clientInfo.getClientName());
		clientInfo.setClientName(identity);
		// broad change information to all clients online
		for (ClientThread clientThread : this.mainHall.getAllClients()) {
			OutputStreamWriter clientOut = clientThread.getOutputStream();
			outFlush(clientOut, obj.toJSONString());
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
			if (identity.equals(client.getClientInfo().getClientName())) {
				// response invalid identity
				generateSystemMsg(String.format("%s is now %s",
						client.getClientInfo().getClientName(), identity));
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
		ChatRoom currentRoom = clientInfo.getCurrentRoom();
		ChatRoom requestedRoom = this.mainHall.getRoomById(roomId);
		if (requestedRoom == null) {
			// generate roomchange msg to individual
			if (currentRoom != null) {
				generateRoomChangeMsg(currentRoom.getRoomName(),
						currentRoom.getRoomName(),
						clientInfo.getClientName(), outputStream);
			} else {
				generateRoomChangeMsg("", "", clientInfo.getClientName(),
						outputStream);
			}
			generateSystemMsg("roomId is invalid or non existent");
		} else if (currentRoom.getRoomName() == null) {
			if (forbidClientToConnect(requestedRoom) > 0) {
				requestedRoom.getClients().add(this);
				clientInfo.setCurrentRoom(requestedRoom);
				broadcastToClients(requestedRoom, "",
						requestedRoom.getRoomName(),
						clientInfo.getClientName());
			}
		} else {
			if (forbidClientToConnect(requestedRoom) > 0) {
				currentRoom.removeClient(this);
				removeRoomFromMainhall(currentRoom);
				requestedRoom.addClient(this);
				clientInfo.setCurrentRoom(requestedRoom);
				broadcastToClients(currentRoom, currentRoom.getRoomName(),
						requestedRoom.getRoomName(),
						clientInfo.getClientName());
				broadcastToClients(requestedRoom, currentRoom.getRoomName(),
						requestedRoom.getRoomName(),
						clientInfo.getClientName());
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
		String futureTime = kickedUser.get(clientInfo.getClientName());
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
			newRoom.setOwnerId(clientInfo.getClientName());
			clientInfo.addRoom(newRoom);
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
			String clientName = client.getClientInfo().getClientName();
			if (clientName.equals(room.getOwnerId())) {
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
					client.getClientInfo().getClientName(), broadOut);
		}
	}

	/**
	 * broadcast room content message
	 * @param message
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void broadMessage(String message) throws IOException {
		if (clientInfo.getCurrentRoom().getClients() == null) {
			return;
		}
		JSONObject obj = new JSONObject();
		obj.put("type", "message");
		obj.put("content", message);
		obj.put("identity", clientInfo.getClientName());
		for (ClientThread client : clientInfo.getCurrentRoom().getClients()) {
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
		ArrayList<ChatRoom> rooms = clientInfo.getOwnerRooms();
		if (room != null && rooms.size() > 0
				&& clientInfo.getOwnerRooms().contains(room)) {
			// broadcast msgs to mainhall
			broadcastToMainHall(room, roomId, "mainhall");
			// all clients are moved to mainhall
			for (ClientThread client : room.getClients()) {
				client.getClientInfo().setCurrentRoom(mainHall);
				this.mainHall.addClient(client);
			}
			// remove room from client's ownerRooms
			clientInfo.getOwnerRooms().remove(room);
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
					clientInfo.getClientName()));
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
		ArrayList<ChatRoom> rooms = clientInfo.getOwnerRooms();
		if (room != null && rooms.size() > 0
				&& room.getOwnerId().equals(clientInfo.getClientName())
				&& client != null
				&& checkAuthenticated(clientInfo.getClientName())) {

			room.getKickedClients().put(clientId, calculateKickedTime(time));

			// remove client from the room and send a response message
			client.joinRoom("mainhall");
		} else {
			// return error response, room is invalid or user is invalid
			generateSystemMsg("invalid roomId or userId or user is not authenticated");
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
		clientInfo.getCurrentRoom().removeClient(this);
		// clear client info if not authenticated
		if(!this.checkAuthenticated(clientInfo.getClientName())) {
			this.clearClientInfo();
		}
		// save client info if it is
		else {
			// save client info
			this.saveClientInfo();
		}
		
		// inform all users
		JSONObject obj = new JSONObject();
		obj.put("type", "quit");
		obj.put("identity", clientInfo.getClientName());
		broadMsgToAll(obj.toJSONString());
		outFlush(outputStream, obj.toJSONString());
	}
	
	/**
	 * clear client's information if the client is not authenticated
	 * @throws IOException
	 */
	private void clearClientInfo() throws IOException {
		removeRoomFromMainhall(clientInfo.getCurrentRoom());
		for (ChatRoom room : clientInfo.getOwnerRooms()) {
			if (room.getClients().size() == 0) {
				System.out.println("Removed room: " + room.getRoomName());
				this.mainHall.removeRoom(room);
			}
			room.setOwnerId("");
		}
		// add clientId to reused id
		clientInfo.getFormerId().add(clientInfo.getClientName());
	}
	
	/**
	 * store client's info
	 */
	private void saveClientInfo() {
		clientInfoHash.put(clientInfo.getClientName(), clientInfo);
	}
	
	private void restoreClientInfo(String identity) {
		ClientInfo oldClientInfo = clientInfoHash.get(identity);
		clientInfo.restoreClientInfo(oldClientInfo);
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
		for (ClientThread client : clientInfo.getCurrentRoom().getClients()) {
			OutputStreamWriter broadOut = client.getOutputStream();
			outFlush(broadOut, jsonStr);
		}
	}

	private synchronized void outFlush(OutputStreamWriter _out, String str)
			throws IOException {
		
		_out.write(str + "\n");
		_out.flush();
	}

	private OutputStreamWriter getOutputStream() {
		return outputStream;
	}

	private void setOutputStream(OutputStreamWriter outputStream) {
		this.outputStream = outputStream;
	}

}
