package application;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Message {
	private String user;
	private String type; //text or bin
	private String textData;
	private String sentTimestamp;
	private long fileParts;
	
	public Message(String type) { 
		this.type = type;
		this.fileParts = 0;
		
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		sentTimestamp = sdf.format(timestamp);
	}
	
	public void setUser(String name) {
		user = name;
	}
	
	public void setText(String text) {
		textData = text;
	}
	
	public void setFileParts(long parts) {
		fileParts = parts;
	}
	
	public void setSentTimestamp(String timestamp) {
		sentTimestamp = timestamp;
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
	
	public long getFileParts() {
		return fileParts;
	}
	
	public String getSentTimestamp() {
		return sentTimestamp;
	}
}
