package se.miun.distsys.messages;

public class SequenceCheckResponseMessage extends Message {

	public boolean run = true;

	public SequenceCheckResponseMessage(boolean run) {
		this.run = run;
	}
}
