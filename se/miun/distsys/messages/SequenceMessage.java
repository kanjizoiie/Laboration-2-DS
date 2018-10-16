package se.miun.distsys.messages;

public class SequenceMessage extends Message {

	public int sequenceNumber = 0;

	public SequenceMessage(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
}
