package se.miun.distsys.messages;

public class OKMessage extends Message {
	
	public int id = 0;

	// The id is the recipients id
	public OKMessage(int id) {
		this.id = id;
	}
}
