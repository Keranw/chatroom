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

public class ClientSender implements Runnable {
	private Socket socket;
	private Client client;
	private Scanner cmdin;
	private OutputStreamWriter out;

	public ClientSender(Socket socket, Scanner cmdin, Client client) {
		this.socket = socket;
		this.client = client;
		this.cmdin = cmdin;
	}

	@Override
	public void run() {
		try {
			// Preparing sending streams
			OutputStreamWriter out = new OutputStreamWriter(
					socket.getOutputStream(), StandardCharsets.UTF_8);
			while (!socket.isClosed()) {
				String msg = cmdin.nextLine();
				// forcing TCP to send data immediately

				if (msg != null || msg != "") {
					String json = encodeRequest(msg);
					System.out.println(json);
					if(!json.equals("{}")) {
						out.write((json + "\n"));
						out.flush();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String encodeRequest(String request) {
		JSONObject requestObj = parseCommand(request);

		return requestObj.toJSONString();
	}

	@SuppressWarnings("unchecked")
	private static JSONObject parseCommand(String command) {
		String cmdPattern = "^#\\w+ ?";
		String argumentPattern = " [\\w\\W]+";
		String msgPattern = "^\\w[\\w\\W ]*";

		Pattern cmd = Pattern.compile(cmdPattern);
		Pattern arg = Pattern.compile(argumentPattern);
		Pattern msg = Pattern.compile(msgPattern);
		Matcher cmdMatcher = cmd.matcher(command);
		Matcher argMatcher = arg.matcher(command);
		Matcher msgMatcher = msg.matcher(command);

		JSONObject requestObj = new JSONObject();

		if (cmdMatcher.find()) {
			String type = cmdMatcher.group(0).toString();
			requestObj.put("type", type.trim());
			ArrayList<String> args = new ArrayList<String>();
			while (argMatcher.find()) {
				args.add(argMatcher.group(0).toString().trim());
			}
			return constructJSON(requestObj, type.trim().split("#")[1], args);
		}

		if (msgMatcher.find()) {
			String message = msgMatcher.group(0).toString();
			requestObj.put("type", "message");
			requestObj.put("content", message);
		}
		return requestObj;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject constructJSON(JSONObject obj, String type,
			ArrayList<String> args) {
		JSONObject requestObj = obj;
		requestObj.put("type", type);
		switch (type) {
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
		return requestObj;
	}
}
