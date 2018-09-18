package se.miun.distsys;

import java.util.Random;
import java.util.Set;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import se.miun.distsys.listeners.ChatMessageListener;
import se.miun.distsys.listeners.JoinMessageListener;
import se.miun.distsys.listeners.LeaveMessageListener;
import se.miun.distsys.listeners.ListMessageListener;
import se.miun.distsys.messages.ChatMessage;
import se.miun.distsys.messages.JoinMessage;
import se.miun.distsys.messages.LeaveMessage;
import se.miun.distsys.messages.ListMessage;
import se.miun.distsys.messages.Message;
import se.miun.distsys.messages.MessageSerializer;

public class GroupCommuncation {

	// members
	private int datagramSocketPort = 25000;
	private int id = 0;
	DatagramSocket datagramSocket = null;	
	boolean runGroupCommuncation = true;	

	// serializer
	MessageSerializer messageSerializer = new MessageSerializer();
	
	// listeners
	ChatMessageListener chatMessageListener = null;
	LeaveMessageListener leaveMessageListener = null;
	JoinMessageListener joinMessageListener = null;
	ListMessageListener listMessageListener = null;

	public GroupCommuncation() {
		id = new Random().nextInt(5000);
		try {
			runGroupCommuncation = true;				
			datagramSocket = new MulticastSocket(datagramSocketPort);		
			RecieveThread rt = new RecieveThread();
			rt.start();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * get the id of the group communication service.
	 * @return the id of the group communication service.
	 */
	public int getId() {
		return id;
	}

	public void shutdown() {
		runGroupCommuncation = false;		
	}
	
	class RecieveThread extends Thread {
		@Override
		public void run() {
			byte[] buffer = new byte[65536];		
			DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
			
			while(runGroupCommuncation) {
				try {
					datagramSocket.receive(datagramPacket);										
					byte[] packetData = datagramPacket.getData();					
					Message recievedMessage = messageSerializer.deserializeMessage(packetData);					
					handleMessage(recievedMessage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		private void handleMessage(Message message) {
			if(message instanceof ChatMessage) {				
				ChatMessage chatMessage = (ChatMessage) message;				
				if(chatMessageListener != null) {
					chatMessageListener.onIncomingChatMessage(chatMessage);
				}
			} 
			else if (message instanceof JoinMessage) {
				JoinMessage joinMessage = (JoinMessage) message;				
				if(joinMessageListener != null) {
					joinMessageListener.onIncomingJoinMessage(joinMessage);
				}
			}
			else if (message instanceof LeaveMessage) {
				LeaveMessage leaveMessage = (LeaveMessage) message;				
				if(leaveMessageListener != null) {
					leaveMessageListener.onIncomingLeaveMessage(leaveMessage);
				}
			}
			else if (message instanceof ListMessage) {
				ListMessage listMessage = (ListMessage) message;				
				if(listMessageListener != null) {
					listMessageListener.onIncomingListMessage(listMessage);
				}
			}
			else {				
				System.out.println("Unknown message type");
			}			
		}		
	}	
	
	public void sendChatMessage(String chat) {
		try {
			ChatMessage chatMessage = new ChatMessage(id, chat);
			byte[] sendData = messageSerializer.serializeMessage(chatMessage);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), datagramSocketPort);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendJoinMessage() {
		try {
			JoinMessage joinMessage = new JoinMessage(id);
			byte[] sendData = messageSerializer.serializeMessage(joinMessage);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), datagramSocketPort);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendLeaveMessage() {
		try {
			LeaveMessage leaveMessage = new LeaveMessage(id);
			byte[] sendData = messageSerializer.serializeMessage(leaveMessage);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), datagramSocketPort);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendListMessage(Set<Integer> clientList) {
		try {
			ListMessage listMessage = new ListMessage(clientList);
			byte[] sendData = messageSerializer.serializeMessage(listMessage);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), datagramSocketPort);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setChatMessageListener(ChatMessageListener listener) {
		this.chatMessageListener = listener;		
	}

	public void setJoinMessageListener(JoinMessageListener listener) {
		this.joinMessageListener = listener;		
	}

	public void setLeaveMessageListener(LeaveMessageListener listener) {
		this.leaveMessageListener = listener;		
	}
	public void setListMessageListener(ListMessageListener listener) {
		this.listMessageListener = listener;		
	}
	
}
