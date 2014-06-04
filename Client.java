/**
	Sisteme de programe pentru retele de calculatoare
	
	Univerity Politehnica of Bucharest, Romania
	Florea Stefan - Razvan
 */

import java.io.*;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Clasa ce implementeaza functionalitatea unui client ce se conecteaza la un server folosind TLS.
 * @author razvan
 *
 */
public class Client {

	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private SSLSocket s;
	
	public void createSSLConnection (String address, int port) throws Exception{

		// set up key manager to do server authentication
		String store=System.getProperty("KeyStore");
		String passwd =System.getProperty("KeyStorePass");

		SSLContext ctx;
		KeyManagerFactory kmf;
		KeyStore ks;
		char[] storepswd = passwd.toCharArray(); 
		ctx = SSLContext.getInstance("TLS");

		/* IBM or Sun vm ? */
		if ( System.getProperty("java.vm.vendor").toLowerCase().indexOf("ibm") != -1 ){
			kmf = KeyManagerFactory.getInstance("IBMX509","IBMJSSE");
		} else {
			kmf = KeyManagerFactory.getInstance("SunX509");
		}

		ks = KeyStore.getInstance("JKS");

		ks.load(new FileInputStream(store), storepswd);
		kmf.init(ks,storepswd);
		ctx.init(kmf.getKeyManagers(), null, null);
		SSLSocketFactory ssf = ctx.getSocketFactory();
		s = (SSLSocket)ssf.createSocket();

		s.connect(new InetSocketAddress(address, port));

		oos = new ObjectOutputStream(s.getOutputStream());
		ois = new ObjectInputStream(s.getInputStream());

	} //createSSLConnection

	public void close() {
		if (oos != null){
            try {
                oos.close();
            }catch(Throwable tt){
            }
        }
        if (ois != null){
            try {
                ois.close();
            }catch(Throwable tt){
            }
        }
        if ( s != null){
            try {
                s.close();
            } catch (Throwable t){
            }
        }
	}

	public static void main(String args[]) {

		if (args == null || args.length < 2) {
			System.out.println("Nu au fost furnizate adresa si portul serverului");
			return;
		}

		System.setProperty("javax.net.ssl.trustStore", "keys/client.ks");

		String host = args[0];
		int port = 0;
		try {
			port = Integer.parseInt(args[1]);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		Client c = new Client();
		Scanner scanner = null;
		try {
			c.createSSLConnection(host, port);
		
			scanner = new Scanner(System.in);;
			while(true) {
				
				System.out.println("Enter command!");
				String command = scanner.nextLine();
				String[] commArgs = command.split(" ");
				
				Message commandMessage, response;
				if(commArgs[0].equals(Constants.LIST)) {
					/* list */
					commandMessage = new Message(Constants.MessageType.List, null, null);
					c.oos.writeObject(commandMessage);
					response = (Message)c.ois.readObject();
					System.out.println("lista: " + response.getMessageBody());
				}
				/* upload */
				else if(commArgs[0].equals(Constants.UPLOAD)) {
					String path = "./client_files/" + commArgs[1];
					FileInputStream fis = null;
					try {
						fis = new FileInputStream(path);
					} catch(FileNotFoundException e) {
						System.out.println("File not found!!!");
						continue;
					}
				    int length = fis.available();
				    byte[] mybytearray = new byte[length];   
				    
				    fis.read(mybytearray, 0, length);
				    fis.close();
				    
					commandMessage = new Message(Constants.MessageType.Upload, commArgs[1], mybytearray);
					c.oos.writeObject(commandMessage);
					
					response = (Message)c.ois.readObject();
					if(response.getMessageBody().equals("Banned")) {
						System.out.println("You've been banned!!!");
					}
					else {
						System.out.println("Upload finished ok!");
					}
				}
				else if(commArgs[0].equals(Constants.DOWNLOAD)) {
					commandMessage = new Message(Constants.MessageType.Download, commArgs[1], null);
					c.oos.writeObject(commandMessage);
					response = (Message)c.ois.readObject();
					
					/* check the response */
					if(response.getMessageBody().equals(Constants.NOT)) {
						System.out.println("You are no allowed to download this file!!!");
					}
					else {
						/* create file and write the into it */
						String path = "./client_files/" + response.getMessageBody();
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(path);
							fos.write(response.getFile());
							fos.close();
						} catch (FileNotFoundException e) {
							System.out.println("File not found!");
							continue;
						}
						
						System.out.println("Download finished ok!");
					}
				}
			}
		} catch (Exception e) {
			scanner.close();
			c.close();
			e.printStackTrace();
		}
	}

} // end of class Client


