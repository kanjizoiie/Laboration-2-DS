package se.miun.distsys.messages;

public class JoinMessage extends Message {

	public int id = 0;
	
	public JoinMessage(int id) {
		this.id = id;
	}
}
