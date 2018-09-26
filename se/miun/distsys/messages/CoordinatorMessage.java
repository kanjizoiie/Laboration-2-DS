package se.miun.distsys.messages;

public class CoordinatorMessage extends Message {
	
	public int id = 0;

	public CoordinatorMessage(int id) {
		this.id = id;
	}
}
