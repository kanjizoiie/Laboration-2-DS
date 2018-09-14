package se.miun.distsys.messages;

public class ChatMessage extends Message {

	public String chat = "";	
	public int id = 0;

	public ChatMessage(int id, String chat) {
		this.id = id;
		this.chat = chat;
	}
}
