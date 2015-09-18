package unimelb.comp90015.project1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import unimelb.comp90015.project1.server.ClientThread;

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
			//Server is listening on port 4444
			serverSocket = new ServerSocket(4444);
			System.out.println("Server is listening...");
			allUsedIds = new HashMap<String, Integer>();
			unUsedIds = new LinkedList<String>();
			threadsList = new ArrayList<ClientThread>();
			while (true) {
				//Server waits for a new connection
				Socket socket = serverSocket.accept();
				
				// A new thread is created per client
				try {
					createNewClient(socket);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (serverSocket != null) {
				serverSocket.close();
			}
		}
	}
	
	private static void initialise(){
		
		mainHall = new MainHall("mainhall");
	}
	
	private static void createNewClient(Socket socket) throws InterruptedException {
		String newId = generateNewId();
		// Java creates new socket object for each connection.
		ClientThread clientThread = new ClientThread(socket, newId, mainHall);
		threadsList.add(clientThread);
		Thread client = new Thread(clientThread);
		client.setDaemon(true);
		// It starts running the thread by calling run() method
		client.start();
		System.out.println(newId + "Connected");
	}
	
	private static String generateNewId(){
		String Id;
		checkUnusedIds();
		if(!unUsedIds.isEmpty()) {
			Id = unUsedIds.poll();
		}
		else {
			Id = clientTag + initialId;
			allUsedIds.put(Id, initialId);
			initialId++;
		}
		return Id;
	}
	
	@SuppressWarnings("static-access")
	private static void checkUnusedIds() {
		ArrayList<String> formerNames = new ArrayList<String>();
		for(ClientThread thread : threadsList) {
			ArrayList<String> array = thread.getHandler().getFormerId();
			if(array.size() > 0) {
				formerNames.addAll(array);
			}
		}
		HashMap<String, Integer> unUsedHash = new HashMap<String, Integer>();
		HashMap<String, Integer> sortedUnUsedHash = new HashMap<String, Integer>();
		for(String str: formerNames) {
			if(allUsedIds.containsKey(str)) {
				unUsedHash.put(str, Integer.valueOf(allUsedIds.get(str)));
			}
		}
		sortedUnUsedHash = sortByValues(unUsedHash);
		unUsedIds.addAll(sortedUnUsedHash.keySet());
	}
	
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

}
