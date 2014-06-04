import java.io.*;
import java.util.*;

/***
 * A manager that runs commands on server.
 * @author razvan
 *
 */
public class MyManager {
	
	/*
	 * Method that runs a command
	 */
	public void runCommand(Message message, ObjectInputStream ois, ObjectOutputStream oos, Map<String, String> clientIdentity) {
		if(message.getMsgType().equals(Constants.MessageType.List)) {		
			list(ois, oos, clientIdentity);
		}
		else if(message.getMsgType().equals(Constants.MessageType.Upload)) {
			upload(message, ois, oos, clientIdentity);
		}
		else if(message.getMsgType().equals(Constants.MessageType.Download)) {
			download(message, ois, oos, clientIdentity);
		}
	}
	
	/*
	 * Method that runs the command "list".
	 */
	private void list(ObjectInputStream ois, ObjectOutputStream oos, Map<String, String> clientIdentity) {
		StringBuilder filesString = new StringBuilder();
		for(String file : Server.files.keySet()) {
			filesString.append(file);
			filesString.append(" ");
		}
		
		Message newMessage = new Message(Constants.MessageType.Response, filesString.toString(), null);
		try {
			oos.writeObject(newMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* 
	 * Method that downloads a file if the user it is not banned
	 */
	private void download(Message message, ObjectInputStream ois, ObjectOutputStream oos, Map<String, String> clientIdentity) {
		Message newMessage = null;
		Message authMessage;
		String msgBody = "";
		msgBody += clientIdentity.get(Constants.OWNERID) + " ";
		msgBody += clientIdentity.get(Constants.DEPARTMENT) + " ";
		Map<String, String> fileId = Server.files.get(message.getMessageBody());
		msgBody += fileId.get(Constants.DEPARTMENT);
		authMessage = new Message(Constants.MessageType.AUTHORIZE, msgBody, null);
		
		Message response = null; 
		try {
			Server.oosForAuthS.writeObject(authMessage);
			response = (Message)Server.oisForAuthS.readObject();
		} catch (Exception e1) {
			e1.printStackTrace();
		}		
		
		
		if(response.getMessageBody().equals(Constants.YEP)) {
		
			byte[] mybytearray = null; 
			String path = "";
			path += "./server_files/" + message.getMessageBody();
			FileInputStream fis;
			try {
				fis = new FileInputStream(path);
				int length = fis.available();
				mybytearray = new byte[length];   
				    
				fis.read(mybytearray, 0, length);
				fis.close();
				
				
				newMessage = new Message(Constants.MessageType.Response, message.getMessageBody(), mybytearray);
				
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		else {
			newMessage = new Message(Constants.MessageType.Response, Constants.NOT, null);
		}
		
		/* send response to client: file or banned message */
		try {
			oos.writeObject(newMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Method that upload a file on server.
	 */
	private void upload(Message message, ObjectInputStream ois, ObjectOutputStream oos, Map<String, String> clientIdentity) {
		Message response = null;
		if(Server.bannedFileNames.contains(message.getMessageBody())) {
			Message banMessage;
			banMessage = new Message(Constants.MessageType.BAN, clientIdentity.get(Constants.OWNERID), null);
			try {
				Server.oosForAuthS.writeObject(banMessage);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			response = new Message(Constants.MessageType.Response, "Banned", null);
		}
		else {
			/* create file and write into it */
			String path = "./server_files/" + message.getMessageBody();
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(path);;
				fos.write(message.getFile());
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/* add file in files */
			addFile(message.getMessageBody(), ois, oos, clientIdentity);
			
			response = new Message(Constants.MessageType.Response, "OK", null);
		}
		/* send response to client */
		try {
			oos.writeObject(response);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void addFile(String fileName, ObjectInputStream ois, ObjectOutputStream oos, Map<String, String> clientIdentity) {
		/* add file in my data structure */
		Server.files.put(fileName, clientIdentity);
		
		String toWrite = "";
		for(Map.Entry<String, Map<String, String>> entry : Server.files.entrySet()) {
			toWrite += entry.getKey() + " ";
			toWrite += entry.getValue().get(Constants.OWNERID) + " ";
			toWrite += entry.getValue().get(Constants.DEPARTMENT);
			toWrite += "\n";
		}
		
		try {
		toWrite = Cipher.encrypt("aaa", toWrite);
		} catch (Exception e) {
			e.printStackTrace();
		}
		/* write the new file in the files file */
		FileWriter fw;
		try {
			fw = new FileWriter(Constants.FILESONSERVER);
			fw.write(toWrite);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		   
	}
}
