package unimelb.comp90015.project1.client;

public class ClientMain {
	public static void main(String[] args){
		Thread ChatClientListener = new Thread(new ChatClient(args));
		ChatClientListener.start();
	}
}
