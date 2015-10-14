package unimelb.comp90015.project1.client;

import java.io.BufferedReader;
import java.io.EOFException;
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
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import unimelb.comp90015.project1.server.ChatRoom;

/**
 * @author kliu2 
 * the main entrance for client, always to receive input from
 * server and print formated json string on standard output 
 */
public class ChatClient {
	private static boolean isQuit;
	private static boolean isFirstTime;
	private static Client client;

	private static Thread senderThread;
	private static ClientSender sender;

	private static CmdOptions cmdOptions;

	public static void main(String[] args) throws IOException {
		Socket socket = null;
		isQuit = false;
		isFirstTime = true;
		try {
			// parser the parameters from the command line
			cmdOptions = new CmdOptions();
			CmdLineParser parser = new CmdLineParser(cmdOptions);
			try {
				parser.parseArgument(args);
			} catch (CmdLineException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// connect to a server listening on port 4444 on localhost
			socket = new Socket(cmdOptions.hostname, cmdOptions.port);
			// // connect to a server listening on port 4444 on localhost
			// socket = new Socket("localhost", 4444);
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
		} catch (EOFException e) {
			// sent a quit command to server if exception occurs
			sender.constructJSON("quit", null);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * decode the response from server and show structured messages in standard
	 * outstream
	 * 
	 * @param socket
	 * @param cmdin
	 *            command input
	 * @param response
	 *            JSON message from server
	 * @throws InterruptedException
	 * @throws IOException
	 */
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
				// create a local client variable to store current status
				if (isFirstTime) {
					isFirstTime = false;
					client = new Client(object.get("identity").toString());
					ChatRoom room = new ChatRoom();
					room.setRoomId("mainhall");
					client.setCurrentRoom(room);

					sender = new ClientSender(socket, cmdin, client);
					senderThread = new Thread(sender);
					msg = String.format("Connected to %s as %s.",
							cmdOptions.hostname, object.get("identity")
									.toString());
					System.out.println(msg);

					senderThread.start();
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

				if (client.getClientName().equals(identity)) {
					client.getCurrentRoom().setRoomId(newRoom);
				}

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
			case "quit":
				String ID = object.get("identity").toString();
				System.err.println(ID + " has quited");
				if (client.getClientName().equals(ID)) {
					isQuit = true;

					cmdin.close();
					socket.close();
					// thread interrupt
					senderThread.interrupt();
				}
				break;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		System.out.print(String.format("[%s] %s >", client.getCurrentRoom()
				.getRoomId(), client.getClientName()));
		return;
	}

	/**
	 * @author kliu2
	 *
	 *         Command options -h hostname -p port number
	 */
	public static class CmdOptions {
		@Argument(index=0, usage = "Give hostname", required = true)
		private String hostname;
		@Option(name = "-p", usage = "Give port num", required = false)
		private int port = 4444;
	}
}
