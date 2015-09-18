package unimelb.comp90015.project1.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unimelb.comp90015.project1.server.ChatRoom;

public class ChatClient {
	private static boolean isQuit;
	private static boolean isFirstTime;
	private static Client client;

	public static void main(String[] args) {
		Socket socket = null;
		isQuit = false;
		isFirstTime = true;
		try {
			// connect to a server listening on port 4444 on localhost
			socket = new Socket("localhost", 4444);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), StandardCharsets.UTF_8));
			// Reading from console
			Scanner cmdin = new Scanner(System.in);

			while (!isQuit) {
				// forcing TCP to receive data immediately
				// TODO EOFException when server shuts down
				String response = in.readLine();

				if (response != null) {
					try {
						decodeResponse(socket, cmdin, response);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			in.close();
			socket.close();
			cmdin.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("null")
	private static void decodeResponse(Socket socket, Scanner cmdin,
			String response) throws InterruptedException, IOException {
		JSONParser parser = new JSONParser();
		String msg = null;
		try {
			JSONObject object = (JSONObject) parser.parse(response);
			// System.out.println(object.toJSONString());

			String type = object.get("type").toString();

			switch (type) {
			case "newidentity":
				// create a thread to send the messages to server
				if (isFirstTime) {
					isFirstTime = false;
					client = new Client(object.get("identity").toString());
					ChatRoom room = new ChatRoom();
					room.setRoomId("mainhall");
					client.setCurrentRoom(room);
					Thread sender = new Thread(new ClientSender(socket, cmdin,
							client));
					msg = String.format("Connected to %s as %s.", "localhost",
							object.get("identity").toString());
					System.out.println(msg);
					sender.start();
				} else {
					String newId = object.get("identity").toString();
					String oldId = object.get("former").toString();
					if (client.getClientName().equals(oldId)) {
						client.setClientName(newId);
					}
					msg = String.format("%s is now %s", oldId, newId);
					System.out.println(msg);
				}
				break;
			case "roomchange":
				String identity = object.get("identity").toString();
				String former = object.get("former").toString();
				String newRoom = object.get("roomid").toString();
				client.getCurrentRoom().setRoomId(newRoom);
				if (!former.equals("")) {
					msg = String.format("%s moves from %s to %s", identity,
							former, newRoom);
				} else {
					msg = String.format("%s moves to %s", identity, newRoom);
				}
				System.out.println(msg);
				break;
			case "roomlist":
				JSONArray rooms = (JSONArray) object.get("rooms");
				for (int i = 0; i < rooms.size(); i++) {
					JSONObject obj = (JSONObject) rooms.get(i);
					String content = String.format("%s: %d", obj.get("roomid")
							.toString(), Integer.valueOf(obj.get("count")
							.toString()));
					System.out.println(content);
				}
				break;
			case "roomcontents":
				String room = object.get("roomid").toString();
				String identities = object.get("identities").toString();
				msg = String.format("%s contains %s", room, identities);
				System.out.println(msg);
				break;
			case "message":
				String id = object.get("identity").toString();
				String content = object.get("content").toString();
				msg = String.format("%s: %s", id, content);
				System.out.println(msg);
				break;
			case "error":
				String errorContent = object.get("content").toString();
				System.err.println(errorContent);
				break;
			case "success":
				String successContent = object.get("content").toString();
				System.out.println(successContent);
				break;
			case "quit":
				System.err.println(object.get("identity").toString()
						+ " has quited");
				isQuit = true;
				socket.close();
				System.exit(0);
				break;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		System.out.print(String.format("[%s] %s >", client.getCurrentRoom()
				.getRoomId(), client.getClientName()));
		return;
	}
}
