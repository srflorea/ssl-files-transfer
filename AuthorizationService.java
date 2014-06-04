import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.*;
import java.util.*;

/***
 * Class that represents the authorization service for permissions of downloading file from server.
 * @author razvan
 *
 */
public class AuthorizationService implements Runnable {

	private SSLServerSocket ss;
	
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	
	private Map<String, String> banned;
	
	public AuthorizationService(int port) {
		banned = new HashMap<String, String>();
		readBanned();
		
		// set up key manager to do server authentication
		String store=System.getProperty("KeyStore");
		String passwd =System.getProperty("KeyStorePass");
		
		try {
			ss = createServerSocket(port, store, passwd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readBanned() {
		FileInputStream fis;
		byte[] mybytearray = null;
		try {
			fis = new FileInputStream(Constants.BANNED);
			int length = fis.available();
			mybytearray = new byte[length];   
		    
		    fis.read(mybytearray, 0, length);
		    fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    
		String all = Cipher.decrypt("aaa", new String(mybytearray));
		String[] entries = all.split("\\n");
		for(String entry : entries) {
			String[] elements = entry.split(" ");
			if(elements.length == 1)
				continue;
			banned.put(elements[0], elements[1]);
		}
	}
	/**
	 * Metoda ce creaza un nou server socket folosind un anumit keystore si parola
	 * @param port: port to listen on
	 * @param store: the path to keystore file containing server key pair (private/public key); if <code>null</code> is passed 
	 * @param passwd: password needed to access keystore file
	 * @return a SSL Socket bound on port specified
	 * @throws IOException
	 */
	public static SSLServerSocket createServerSocket(int port, String keystore, String password) throws IOException {
		SSLServerSocketFactory ssf = null;
		SSLServerSocket ss = null;
		try {
			SSLContext ctx;
			KeyManagerFactory kmf;
			KeyStore ks;
			ctx = SSLContext.getInstance("TLS");
			kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			FileInputStream is = new FileInputStream(keystore);
			ks.load(is, password.toCharArray());
			kmf.init(ks, password.toCharArray());

			ctx.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
			ssf = ctx.getServerSocketFactory();
			
			ss = (SSLServerSocket) ssf.createServerSocket();

			ss.bind(new InetSocketAddress(port));
			
			// this socket will not try to authenticate clients based on X.509 Certificates			
			ss.setNeedClientAuth(false);

		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException(t.getMessage());
		}
		return ss;
	}


	@Override
	public void run() {
		try {
			SSLSocket s = (SSLSocket)ss.accept();
			
			oos = new ObjectOutputStream(s.getOutputStream());
			ois = new ObjectInputStream(s.getInputStream());
			oos.flush();
			
			s.setTcpNoDelay(true);	
			
			System.out.println("Server connected!!!");
			
			while(true) {
				Message message = (Message)ois.readObject();
				if(message.getMsgType().equals(Constants.MessageType.BAN)) {
					banUser(message);
				}
				if(message.getMsgType().equals(Constants.MessageType.AUTHORIZE)) {
					authorizeUser(message);
				}
			}
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
	}
	
	/*
	 * Method that is authorizing or not a user to download a file from server
	 */
	private void authorizeUser(Message message) {
		Message response;
		response = new Message(Constants.MessageType.Response, null, null);
		String[] messageBody = message.getMessageBody().split(" ");
		String userName = messageBody[0];
		String userDept = messageBody[1];
		String fileDept = messageBody[2];
		if(banned.containsKey(userName)) {
			response.setMessageBody(Constants.NOT);
		}
		else if(Constants.Departments.valueOf(userDept).ordinal() 
				> Constants.Departments.valueOf(fileDept).ordinal()) {
			response.setMessageBody(Constants.NOT);
		}
		else {
			response.setMessageBody(Constants.YEP);
		}
		
		try {
			oos.writeObject(response);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Method that is banning a given user of downloading files from server
	 */
	private void banUser(Message message) {
		Date d = new java.util.Date();
		String date;
		date = d.getDay() + "/" + d.getMonth() + "/" + d.getYear();
		banned.put(message.getMessageBody(), date);
		
		/* build the string with banned users to be written in banned file  */
		String toWrite = "";
		for(Map.Entry<String, String> entry : banned.entrySet()) {
			toWrite += entry.getKey() + " ";
			toWrite += entry.getValue();
			toWrite += "\n";
		}
		
		System.out.println(toWrite);
		toWrite = Cipher.encrypt(Constants.KEY, toWrite);
		
		/* write banned users in banned file */
		FileWriter fw;
		try {
			fw = new FileWriter(Constants.BANNED);
			fw.write(toWrite);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		if (args == null || args.length < 1) {
			System.out.println("Nu a fost furnizat ca argument portul");
			return;
		}

		System.setProperty("javax.net.ssl.trustStore", "keys/server.ks");
				
		try {
			int port = Integer.parseInt(args[0]);
			(new Thread(new AuthorizationService(port))).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
