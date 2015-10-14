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
		String cmdPattern = "^#\\w+ ?";
		String argumentPattern = " [\\w\\W]+";
		String msgPattern = "^\\w[\\w\\W ]*";

		Pattern cmd = Pattern.compile(cmdPattern);
		Pattern arg = Pattern.compile(argumentPattern);
		Pattern msg = Pattern.compile(msgPattern);
		Matcher cmdMatcher = cmd.matcher(command);
		Matcher argMatcher = arg.matcher(command);
		Matcher msgMatcher = msg.matcher(command);

		ArrayList<String> args = new ArrayList<String>();

		if (cmdMatcher.find()) {
			String type = cmdMatcher.group(0).toString();

			while (argMatcher.find()) {
				args.add(argMatcher.group(0).toString().trim());
			}
			constructJSON(type.trim().split("#")[1], args);
		}

		args.clear();
		if (msgMatcher.find()) {
			String message = msgMatcher.group(0).toString();
			args.add(message);
			constructJSON("message", args);
		}
	}

	/**
	 * construct json string
	 * @param type
	 * @param args
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static void constructJSON(String type, ArrayList<String> args)
			throws IOException {
		JSONObject requestObj = new JSONObject();
		requestObj.put("type", type);
		switch (type) {
		case "message":
			requestObj.put("content", args.get(0));
			break;
		case "identitychange":
			requestObj.put("identity", args.get(0));
			break;
		case "join":
			requestObj.put("roomid", args.get(0));
			break;
		case "who":
			requestObj.put("roomid", args.get(0));
			break;
		case "list":
			break;
		case "createroom":
			requestObj.put("roomid", args.get(0));
			break;
		case "kick":
			requestObj.put("identity", args.get(0));
			requestObj.put("roomid", args.get(1));
			requestObj.put("time", args.get(2));
			break;
		case "delete":
			requestObj.put("roomid", args.get(0));
			break;
		case "quit":
			break;
		}
		
		// send jsonstring to server 
		out.write((requestObj.toJSONString() + "\n"));
		out.flush();
	}
}
