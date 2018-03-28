package application;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class Message {
	private String user;
	private String type; //text or bin
	private String textData;
	private ByteBuffer binaryData;
	
	public Message(String type) { 
		this.type = type;		
	}
	
	public void setUser(String name) {
		user = name;
	}
	
	public void setText(String text) {
		textData = text;
	}
	
	public void setBinaryFromString(String binary) {
		try {
			ByteBuffer byteBuffer;
			byteBuffer = ByteBuffer.wrap(binary.getBytes("UTF-8"));
			binaryData = byteBuffer;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void setData(ByteBuffer binary) {
		binaryData = binary;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getType() {
		return type;
	}
	
	public String getText() {
		return textData;
	}
	
	public String toString() {
		return textData;
	}
	
	public ByteBuffer getBinaryData() {
		return binaryData;
	}
	
	public String getBinaryAsString() {
		try {
			return new String(binaryData.array(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
