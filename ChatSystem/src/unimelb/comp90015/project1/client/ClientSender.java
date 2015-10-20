package unimelb.comp90015.project1.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.json.simple.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;

import com.sun.org.apache.bcel.internal.generic.LADD;

import unimelb.comp90015.project1.cypt.Crypto;

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
	private void parseCommand(String command) throws IOException {
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
	 * Using PBKDF2WithHmacSHA1 to generate a hash of password
	 * to authenticate the identity in server
	 * @param password
	 * @return a hash of password
	 */
	private String generatedSecuredPasswordHash(String identity, String passwordToHash) {
		String passwordHash = null;
		try {
			passwordHash = Crypto.generateStorngPasswordHash(passwordToHash, getSalt(identity));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println(passwordHash);
		return passwordHash;
	}
	
	private String getSalt(String identity) throws NoSuchAlgorithmException
    {
		// TODO get salt from disk if not generate a new one
		String saltStr = getSaltFromDisk(identity);
		if(saltStr == null) {
			SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
			byte[] salt = new byte[16];
			sr.nextBytes(salt);
			saltStr = salt.toString();
	        // TODO store the salt in disk for specific client
			storeSaltinDisk(identity, saltStr);
		}
        return saltStr;
    }
	
	private String getSaltFromDisk(String identity) {
		Map<String, String> ldapContent = new HashMap<String, String>();
		Properties properties = new Properties();
		String filename = identity + ".properties";
		File file = new File(filename);
		
		try {
			if(file.exists()) {
				properties.load(new FileInputStream(filename));
			} else {
				return null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (String key : properties.stringPropertyNames()) {
		   ldapContent.put(key, properties.get(key).toString());
		}
		return ldapContent.get(identity).toString();
	}
	
	private void storeSaltinDisk(String identity, String saltStr) {
		Map<String, String> ldapContent = new HashMap<String, String>();
		ldapContent.put(identity, saltStr);
		Properties properties = new Properties();
		String filename = identity + ".properties";
		
		for (Map.Entry<String,String> entry : ldapContent.entrySet()) {
		    properties.put(entry.getKey(), entry.getValue());
		}

		try {
			properties.store(new FileOutputStream(filename), null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * construct json string
	 * @param type
	 * @param args
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void constructJSON(String type, String[] args)
			throws IOException {
		JSONObject requestObj = new JSONObject();
		requestObj.put("type", type);
		switch (type) {
		case "message":
			requestObj.put("content", args[0]);
			break;
		case "register":
			// send a plain text of password when guest register
			String identity = args[1];
			String password = args[2];
			requestObj.put("identity", identity);
			requestObj.put("passwordHash", generatedSecuredPasswordHash(identity, password));
			
			break;
		case "login":
			// send password hash to server and server would verify the string
			// after guest has registered
			String _identity = args[1];
			String _password = args[2];
			requestObj.put("identity", args[1]);
			requestObj.put("passwordHash", generatedSecuredPasswordHash(_identity, _password));
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
