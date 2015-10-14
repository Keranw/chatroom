package unimelb.comp90015.project1.server;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

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

	private static MainHall mainHall;
	private ClientThreadHandler handler;
	private static Thread handlerThread;
	
//	private OutputStreamWriter out;

	/**
	 * Constructor
	 * start a thread to listen this client, receiving and sending messages
	 * @param socket
	 * @param id
	 * @param _mainHall
	 */
	public ClientThread(Socket socket, String id, MainHall _mainHall) {
		this.socket = socket;

//		clientId = id;
		clientName = id;
		currentRoom = new ChatRoom();
		ownerRooms = new ArrayList<ChatRoom>();

		mainHall = _mainHall;
		
		this.handler = new ClientThreadHandler(socket, this, this.mainHall);
		handlerThread = new Thread(this.handler);
		handlerThread.setDaemon(true);
		handlerThread.start();
	}

	// stop this thread
	public static void interruptThread() {
		handlerThread.interrupt();
	}
	
	
	
//	public String getClientId() {
//		return clientId;
//	}
//
//	public void setClientId(String clientName) {
//		clientId = clientName;
//	}
	
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
	}

}
