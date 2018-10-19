package se.miun.distsys.messages;

import java.util.Set;

public class ListMessage extends Message {
	public int id;

	public Set<Integer> clientList;
	
	public ListMessage(int id, Set<Integer> clientList) {
		this.id = id;
		this.clientList = clientList;
	}
}
