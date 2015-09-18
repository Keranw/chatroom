package unimelb.comp90015.project1.server;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class ClientThread implements Runnable {
	private Socket socket;

	// Client Info
	private String clientId;
	private String clientName;
	private ChatRoom currentRoom;
	private ArrayList<ChatRoom> ownerRooms;

	private boolean isFirstLog;
	private static MainHall mainHall;
	private ClientThreadHandler handler;
	private BufferedReader in;
	private OutputStreamWriter out;

	public ClientThread(Socket socket, String id, MainHall _mainHall) {
		this.socket = socket;

		clientId = id;
		clientName = id;
		currentRoom = new ChatRoom();
		ownerRooms = new ArrayList<ChatRoom>();

		this.isFirstLog = true;
		mainHall = _mainHall;

		try {
			this.in = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "UTF-8"));
			this.out = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.handler = new ClientThreadHandler(socket, this, this.mainHall,
				this.out);
	}

	@Override
	public void run() {
		try {
			try {
				while (!socket.isClosed()) {
					if (this.isFirstLog) {
						this.isFirstLog = false;
						System.out.println("ClientThread.run()" + clientId);
						handler.sendFirstId();
					}
					String msg = in.readLine();
					System.out.println("receive message from " + clientId
							+ ": " + msg);

					String type = handler.decodeRequestJSON(msg);

					if (type == null || type.equals("quit")) {
						break;
					}
				}
			} catch (EOFException e) {
				closeSocket();
			} catch (SocketException s) {
				closeSocket();
			}
		} catch (IOException e) {
			e.printStackTrace();
			closeSocket();
		}

		// A thread finishes if run method finishes
	}

	private void closeSocket() {
		try {
			if (socket != null) {

				socket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Client disconnected.");
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientName) {
		clientId = clientName;
	}

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
