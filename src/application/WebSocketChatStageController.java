package application;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.websocket.*;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

public class WebSocketChatStageController {
	@FXML TextField userTextField;
	@FXML TextArea chatTextArea;
	@FXML TextField messageTextField;
	@FXML ListView<String> attachmentsListView;
	@FXML Button btnSet;
	@FXML Button btnSend;
	@FXML Button btnChooseAndSendAttachment;
	@FXML ProgressIndicator fileDownloadPI;
	@FXML Label fileDownloadLbl;
	@FXML Label fileUploadLbl;
	
	
	private String user = "user";
	private WebSocketClient webSocketClient;
	private ArrayList<Message> incomingMessages;
	private long currPartToWrite = 0;
	private int chunkSize = 4096; //4 KiB
	private Boolean currentlySendingFile = false;
	private Boolean currentlyReceivingFile = false;
	
	@FXML private void initialize() {
		webSocketClient = new WebSocketClient();
		user = userTextField.getText();
		incomingMessages = new ArrayList<>();
		fileDownloadPI.setVisible(false);
		fileDownloadLbl.setVisible(false);
		fileUploadLbl.setVisible(false);
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
		File file = fileChooser.showOpenDialog(null);
		if(file != null) {
			if(currentlyReceivingFile || currentlySendingFile) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Błąd!");
				alert.setHeaderText("Nie udało się wysłać pliku");
				alert.setContentText("W tej chwili pobierany jest inny plik. Poczekaj na dokończenie pobierania i spróbuj wysłać swój plik ponownie.");
				alert.showAndWait();
				return;
			}
			
			long parts = (long) Math.ceil(file.length()/chunkSize);
			Message message = new Message("bin");
			message.setUser(user);
			message.setText(file.getName());
			message.setFileParts(parts);
			
			currentlySendingFile = true;
			fileDownloadPI.setVisible(true);
			fileUploadLbl.setVisible(true);
			btnChooseAndSendAttachment.setDisable(true);
			btnSend.setDisable(true);
			
			Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() {
				encodeAndSendText(message);
				prepareAndSendBinary(file,parts);
			
				fileDownloadPI.setVisible(false);
				fileUploadLbl.setVisible(false);
				btnChooseAndSendAttachment.setDisable(false);
				btnSend.setDisable(false);
				currentlySendingFile = false;
				
				return null;
				}
			};
			task.setOnSucceeded(e -> {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Sukces!");
				alert.setHeaderText("Plik wysłano pomyślnie");
				alert.setContentText("Plik \""+message.getText()+"\" wysłano pomyślnie.");
				alert.showAndWait();
			});
			new Thread(task).start();
				
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
           
