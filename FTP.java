import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.StringTokenizer;

public class FTP {

	private static String currentPath = "/";
	private Socket sock,socketData;
	private BufferedWriter writer,writerData;
	private BufferedInputStream bis,bisData;
	int stream;
	byte[] b = new byte[1024];
	private boolean QUIT = false;
	Scanner sc;
	String listIp = "";
	int listPort = 0;
	

	public FTP(String serverAddress, int connectionPort) throws UnknownHostException, IOException {

		System.out.println("Creating the socket");
		sock = new Socket(InetAddress.getByName(serverAddress).getHostAddress(), connectionPort);
		writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		bis = new BufferedInputStream(sock.getInputStream());
		String response = read();
		if (!response.startsWith("220")) {
			throw new IOException("Connectiopn error");
		}
		loginToServer();
		manager();
	}

	public void loginToServer() throws IOException {
		String userRequest = "USER lyes";
		send(userRequest);
		String response = read();
		if (!response.startsWith("331")) {
			throw new IOException("Connection error");
		}

		String passRequest = "PASS root";
		send(passRequest);

		response = read();
		if (!response.startsWith("230")) {
			throw new IOException("Connection error");
		}else {
			log("You are connected to the server");
		}

	}
	
	public void manager() throws IOException {
		int choice = 0;
		String path = "";
		String showPath = "PWD";
		String targetPath = "CWD ";
		String newDirectory = "MKD ";
		String listDirectory = "LIST ";
		String sendFile = "put";
		sc = new Scanner(System.in);
		
		
		System.out.println("Connection established with the server");
		System.out.println("--------------------------------------");
		System.out.println("Please enter your command ");
		System.out.println("--------------------------------------");
		System.out.println("Enter 1 to see the current path ");
		System.out.println("Enter 2 to navigate to other directory ");
		System.out.println("Enter 3 to create new directory");
		System.out.println("Enter 4 to list the directory");
		System.out.println("Enter 5 to upload a file");
		System.out.println("Enter 6 to quit");
		
		
		while(!QUIT) {
			System.out.println("Choose your command");
			choice = sc.nextInt();
			switch(choice) {
			case 1:
				send(showPath);
				String response = read();
				if (!response.startsWith("257")) {
					System.out.println("Error contacting the server");
				}else {
					log(response);
				}
				break;
			case 2:
				Scanner sc = new Scanner(System.in);
				System.out.println("Enter the folder name");
				path = sc.nextLine();
				send(targetPath+path);
				response = read();	
				if(!response.startsWith("250")) {
					System.err.println(path+" directory doesn't exist");
				}else {
					currentPath = currentPath+"/"+path;
					log(response);
				}
				break;
			case 3:
				Scanner sc1 = new Scanner(System.in);
				path = sc1.nextLine();
				send(newDirectory+path);
				response = read();	
				if(!response.startsWith("257")) {
					System.err.println(path+" directory can't be created");
				}else {
					log(response);
				}
				break;
			case 4:
				send("TYPE ASCII");
				read();
				passive();
				dataSocket();
				send(listDirectory+" "+currentPath);
				response = listData();
				log(response);
				break;
			case 5:
				Scanner sc2 = new Scanner(System.in);
				send("TYPE ASCII");
				read();
				passive();
				System.out.println("Enter the file location");
				path = sc2.nextLine();
				send(sendFile+" "+path);
				response = read();
				break;
			case 6:
				quit();
				System.out.println("Connection closed successfully");
				break;
			
			}
		}
		
	}

	public void send(String command) throws IOException {
		command += "\r\n";
		writer.write(command);
		writer.flush();
	}

	private String read() throws IOException {
		String response = "";
		int stream;
		byte[] b = new byte[4096];
		stream = bis.read(b);
		response = new String(b, 0, stream);		
		return response;
	}
	
	private String listData() throws IOException {
		String response = "";
		byte[] b  = new byte[1024];
		int stream;
		
		while((stream = bisData.read(b))!=-1) {
			response+=new String(b,0,stream);
		}
		return response;
	}

	private void log(String str) {
		System.out.println(">> " + str);
	}
	
	
	public void quit() throws IOException {
		String quit = "QUIT";
		send(quit);
		String response = read();
		if(!response.startsWith("221")) {
			throw new IOException("Error when quiting the server");
		}
		sock.close();
		QUIT = true;
	}
	
	public void passive() throws IOException {
		String passive = "PASV";
		send(passive);
		String response = read();
		String canalIp = "";
		int canalPort = 0;
		
		int debut = response.indexOf('(');
	     int fin = response.indexOf(')', debut + 1);
	     if(debut > 0){
	        String dataLink = response.substring(debut + 1, fin);
	        StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
	        try {

	           canalIp = tokenizer.nextToken() + "." + tokenizer.nextToken() + "."
	                   + tokenizer.nextToken() + "." + tokenizer.nextToken();

	           canalPort = Integer.parseInt(tokenizer.nextToken()) * 256
	                + Integer.parseInt(tokenizer.nextToken());
	           listIp = canalIp;
	           listPort = canalPort; 
	          
	        } catch (Exception e) {
	          throw new IOException("SimpleFTP received bad data link information: "
	              + response);
	        }        
	     }
	}
	
	/* We create a new socket to recieve list of directory data */
	public void dataSocket() throws UnknownHostException, IOException {
		socketData = new Socket(listIp,listPort);
		writerData = new BufferedWriter(new OutputStreamWriter(socketData.getOutputStream()));
		bisData = new BufferedInputStream(socketData.getInputStream());
	}

}
