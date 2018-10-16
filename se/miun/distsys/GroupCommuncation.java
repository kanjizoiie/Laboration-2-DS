package se.miun.distsys;

import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.io.Console;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
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
import se.miun.distsys.messages.SequenceCheckMessage;
import se.miun.distsys.messages.SequenceCheckResponseMessage;
import se.miun.distsys.messages.SequenceMessage;
import se.miun.distsys.messages.SequenceRequestMessage;


public class GroupCommuncation {

	// members
	private int datagramSocketPort = 25000;
	private int id = 0;
	private int coordinator = 0;
	private boolean electionWaitForVictory = false;

	private ServerSocket coordinatorServer;
	private int coordinatorPort = 8080;

	private DatagramSocket datagramSocket = null;
	private boolean runGroupCommuncation = true;	
	private boolean TCPAlive = false;

	// TCP server value
	private int serverSequence = 0;

	// serializer
	MessageSerializer messageSerializer = new MessageSerializer();
	
	// listeners
	ChatMessageListener chatMessageListener = null;
	LeaveMessageListener leaveMessageListener = null;
	JoinMessageListener joinMessageListener = null;
	ListMessageListener listMessageListener = null;
	OKMessageListener okMessageListener = null;
	ElectionMessageListener electionMessageListener = null;
	

	public GroupCommuncation() {
		// generate random id
		id = new Random().nextInt(123000);
		// start the communication
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

	class TCPAcceptThread extends Thread {
		@Override
		public void run() {
			while(TCPAlive) {
				try {
					Socket client = null;
					if (coordinatorServer != null && !coordinatorServer.isClosed()) {
						client = coordinatorServer.accept();
					}
					InputStream in = client.getInputStream();
					byte[] buffer = new byte[65536];
					in.read(buffer);
					Message recievedMessage = messageSerializer.deserializeMessage(buffer);
					System.out.println("Receive on TCP: " + recievedMessage.getClass());	
					HandleTCPMessage(recievedMessage, client);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void HandleTCPMessage(Message message, Socket client) {
		if (message instanceof SequenceCheckMessage) {
			SequenceCheckMessage sequenceCheckMessage = (SequenceCheckMessage) message;
			SendTCPMessage(client, new SequenceCheckResponseMessage(sequenceCheckMessage.sequenceNumber == serverSequence));
		}
		else if (message instanceof SequenceRequestMessage) {
			SendTCPMessage(client, new SequenceMessage(++serverSequence));
		}
		else {				
			System.out.println("Unknown message type");
		}			
	}

	public void SendTCPMessage(Socket socket, Message message) {
		try {
			// open output stream
			OutputStream out = socket.getOutputStream();
			// write serialized message on output stream
			out.write(messageSerializer.serializeMessage(message));
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
		TCPAlive = false;	
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
					System.out.println("Receive on UDP: " + recievedMessage.getClass());				
					handleMessage(recievedMessage);
				}
				catch (SocketTimeoutException e) { // Handle timeout from socket
					sendCoordinatorMessage();
					try {
						// Reset timeout to infinite
						datagramSocket.setSoTimeout(0);
						TCPAlive = true;
						if (coordinatorServer == null || coordinatorServer.isClosed()) {
							coordinatorServer = new ServerSocket(coordinatorPort);
						}
						TCPAcceptThread rt = new TCPAcceptThread();
						rt.start();
					}
					catch (Exception ie) {
						ie.printStackTrace();
					}
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
					if (checkSequenceMessage(chatMessage.sequence)) {
						chatMessageListener.onIncomingChatMessage(chatMessage);
					}
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
				System.out.println("ID: " + id + " ELE RECV: " + electionMessage.id); //DEBUG
				if (electionMessage.id != id && !electionWaitForVictory) {
					sendElectionMessage();
				}
				if (id > electionMessage.id) {
					sendOKMessage(electionMessage.id);
				}
			}
			else if (message instanceof OKMessage) {
				OKMessage okMessage = (OKMessage) message;
				System.out.println("ID: " + id + " OKM RECV: " + okMessage.id); //DEBUG
				if (id == okMessage.id) {
					try {
						datagramSocket.setSoTimeout(0);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					electionWaitForVictory = true;
				}
			}
			else if (message instanceof CoordinatorMessage) {
				CoordinatorMessage coordinatorMessage = (CoordinatorMessage) message;
				System.out.println("The coordinator is: " + coordinatorMessage.id);
				coordinator = coordinatorMessage.id;
				if (coordinatorMessage.id != id) {
					try {
						if (coordinatorServer != null && !coordinatorServer.isClosed()) {
							TCPAlive = false;
							coordinatorServer.close();
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			else {				
				System.out.println("Unknown message type");
			}			
		}		
	}
	
	public int getSequenceNumber() {
		Socket server = null;
		try {
			server = new Socket("localhost", coordinatorPort);
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
		finally {
			if (server.isConnected()) {
				System.out.println("Server connected: " + server.getInetAddress());
			}
		}
		byte[] buffer = new byte[65536];
		try {
			// Send SEQUENCE REQUEST message
			SendTCPMessage(server, new SequenceRequestMessage());

			// Open read stream
			InputStream in = server.getInputStream();
			in.read(buffer);

			// Deserialize message
			Message recievedMessage = messageSerializer.deserializeMessage(buffer);	

			// Print the recieved sequence number
			System.out.println("Receive: " + ((SequenceMessage)recievedMessage).sequenceNumber);

			// Close the connection
			server.close();

			// Return the sequence number
			return ((SequenceMessage)recievedMessage).sequenceNumber;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		// On error return -1
		return -1;
	}

	public boolean checkSequenceMessage(int checkSequence) {
		Socket server = null;
		try {
			server = new Socket("localhost", coordinatorPort);
		}
		catch (Exception e) {
			e.printStackTrace();
		}		
		finally {
			if (server.isConnected()) {
				System.out.println("Server connected: " + server.getInetAddress());
			}
		}

		byte[] buffer = new byte[65536];

		try {
			// Send SEQUENCE CHECK message
			SendTCPMessage(server, new SequenceCheckMessage(checkSequence));

			// Open read stream
			InputStream in = server.getInputStream();
			in.read(buffer);

			// Deserialize message
			Message recievedMessage = messageSerializer.deserializeMessage(buffer);	

			// Print the recieved boolean value
			System.out.println("Receive: " + ((SequenceCheckResponseMessage)recievedMessage).run);

			// Close the connection
			server.close();

			// Return if message correct order
			return ((SequenceCheckResponseMessage)recievedMessage).run;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		// On error return false
		return false;
	}

	public void sendMessage(Message message) {
		try {
			System.out.println("Sent: " + message.getClass());		
			byte[] sendData = messageSerializer.serializeMessage(message);
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), datagramSocketPort);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendChatMessage(String chat) {
		sendMessage(new ChatMessage(id, getSequenceNumber(), chat));
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
		try {
			datagramSocket.setSoTimeout(6000);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		sendMessage(new ElectionMessage(id));
	}

	public void sendOKMessage(int id) {
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