           if(attachmentIndex<0) return;
           	FileChooser fileChooser = new FileChooser();
           	fileChooser.setInitialFileName(incomingMessages.get(attachmentIndex).getText());
	   		File file = fileChooser.showSaveDialog(null);
	   		if(file != null) {
				File tempFile = new File(System.getProperty("java.io.tmpdir") + incomingMessages.get(attachmentIndex).getText());
				tempFile.renameTo(file);
				incomingMessages.remove(attachmentsListView.getSelectionModel().getSelectedIndex());
				attachmentsListView.getItems().remove(attachmentsListView.getSelectionModel().getSelectedIndex());
	   		}
        }
    }
	
	private void encodeAndSendText(Message message) {
		JsonObjectBuilder objBuilder = Json.createObjectBuilder();
		objBuilder.add("type", message.getType())
		.add("user", message.getUser())
		.add("text", message.getText())
		.add("timestamp", message.getSentTimestamp());
		if(message.getType().equals("bin")) {
			objBuilder.add("fileParts", message.getFileParts());
		}
		JsonObject jsonMessage = objBuilder.build();
		webSocketClient.sendMessage(jsonMessage.toString());
		if(!message.getType().equals("bin"))
			chatTextArea.setText(chatTextArea.getText() + "["+message.getSentTimestamp()+" od CIEBIE]"+": "+message.getText() + "\n");
		else
			chatTextArea.setText(chatTextArea.getText() + "["+message.getSentTimestamp()+" od "+message.getUser()+"]"+" wysłałeś plik \""+message.getText()+"\"\n");
	}
	
	private void prepareAndSendBinary(File file,long parts)
	{
		RandomAccessFile aFile;
		try {
			aFile = new RandomAccessFile(file,"r");
	        FileChannel inChannel = aFile.getChannel();
	        ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
	        long currPart = 0;
	        while(inChannel.read(buffer) > 0)
	        {
	            buffer.flip();
	            Boolean isLast = false;
            	if(currPart>=parts) isLast = true;
                webSocketClient.sendMessage(buffer,isLast);
	            buffer.clear();
	            currPart++;
	        }
	        inChannel.close();
	        aFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendTextMessage() {
		if(currentlySendingFile || currentlyReceivingFile) return;
		Message message = new Message("text");
		message.setUser(user);
		message.setText(messageTextField.getText());
		encodeAndSendText(message);
		messageTextField.setText("");
	}
	
	private void encodeBackToMessageAndPopulate(String messageString) {
		try {
	        JsonObject jsonMessage = Json.createReader(new StringReader(messageString)).readObject();
	        Message message = new Message(jsonMessage.getString("type"));
	        message.setUser(jsonMessage.getString("user"));
	        message.setText(jsonMessage.getString("text"));
	        message.setSentTimestamp(jsonMessage.getString("timestamp"));
	        
	        if(message.getType().equals("text")) {
	        	chatTextArea.setText(chatTextArea.getText() + "["+message.getSentTimestamp()+" od "+message.getUser()+"]"+": "+message.getText() + "\n");
	        }
	        else if(message.getType().equals("bin")) {
	        	if(currentlySendingFile || currentlyReceivingFile) return;
	        	currentlyReceivingFile = true;
	        	fileDownloadPI.setVisible(true);
	        	fileDownloadLbl.setVisible(true);
				btnChooseAndSendAttachment.setDisable(true);
				btnSend.setDisable(true);
	        	message.setFileParts(jsonMessage.getInt("fileParts"));
	        	incomingMessages.add(message);
	        	currPartToWrite = 0;
	        }
	         
	    } catch (Exception e) {
	        System.out.println("error : " + e.getMessage());
	    }
	}
	
	private void saveFileToTemp(ByteBuffer data,Boolean isLast) {
		if(currentlySendingFile || !currentlyReceivingFile) return;
		
		Message fileInfo = incomingMessages.get(incomingMessages.size() - 1);
		RandomAccessFile aFile;
		try {
			aFile = new RandomAccessFile(System.getProperty("java.io.tmpdir") + fileInfo.getText(),"rw");
			aFile.seek(currPartToWrite*chunkSize);
	        FileChannel inChannel = aFile.getChannel();
	        inChannel.write(data);
            if(isLast) { //last chunk of file
            	currentlyReceivingFile = false;
            	fileDownloadPI.setVisible(false);
            	fileDownloadLbl.setVisible(false);
    			btnChooseAndSendAttachment.setDisable(false);
    			btnSend.setDisable(false);
    			if(currPartToWrite>=fileInfo.getFileParts())
    				Platform.runLater(() -> {
    					chatTextArea.setText(chatTextArea.getText() + "["+fileInfo.getSentTimestamp()+" od "+fileInfo.getUser()+"]"+" otrzymałeś plik \""+fileInfo.getText()+"\"\n");
    					attachmentsListView.getItems().add(fileInfo.getUser()+": "+fileInfo.getText());
    				});
    			else {
    				Platform.runLater(()->{
    					Alert alert = new Alert(AlertType.ERROR);
        				alert.setTitle("Błąd!");
        				alert.setHeaderText("Nie udało się pobrać pliku");
        				alert.setContentText("Plik nie został poprawnie pobrany. Prawdopodobnie wystąpiły problemy z połączeniem");
        				alert.showAndWait();
    				});
    			}
            }
	        currPartToWrite++;
	        inChannel.close();
	        aFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeSession(CloseReason closeReason) {
		try {
			webSocketClient.session.close(closeReason);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	@ClientEndpoint
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
		@OnMessage public void onMessage(String message, Session session) {
			//System.out.println("Message received: "+ message);
			encodeBackToMessageAndPopulate(message);
			
		}
		@OnMessage public void onMessage(ByteBuffer message,Boolean isLast, Session session) {
			//System.out.println("Message received: "+ message);
			//System.out.println("Binary received");
			saveFileToTemp(message,isLast);
		}
		
		private void connectToWebSocket() {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:8080/WebSocketServer/websocketserver");
				webSocketContainer.connectToServer(this, uri);
			} catch (DeploymentException | IOException e) { e.printStackTrace(); }
		}
		
		public void sendMessage(String message) {
			try {
				//System.out.println("Message sent: " + message);
				session.getBasicRemote().sendText(message);
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		public void sendMessage(ByteBuffer message,Boolean isLast) {
			try {
				//System.out.println("Binary sent");
				session.getBasicRemote().sendBinary(message,isLast);
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
}
