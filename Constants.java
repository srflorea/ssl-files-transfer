/**
 * Class that contains program constants.
 * @author razvan
 *
 */
public class Constants {

	public static final String LIST 	 	 = "list";
	public static final String UPLOAD 	 	 = "upload";
	public static final String DOWNLOAD  	 = "download";

	public static final String FILESONSERVER = "files.txt";
	public static final String BANNED		 = "banned.txt";
	
	public static final String OWNERID		 = "CN";
	public static final String DEPARTMENT	 = "OU";
	
	public static final String KEY			 = "aaa"; 
	
	public static final String NOT			 = "Not";
	public static final String YEP			 = "Yep";
	
	public static enum MessageType {
		List, Upload, Download, Response, BAN, AUTHORIZE
	}
	
	public static enum Departments {
		Unknown, Contabilitate, HR, IT
	}
}
