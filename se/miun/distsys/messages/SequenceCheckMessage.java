package se.miun.distsys.messages;

public class SequenceCheckMessage extends Message {

	public int sequenceNumber = 0;
	public int id = 0;

	public SequenceCheckMessage(int id, int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
		this.id = id;
	}
}
