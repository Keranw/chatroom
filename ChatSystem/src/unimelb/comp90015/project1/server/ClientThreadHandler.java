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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author kliu2
 * A handler to receive and response clients
 */
public class ClientThreadHandler implements Runnable {
	private Socket socket;
	private ClientThread _client;
	private boolean isFirstLog;
	private BufferedReader in;

	/**
	 * Constructor
	 * @param socket
	 * @param client
	 */
	public ClientThreadHandler(Socket socket, ClientThread client) {
		this.socket = socket;
		this._client = client;
		
		try {
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.isFirstLog = true;
	}

	@Override
	public void run() {
		try {
			try {
				while (!socket.isClosed()) {
					if (this.isFirstLog) {
						this.isFirstLog = false;
						this._client.sendFirstId();
					}
					String msg = in.readLine();
					System.out.println("receive message from "
							+ _client.getClientName() + ": " + msg);

					String type = decodeRequestJSON(msg);

					if (type == null || type.equals("quit")) {
						break;
					}
				}
//				interruptThread();
			} catch (EOFException e) {
				interruptThread();
				System.out.println("Client disconnected in EOFException");
				e.printStackTrace();
			} catch (SocketException s) {
//				interruptThread();
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
			this._client.quit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("thread is interrupted");
		this._client.interruptThread();
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
			if (this._client.checkArgs(object, "identity")) {
				String identity = object.get("identity").toString();
				this._client.changeId(identity);
			}
			break;
		case "join":
			if (this._client.checkArgs(object, "roomid")) {
				roomId = object.get("roomid").toString();
				this._client.joinRoom(roomId);
			}
			break;
		case "who":
			if (this._client.checkArgs(object, "roomid")) {
				roomId = object.get("roomid").toString();
				this._client.fetchRoomInfo(roomId);
			}
			break;
		case "list":
			this._client.generateRoomListMsg();
			break;
		case "createroom":
			if (this._client.checkArgs(object, "roomid")) {
				roomId = object.get("roomid").toString();
				this._client.createRoom(roomId);
			}
			break;
		case "kick":
			if (this._client.checkArgs(object, "identity") && this._client.checkArgs(object, "roomid")
					&& this._client.checkArgs(object, "time")) {
				String user = object.get("identity").toString();
				roomId = object.get("roomid").toString();
				Integer time = Integer.valueOf(object.get("time").toString());
				this._client.kickClientFromRoom(roomId, user, time);
			}
			break;
		case "delete":
			if (this._client.checkArgs(object, "roomid")) {
				roomId = object.get("roomid").toString();
				this._client.deleteRoom(roomId);
			}
			break;
		case "message":
			String content = object.get("content").toString();
			this._client.broadMessage(content);
			break;
		case "register":
			String newIdentity = object.get("identity").toString();
			String pasword = this._client.encyptPassword(object.get("password").toString());
//			identitiesHash.put(newIdentity, pasword);
			// TODO: return new identity
			this._client.changeId(newIdentity);
			break;
		case "login":
			String identity = object.get("identity").toString();
			String passwordHash = object.get("password").toString();
			// verify identity by comparing the password hash
//			String passwordInServer = identitiesHash.get(identity).toString();
//			if (passwordInServer.equals(passwordHash)) {
//				changeId(identity);
//			} else {
//				// TODO password is invalid, return error msg
//				generateSystemMsg("password is invalid");
//			}
			break;
		case "quit":
			this._client.quit();
			break;
		}

		return type;
	}
}
