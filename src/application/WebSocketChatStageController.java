package application;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import javax.websocket.*;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class WebSocketChatStageController {
	@FXML TextField userTextField;
	@FXML TextArea chatTextArea;
	@FXML TextField messageTextField;
	@FXML ListView<String> attachmentsListView;
	@FXML Button btnSet;
	@FXML Button btnSend;
	@FXML Button btnChooseAndSendAttachment;
	
	private String user;
	private WebSocketClient webSocketClient;
	private ArrayList<Message> incomingMessages;
	
	@FXML private void initialize() {
		webSocketClient = new WebSocketClient();
		user = userTextField.getText();
		incomingMessages = new ArrayList<>();
	}
	
	@FXML private void btnSet_Click() {
		if(userTextField.getText().isEmpty()) { return; }
		user = userTextField.getText();
	}
	
	@FXML private void btnSend_Click() {
		sendTextMessage();
	}
	
	@FXML private void messageTextField_KeyPressed(KeyEvent e) {
		if(e.getCode() == KeyCode.ENTER) {
			sendTextMessage();
		}
	}
	
	@FXML private void btnChooseAndSendAttachment_Click() {
		FileChooser fileChooser = new FileChooser();
		File file = fileChooser.showOpenDialog(new Stage());
		if(file != null) {
			ByteBuffer fileToSend = convertToByteBuffer(file);
			Message message = new Message("bin");
			message.setUser(user);
			message.setText(file.getName());
			message.setData(fileToSend);
			encodeAndSend(message);
		}
		else {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Uwaga!");
			alert.setHeaderText(null);
			alert.setContentText("Nie załadowano pliku");

			alert.showAndWait();
		}
	}
	
	@FXML public void attachmentsListView_doubleClick(MouseEvent click) {

        if (click.getClickCount() == 2) {
           Integer attachmentIndex = attachmentsListView.getSelectionModel().getSelectedIndex();
           
           	FileChooser fileChooser = new FileChooser();
           	fileChooser.setInitialFileName(incomingMessages.get(attachmentIndex).getText());
	   		File file = fileChooser.showSaveDialog(new Stage());
	   		if(file != null) {
	   			FileChannel wChannel;
				try {
					wChannel = new FileOutputStream(file).getChannel();
					wChannel.write(incomingMessages.get(attachmentIndex).getBinaryData());
		   			wChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	   		}
	   		else {
	   			Alert alert = new Alert(AlertType.WARNING);
	   			alert.setTitle("Uwaga!");
	   			alert.setHeaderText(null);
	   			alert.setContentText("Nie załadowano pliku");
	
	   			alert.showAndWait();
	   		}
        }
    }
	
	private void sendTextMessage() {
		Message message = new Message("text");
		message.setUser(user);
		message.setText(messageTextField.getText());
		encodeAndSend(message);
		messageTextField.setText("");
	}
	
	private ByteBuffer convertToByteBuffer(File file) {
		try {
			
			Integer fileLength = (int)file.length();
			byte[] bytesArray = new byte[fileLength];
			FileInputStream fis = new FileInputStream(file);
			fis.read(bytesArray);
			fis.close();
			
			ByteBuffer byteBuffer = ByteBuffer.allocate(fileLength);
			byteBuffer.put(bytesArray);
			return byteBuffer;
			
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	private void encodeAndSend(Message message) {
		ArrayList<String> listObj = new ArrayList<>();
		listObj.add(message.getType());
		listObj.add(message.getUser());
		listObj.add(message.getText());
		if(message.getType().equals("bin")) {
			listObj.add(message.getBinaryAsString());
		}
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(listObj);
			ByteBuffer bytes = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
			
			webSocketClient.sendMessage(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}                    
	}
	
	private void encodeBackToMessageAndPopulate(ByteBuffer messageString) {
		try {
	        ByteArrayInputStream bis = new ByteArrayInputStream(messageString.array());
	        ObjectInputStream ois = new ObjectInputStream(bis);
	        @SuppressWarnings("unchecked")
			ArrayList<String> list = (ArrayList<String>) ois.readObject();
	        Message message = new Message(list.get(0));
	        message.setUser(list.get(1));
	        message.setText(list.get(2));
	        
	        if(message.getType().equals("text")) {
	        	System.out.println(message.getText());
	        	chatTextArea.setText(chatTextArea.getText() + message.getUser()+": "+message.getText() + "\n");
	        }
	        else if(message.getType().equals("bin")) {
	        	attachmentsListView.getItems().add(message.getUser()+": "+message.getText());
	        	message.setBinaryFromString(list.get(3));
	        	incomingMessages.add(message);
	        }
	         
	    } catch (Exception e) {
	        System.out.println("error : " + e.getMessage());
	    }
	}
	
	public void closeSession(CloseReason closeReason) {
		try {
			webSocketClient.session.close(closeReason);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	@ClientEndpoint//(encoders = MessageEncoder.class, decoders = MessageDecoder.class)
	public class WebSocketClient {
		private Session session;
		public WebSocketClient() { connectToWebSocket(); }
		
		@OnOpen public void onOpen(Session session) {
			System.out.println("Connection is open");
			this.session = session;
		}
		@OnClose public void onClose(CloseReason closeReason) {
			System.out.println("Connection is closed due to: "+closeReason.getReasonPhrase());
		}
		@OnMessage public void onMessage(ByteBuffer message, Session session) {
			//System.out.println("Message received: "+ message);
			encodeBackToMessageAndPopulate(message);
			
		}
		
		private void connectToWebSocket() {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:8080/WebSocketServer/websocketserver");
				webSocketContainer.connectToServer(this, uri);
			} catch (DeploymentException | IOException e) { e.printStackTrace(); }
		}
		
		public void sendMessage(ByteBuffer message) {
			try {
				//System.out.println("Message sent: " + message);
				session.getBasicRemote().sendBinary(message);
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
}
