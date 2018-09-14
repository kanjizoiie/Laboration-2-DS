package se.miun.distsys.messages;

public class LeaveMessage extends Message {

	public String chat = "";	
	
	public JoinMessage(String chat) {
		this.chat = chat;
	}
}
