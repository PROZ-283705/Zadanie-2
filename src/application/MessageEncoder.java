package application;

import javax.json.Json;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;


public class MessageEncoder implements Encoder.Text<Message> {
	
	@Override
    public String encode(final Message message) throws EncodeException {
        return Json.createObjectBuilder()
                       .add("type", message.getType())
                       .add("text", message.getText())
                       .add("binary", message.getBinaryAsString())
                       .build().toString();
        
    }
	
	 @Override
	  public void init(EndpointConfig ec) {
	    System.out.println("MessageEncoder - init method called");
	  }

	  @Override
	  public void destroy() {
	    System.out.println("MessageEncoder - destroy method called");
	  }
}
