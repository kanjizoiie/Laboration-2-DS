package se.miun.distsys.messages;

public class SequenceCheckMessage extends Message {

	public int sequenceNumber = 0;

	public SequenceCheckMessage(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
}
