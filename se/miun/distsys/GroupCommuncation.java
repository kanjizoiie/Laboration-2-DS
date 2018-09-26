package se.miun.distsys;

import java.util.Random;
import java.util.Set;
import java.io.Console;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import se.miun.distsys.listeners.ChatMessageListener;
import se.miun.distsys.listeners.ElectionMessageListener;
import se.miun.distsys.listeners.JoinMessageListener;
import se.miun.distsys.listeners.LeaveMessageListener;
import se.miun.distsys.listeners.ListMessageListener;
import se.miun.distsys.listeners.OKMessageListener;

import se.miun.distsys.messages.Message;
import se.miun.distsys.messages.MessageSerializer;
import se.miun.distsys.messages.ChatMessage;
import se.miun.distsys.messages.CoordinatorMessage;
import se.miun.distsys.messages.JoinMessage;
import se.miun.distsys.messages.LeaveMessage;
import se.miun.distsys.messages.ListMessage;
import se.miun.distsys.messages.ElectionMessage;
import se.miun.distsys.messages.OKMessage;


public class GroupCommuncation {

	// members
	private int datagramSocketPort = 25000;
	private int id = 0;
	private boolean election = false;

	private DatagramSocket datagramSocket = null;	
	private boolean runGroupCommuncation = true;	

	// serializer
	MessageSerializer messageSerializer = new MessageSerializer();
	
	// listeners
	ChatMessageListener chatMessageListener = null;
	LeaveMessageListener leaveMessageListener = null;
	JoinMessageListener joinMessageListener = null;
	ListMessageListener listMessageListener = null;
	ElectionMessageListener electionMessageListener = null;
	OKMessageListener okMessageListener = null;

	public GroupCommuncation() {
		id = new Random().nextInt(5000000);
		try {
			runGroupCommuncation = true;				
			datagramSocket = new MulticastSocket(datagramSocketPort);		
			RecieveThread rt = new RecieveThread();
			rt.start();
			
		}
		catch (Exception e) {
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
					System.out.println(recievedMessage.getClass());				
					handleMessage(recievedMessage);
					datagramSocket.setSoTimeout(0);
				}
				catch (SocketTimeoutException e) {
					System.out.println("I am the leader");
					sendCoordinatorMessage();
				} 
				catch (Exception e) {
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
			else if (message instanceof ElectionMessage) {
				ElectionMessage electionMessage = (ElectionMessage) message;
				if (electionMessage.id > id) {
					sendOKMessage();
					sendElectionMessage();
				}
			}
			else if (message instanceof OKMessage) {
				try {
					datagramSocket.setSoTimeout(0);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {				
				System.out.println("Unknown message type");
			}			
		}		
	}	
	
	public void sendMessage(Message message) {
		try {
			byte[] sendData = messageSerializer.serializeMessage(message);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), datagramSocketPort);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendChatMessage(String chat) {
		sendMessage(new ChatMessage(id, chat));
	}

	public void sendJoinMessage() {
		sendMessage(new JoinMessage(id));
	}

	public void sendLeaveMessage() {
		sendMessage(new LeaveMessage(id));
	}

	public void sendListMessage(Set<Integer> clientList) {
		sendMessage(new ListMessage(clientList));
	}

	public void sendElectionMessage() {
		sendMessage(new ElectionMessage(id));
		election = true;
		datagramSocket.setSoTimeout(4000);
	}

	public void sendOKMessage() {
		sendMessage(new OKMessage(id));
	}

	public void sendCoordinatorMessage() {
		sendMessage(new CoordinatorMessage(id));
	}

	public void setElectionMessageListener(ElectionMessageListener listener) {
		this.electionMessageListener = listener;
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
