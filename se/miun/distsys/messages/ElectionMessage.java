package se.miun.distsys.messages;

public class ElectionMessage extends Message {
	
	public int id = 0;

	public ElectionMessage(int id) {
		this.id = id;
	}
}
