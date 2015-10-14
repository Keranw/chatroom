package unimelb.comp90015.project1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import unimelb.comp90015.project1.client.ChatClient.CmdOptions;
import unimelb.comp90015.project1.server.ClientThread;

/**
 * @author kliu2 Main Thread for Server
 */
public class ChatServer {
	private static MainHall mainHall;
	private static HashMap<String, Integer> allUsedIds;
	private static Queue<String> unUsedIds;
	private static ArrayList<ClientThread> threadsList;

	private final static String clientTag = "guest-";
	private static Integer initialId = 1;

	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = null;
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
			// Server is listening on port 4444
			serverSocket = new ServerSocket(serverOptions.port);
			System.out.println("Server is listening...");

			// To store the all used Id and reUsed Ids
			allUsedIds = new HashMap<String, Integer>();
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

		} catch (SocketException e) {

			// e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
	 */
	private static void createNewClient(Socket socket)
			throws InterruptedException {
		String newId = generateNewId();
		// Java creates new socket object for each connection.
		ClientThread clientThread = new ClientThread(socket, newId, mainHall);
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
			ArrayList<String> array = thread.getHandler().getFormerId();
			if (array.size() > 0) {
				formerNames.addAll(array);
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
