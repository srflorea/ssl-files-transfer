/**
	Sisteme de programe pentru retele de calculatoare
	
	Copyright (C) 2008 Ciprian Dobre & Florin Pop
	Univerity Politehnica of Bucharest, Romania

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 */

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.cert.*;

import java.util.*;

/**
 * O clasa server ce accepta conexiuni TLS.
 * @author Dobre Ciprian
 *
 */
public class Server implements Runnable {

	/** Logger used by this class */
	private static final transient Logger logger = Logger.getLogger("capV.example3.Server");
	
	// variabila ce este folosita pentru testarea conditiei de oprire
	protected volatile boolean hasToRun = true;
	// socketul server
	protected ServerSocket ss = null;
	protected SSLSocket ass = null; 
	
	// un pool de threaduri ce este folosit pentru executia secventelor de operatii corespunzatoare
	// conextiunilor cu fiecare client
	final protected ExecutorService pool;
	final private ThreadFactory tfactory;
	
	public static Map<String, Map<String, String>> files;
	public MyManager manager;
	
	public static ObjectOutputStream oosForAuthS;
	public static ObjectInputStream oisForAuthS;
	
	public static List<String> bannedFileNames;
	
	/**
	 * Constructor.
	 * @param port Portul pe care va asculta serverul
	 * @throws Exception
	 */
	public Server(int portForClients, int portForAuthorizationService, String authServiceHost) throws Exception {
		bannedFileNames = new LinkedList<String>(Arrays.asList("bomba", "grenada"));
		files = new ConcurrentHashMap<String, Map<String, String>>();
		populateFiles();
		
		manager = new MyManager();
		
		// set up key manager to do server authentication
		String store=System.getProperty("KeyStore");
		String passwd =System.getProperty("KeyStorePass");
		
		/* connect to authorization service */
		createSSLConnection(authServiceHost, portForAuthorizationService, store, passwd);

		ss = createServerSocket(portForClients, store, passwd);
		tfactory = new DaemonThreadFactory();
		pool = Executors.newCachedThreadPool(tfactory);
	}
	
	private void populateFiles() {
		FileInputStream fis;
		byte[] mybytearray = null;
		try {
			fis = new FileInputStream(Constants.FILESONSERVER);
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
			Map<String, String> attrs = new HashMap<String, String>();
			attrs.put(Constants.OWNERID, elements[1]);
			attrs.put(Constants.DEPARTMENT, elements[2]);
			
			files.put(elements[0], attrs);
		}
	}
	
	/*
	 * Function that creates the secured connection with the authorization service
	 */
	public void createSSLConnection (String address, int port, String store, String passwd) throws Exception{

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
		ass = (SSLSocket)ssf.createSocket();
		ass.connect(new InetSocketAddress(address, port));
		
		oosForAuthS = new ObjectOutputStream(ass.getOutputStream());
		oisForAuthS = new ObjectInputStream(ass.getInputStream());
	} //createSSLConnection
	
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
			if (logger.isLoggable(Level.FINER))
				logger.log(Level.FINER, "Server keys loaded");

			ctx.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
			ssf = ctx.getServerSocketFactory();
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "Creating SSocket");
			}
			ss = (SSLServerSocket) ssf.createServerSocket();

			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "SSocket created!");
			}

			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "SSocket binding on port " + port);
			}
			ss.bind(new InetSocketAddress(port));
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "SSocket bounded on port " + port);
			}
			// this socket will not try to authenticate clients based on X.509 Certificates			
			ss.setNeedClientAuth(true);
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "SSocket FINISHED ok! Bounded on " + port);
			}

		} catch (Throwable t) {
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, "Got Exception", t);
			}
			t.printStackTrace();
			throw new IOException(t.getMessage());
		}
		return ss;
	}

	/**
	 * Metoda run ... accepta conexiuni si initiaza noi threaduri pentru fiecare conexiune in parte
	 */
	public void run() {
		if (logger.isLoggable(Level.INFO))
			logger.log(Level.INFO, "TLSServerSocket entering main loop ... ");
		while (hasToRun) {
			try {
				SSLSocket s = (SSLSocket)ss.accept();		
				
				s.setTcpNoDelay(true);	
				//add the client connection to connection pool
				pool.execute(new ClientThread(s));
				if (logger.isLoggable(Level.INFO))
					logger.log(Level.INFO, "New client connection added to connection-pool",s);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/**
	 * Metoda poate fi folosita pentru oprirea serverului
	 */
	public void stop() {
		hasToRun = false;
		try {
			ss.close();
		} catch (Exception ex) {}
		ss = null;
	}
	
	/**
	 * Custom thread factory used in connection pool
	 */
	private final class DaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			return thread;
		}
	}
	
	/**
	 * Clasa ce implementeaza functionalitatea conexiunii cu un anumit client
	 * @author Dobre Ciprian
	 *
	 */
	private final class ClientThread implements Runnable {
		
		private ObjectOutputStream oos;
		private ObjectInputStream ois;
		private SSLSocket s;
		private Map<String, String> clientIdentity;

		public ClientThread(SSLSocket s) {
			try {
				oos = new ObjectOutputStream(s.getOutputStream());
				ois = new ObjectInputStream(s.getInputStream());
				this.s = s;
			} catch (Exception e) { }
			
			clientIdentity = new HashMap<String, String>();
			
			/* get certificate attributes */
			X509Certificate[] certificate = null;
			try {
				certificate = s.getSession().getPeerCertificateChain();
			} catch (SSLPeerUnverifiedException e1) {
				e1.printStackTrace();
			}
			
			String cert = certificate[0].getSubjectDN().getName();
			
			String[] strings = cert.split(", ");
			for(String str : strings) {
				String[] clIdEntry = str.split("=");
				clientIdentity.put(clIdEntry[0], clIdEntry[1]);
			}
		}

		public void close() {
			if (ois != null){
	            try {
	                ois.close();
	            }catch(Throwable tt){
	            }
	        }
	        if (oos != null){
	            try {
	                oos.close();
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

		public void run() {
			
			// run indefinetely until exception
			while (true) {
				try {
					System.out.println("Reading command...");
					Message message = (Message)ois.readObject();
					System.out.println("Command received!");
					manager.runCommand(message, ois, oos, clientIdentity);
				} catch (Exception e) {
					break;
				}
			}
			close();
		}
	}
	
	public static void main(String args[]) {
		if (args == null || args.length < 1) {
			System.out.println("Nu a fost furnizat ca argument portul");
			return;
		}

		System.setProperty("javax.net.ssl.trustStore", "keys/client.ks");
				
		try {
			int portToListenForClients     	= Integer.parseInt(args[0]);
			int portForAuthorizationService = Integer.parseInt(args[1]);
			String authServiceHost 			= args[2]; 
			(new Thread(new Server(portToListenForClients, portForAuthorizationService, authServiceHost))).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
