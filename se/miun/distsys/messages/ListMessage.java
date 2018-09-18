package se.miun.distsys.messages;

import java.util.Set;

public class ListMessage extends Message {

	public Set<Integer> clientList;
	
	public ListMessage(Set<Integer> clientList) {
		this.clientList = clientList;
	}
}
