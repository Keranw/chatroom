package unimelb.comp90015.project1.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author kliu2 
 * A Sender Thread to send command to server should parse the command
 * from command line to JSON Strings
 *
 */
public class ClientSender implements Runnable {
	private Socket socket;
	private Client client;
	private Scanner cmdin;
	private static OutputStreamWriter out;

	/**
	 * Constructor
	 * @param socket
	 * @param cmdin
	 * @param client
	 */
	public ClientSender(Socket socket, Scanner cmdin, Client client) {
		this.socket = socket;
		this.client = client;
		this.cmdin = cmdin;
	}

	@Override
	public void run() {
		try {
			// Preparing sending streams
			out = new OutputStreamWriter(socket.getOutputStream(),
					StandardCharsets.UTF_8);
			while (!socket.isClosed()) {
				String msg = cmdin.nextLine();
				// forcing TCP to send data immediately

				if (msg != null || msg != "") {
					// parse command into json and send it to server
					parseCommand(msg);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Using regular expression to extract command and arguments
	 * @param command
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private static void parseCommand(String command) throws IOException {
		String type = null;
		if(command.matches("^#[\\w\\W]+")) {
			String[] params = command.trim().split("\\s+");
			
			type = params[0].split("#")[1];
			constructJSON(type, params);
		} else {
			type = "message";
			String[] param = new String[1];
			param[0] = command;
			constructJSON(type, param);
		}
	}
	
	/**
	 * Using SHA1 to generate a hash of password
	 * to authenticate the identity in server
	 * @param password
	 * @return a hash of password
	 */
	private static String encyptPassword(String password) {
		return DigestUtils.sha1Hex(password);
	}

	/**
	 * construct json string
	 * @param type
	 * @param args
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static void constructJSON(String type, String[] args)
			throws IOException {
		JSONObject requestObj = new JSONObject();
		requestObj.put("type", type);
		switch (type) {
		case "message":
			requestObj.put("content", args[0]);
			break;
		case "register":
			// send a plain text of password when guest register
			requestObj.put("identity", args[1]);
			requestObj.put("password", args[2]);
			break;
		case "login":
			// hash the password
			String passwordHash = encyptPassword(args[2]);
			// send password hash to server and server would verify the string
			// after guest has registered
			requestObj.put("identity", args[1]);
			requestObj.put("password", passwordHash);
			break;
		case "identitychange":
			requestObj.put("identity", args[1]);
			break;
		case "join":
			requestObj.put("roomid", args[1]);
			break;
		case "who":
			requestObj.put("roomid", args[1]);
			break;
		case "list":
			break;
		case "createroom":
			requestObj.put("roomid", args[1]);
			break;
		case "kick":
			requestObj.put("identity", args[1]);
			requestObj.put("roomid", args[2]);
			requestObj.put("time", args[3]);
			break;
		case "delete":
			requestObj.put("roomid", args[1]);
			break;
		case "quit":
			break;
		}
		
		// send jsonstring to server 
		out.write((requestObj.toJSONString() + "\n"));
		out.flush();
	}
}
