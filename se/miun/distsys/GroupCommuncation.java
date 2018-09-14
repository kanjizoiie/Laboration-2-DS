package se.miun.distsys;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import se.miun.distsys.listeners.ChatMessageListener;
import se.miun.distsys.listeners.JoinMessageListener;
import se.miun.distsys.listeners.LeaveMessageListener;

import se.miun.distsys.messages.ChatMessage;
import se.miun.distsys.messages.JoinMessage;
import se.miun.distsys.messages.LeaveMessage;

import se.miun.distsys.messages.Message;
import se.miun.distsys.messages.MessageSerializer;

public class GroupCommuncation {
	private int datagramSocketPort = 1885; //You need to change this!		
	DatagramSocket datagramSocket = null;	
	boolean runGroupCommuncation = true;	
	MessageSerializer messageSerializer = new MessageSerializer();
	
	//Listeners
	ChatMessageListener chatMessageListener = null;
	LeaveMessageListener leaveMessageListener = null;
	JoinMessageListener joinMessageListener = null;

	public GroupCommuncation() {

		try {
			runGroupCommuncation = true;				
			datagramSocket = new MulticastSocket(datagramSocketPort);		
			RecieveThread rt = new RecieveThread();
			rt.start();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
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
				if(chatMessageListener != null){
					chatMessageListener.onIncomingChatMessage(chatMessage);
				}
			} 
			else if (message instanceof JoinMessage) {
				JoinMessage joinMessage = (JoinMessage) message;				
				if(joinMessageListener != null){
					joinMessageListener.onIncomingJoinMessage(joinMessage);
				}
			}
			else if (message instanceof LeaveMessage) {
				LeaveMessage leaveMessage = (LeaveMessage) message;				
				if(leaveMessageListener != null){
					leaveMessageListener.onIncomingLeaveMessage(leaveMessage);
				}
			}
			else {				
				System.out.println("Unknown message type");
			}			
		}		
	}	
	
	public void sendChatMessage(int id, String chat) {
		try {
			ChatMessage chatMessage = new ChatMessage(id, chat);
			byte[] sendData = messageSerializer.serializeMessage(chatMessage);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), datagramSocketPort);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendJoinMessage(int id) {
		try {
			JoinMessage joinMessage = new JoinMessage(id);
			byte[] sendData = messageSerializer.serializeMessage(joinMessage);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), datagramSocketPort);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendLeaveMessage(int id) {
		try {
			LeaveMessage leaveMessage = new LeaveMessage(id);
			byte[] sendData = messageSerializer.serializeMessage(leaveMessage);
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
	
}
