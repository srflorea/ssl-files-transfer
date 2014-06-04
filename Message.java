import java.io.Serializable;

/***
 * Class that contains the implementation of a message that will be sent thorugh the sockets.
 * @author razvan
 *
 */
public class Message implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Constants.MessageType msgType;
	private String messageBody;
	private byte[] file;
	
	public Message(Constants.MessageType msgType, String messageBody, byte[] file) {
		this.setMsgType(msgType);
		this.setMessageBody(messageBody);
		this.setFile(file);
	}

	public Constants.MessageType getMsgType() {
		return msgType;
	}

	public void setMsgType(Constants.MessageType msgType) {
		this.msgType = msgType;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}
}
