package se.miun.distsys.messages;

public class ChatMessage extends Message {

	public String chat = "";	
	public int id = 0;
	public int sequence = 0;

	public ChatMessage(int id, int sequence, String chat) {
		this.id = id;
		this.sequence = sequence;
		this.chat = chat;
	}
}
