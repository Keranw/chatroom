package unimelb.comp90015.project1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import unimelb.comp90015.project1.keystore.*;

/**
 * @author kliu2 Main Thread for Server
 */
public class ChatServer {
	private static MainHall mainHall;
	private static HashMap<String, Integer> allUsedIds;
	private static HashMap<String, ClientInfo> clientInfoHash;
	private static Queue<String> unUsedIds;
	private static ArrayList<ClientThread> threadsList;

	private final static String clientTag = "guest-";
	private static Integer initialId = 1;

	public static void main(String[] args) throws IOException, ParseException {
		try {
			initialise();
			// parser the parameters from the command line
			ServerOptions serverOptions = new ServerOptions();
			CmdLineParser parser = new CmdLineParser(serverOptions);
			try {
				parser.parseArgument(args);
			} catch (CmdLineException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			Util util = new Util();
			SSLContext sc = util.getSSLServerContext();
			// Create SSL server socket factory, which creates SSLServerSocket instances
			ServerSocketFactory factory = sc.getServerSocketFactory();
			
			// Server is listening on port 4444
			try(SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(serverOptions.port)) {
				
//				serverSocket.setEnabledCipherSuites(Util.enabledCipherSuites);
				System.out.println("Server is listening...");

				// To store the all used Id and reUsed Ids
				allUsedIds = new HashMap<String, Integer>();
//				identitiesHash = new HashMap<String, String>();
				clientInfoHash = new HashMap<String, ClientInfo>();
				unUsedIds = new LinkedList<String>();
				threadsList = new ArrayList<ClientThread>();
				while (true) {
					// Server waits for a new connection
					Socket socket = serverSocket.accept();

					// A new thread is created per client
					try {
						createNewClient(socket);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		} catch (SocketException e) {

			 e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyManagementException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			// if (serverSocket != null) {
			// serverSocket.close();
			// }
		}
	}

	private static void initialise() {

		mainHall = new MainHall("mainhall");
	}

	/**
	 * Create a new client thread to listen each connected client
	 * 
	 * @param socket
	 * @throws InterruptedException
	 * @throws ParseException 
	 */
	private static void createNewClient(Socket socket)
			throws InterruptedException, ParseException {
		String newId = generateNewId();
		// Java creates new socket object for each connection.
		ClientThread clientThread = new ClientThread(socket, newId, mainHall, clientInfoHash);
		threadsList.add(clientThread);
		System.out.println(newId + "Connected");
	}

	/**
	 * Generate a new ID for a new connected client
	 * 
	 * @return ID
	 */
	private static String generateNewId() {
		String Id;
		checkUnusedIds();
		if (!unUsedIds.isEmpty()) {
			Id = unUsedIds.poll();
		} else {
			Id = clientTag + initialId;
			allUsedIds.put(Id, initialId);
			initialId++;
		}
		return Id;
	}

	/**
	 * Get Re-used IDs
	 */
	@SuppressWarnings("static-access")
	private static void checkUnusedIds() {
		ArrayList<String> formerNames = new ArrayList<String>();
		for (ClientThread thread : threadsList) {
			ArrayList<String> array = thread.getClientInfo().getFormerIds();
			if (array.size() > 0) {
				formerNames.addAll(array);
				thread.getClientInfo().clearFormerIds();
			}
		}
		HashMap<String, Integer> unUsedHash = new HashMap<String, Integer>();
		HashMap<String, Integer> sortedUnUsedHash = new HashMap<String, Integer>();
		for (String str : formerNames) {
			if (allUsedIds.containsKey(str)) {
				unUsedHash.put(str, Integer.valueOf(allUsedIds.get(str)));
			}
		}
		sortedUnUsedHash = sortByValues(unUsedHash);
		unUsedIds.addAll(sortedUnUsedHash.keySet());
	}

	/**
	 * Sort The Re-used IDS
	 * 
	 * @param map
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static HashMap sortByValues(HashMap map) {
		List list = new LinkedList(map.entrySet());
		// Defined Custom Comparator here
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		// Here I am copying the sorted list in HashMap
		// using LinkedHashMap to preserve the insertion order
		HashMap sortedHashMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedHashMap.put(entry.getKey(), entry.getValue());
		}
		return sortedHashMap;
	}

	/**
	 * @author kliu2
	 *
	 *         Command options -h hostname -p port number
	 */
	public static class ServerOptions {
		@Option(name = "-p", usage = "Give port num", required = false)
		private int port = 4444;
	}
}
