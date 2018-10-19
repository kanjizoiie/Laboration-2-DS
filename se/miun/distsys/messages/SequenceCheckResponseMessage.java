package se.miun.distsys.messages;

public class SequenceCheckResponseMessage extends Message {

	public int sequenceNumber = 0;

	public SequenceCheckResponseMessage(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
}
