package se.miun.distsys;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ConnectException;
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

    private int id = 0;
    private int coordinator = 0;
    private boolean TCPAlive = false;
    private int coordinatorPort = 25001;
    private ServerSocket coordinatorServer;
    private int datagramSocketPort = 25000;
    private boolean electionState = false;
    private DatagramSocket datagramSocket = null;
    private boolean runGroupCommuncation = true;

    private ClientList clientList = null;

    // TCP server value
    private int serverSequence = 0;

    // serializer
    MessageSerializer messageSerializer = new MessageSerializer();

    // listeners
    OKMessageListener okMessageListener = null;
    ChatMessageListener chatMessageListener = null;
    JoinMessageListener joinMessageListener = null;
    LeaveMessageListener leaveMessageListener = null;
    ElectionMessageListener electionMessageListener = null;

    private Map<Integer, ChatMessage> chatMap = null;
    private Map<Integer, Set<Integer>> seqMap = null;

    public GroupCommuncation() {
        // generate random id
        id = new Random().nextInt(123000);
        // construct the clientlist
        clientList = new ClientList();

        chatMap = new HashMap<Integer, ChatMessage>();
        seqMap = new HashMap<Integer, Set<Integer>>();

        // start the communication
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
     *
     * @return the id of the group communication service.
     */
    public int getId() {
        return id;
    }

    public void shutdown() {
        runGroupCommuncation = false;
        TCPAlive = false;
    }

    class SequenceCheckThread extends Thread {

        private int sequenceNumber = 0;
        private Socket client = null;

        SequenceCheckThread(Socket client, int sequenceNumber) {
            this.client = client;
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public void run() {
            while (true) {
                if (!TCPAlive) {
                    return;
                } else if (!seqMap.get(sequenceNumber).isEmpty()) {
                    continue;
                } else {
                    break;
                }
            }
            sendSequenceCheckResponseMessage(sequenceNumber);
            seqMap.remove(sequenceNumber);
        }
    }

    class TCPAcceptThread extends Thread {
        @Override
        public void run() {
            while (TCPAlive) {
                Socket client = null;
                InputStream in = null;
                Message recievedMessage = null;

                try {
                    // Accept a TCP connection
                    client = coordinatorServer.accept();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (client != null) {
                    try {
                        // Get the accepted clients inputstream
                        in = client.getInputStream();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Create a buffer
                    byte[] buffer = new byte[65536];

                    try {
                        // Read all bytes into the buffer
                        in.read(buffer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        // Deserialize message
                        recievedMessage = messageSerializer.deserializeMessage(buffer);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // print messagetype
                    System.out.println("Receive on TCP: " + recievedMessage.getClass());

                    // Send message to handler
                    HandleTCPMessage(recievedMessage, client);
                }
            }
        }
    }

    public void HandleTCPMessage(Message message, Socket client) {
        if (message instanceof SequenceCheckMessage) {
            SequenceCheckMessage sequenceCheckMessage = (SequenceCheckMessage) message;
            seqMap.get(sequenceCheckMessage.sequenceNumber).remove(sequenceCheckMessage.id); // Remove id from seqMap if the client responds with sequenceCheckMessage
        } else if (message instanceof SequenceRequestMessage) {
            seqMap.put(serverSequence, new HashSet<Integer>(clientList.getClientList())); // Add a new sequence to the seqMap with a filled clientlist

            SequenceCheckThread sequenceCheckThread = new SequenceCheckThread(client, serverSequence); // Create thread for handling this sequence
            sequenceCheckThread.start(); // Start the thread

            SendTCPMessage(client, new SequenceMessage(serverSequence)); // Send the current sequence number.
            serverSequence += 1; // Increment the sequence number
        } else {
            System.out.println("Unknown message type");
        }
    }

    public void SendTCPMessage(Socket socket, Message message) {
        System.out.println("Sent on TCP: " + message.getClass());
        try {
            // open output stream
            OutputStream out = socket.getOutputStream();
            // write serialized message on output stream
            out.write(messageSerializer.serializeMessage(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class RecieveThread extends Thread {

        @Override
        public void run() {
            byte[] buffer = new byte[65536];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            while (runGroupCommuncation) {
                try {
                    datagramSocket.receive(datagramPacket);
                    byte[] packetData = datagramPacket.getData();
                    Message recievedMessage = messageSerializer.deserializeMessage(packetData);
                    System.out.println("Receive on UDP: " + recievedMessage.getClass());
                    handleMessage(recievedMessage);
                } catch (SocketTimeoutException sTimeoutException) { // Handle timeout from socket
                    sendCoordinatorMessage(); // Send the coordinator message where it tells everyone he has won.
                    try {
                        // Reset timeout to infinite
                        datagramSocket.setSoTimeout(0);
                    } catch (Exception ie) {
                        ie.printStackTrace();
                    }

               

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleMessage(Message message) {
            // Put the chatmessage into the chatmap on recieve. Run checkSequenceNumber on the chatmessages sequencenumber.
            if (message instanceof ChatMessage) {
                ChatMessage chatMessage = (ChatMessage) message;
                chatMap.put(chatMessage.sequence, chatMessage);
                checkSequenceNumber(chatMessage.sequence);
            } // Add the client to the clientlist and send list to other clients. Also raise event to the listener.
            else if (message instanceof JoinMessage) {
                JoinMessage joinMessage = (JoinMessage) message;
                if (joinMessageListener != null) {
                    joinMessageListener.onIncomingJoinMessage(joinMessage);
                    clientList.add(joinMessage.id);
                    if (joinMessage.id != id) {
                        sendListMessage(clientList.getClientList());
                    }
                }
            } // Remove the leaving client from the clientlist. Also raise event to the listener.
            else if (message instanceof LeaveMessage) {
                LeaveMessage leaveMessage = (LeaveMessage) message;
                if (leaveMessageListener != null) {
                    clientList.remove(leaveMessage.id);
                    leaveMessageListener.onIncomingLeaveMessage(leaveMessage);
                }
            } // Set the clientlist to the recieved list. And then print the current list.
            else if (message instanceof ListMessage) {
                ListMessage listMessage = (ListMessage) message;
                if (listMessage.id != id) {
                    clientList.setClientList(listMessage.clientList);
                    clientList.printList();
                }
            } // Check if own election message and not in electionstate. If true send electionmessage to all other clients.
            // If recieved id is less than your id send OKMessage
            else if (message instanceof ElectionMessage) {
                ElectionMessage electionMessage = (ElectionMessage) message;
                if (electionMessage.id != id && !electionState) {
                    sendElectionMessage();
                }
                if (electionMessage.id < id) {
                    sendOKMessage(electionMessage.id);
                }
            } // Set electionstate if it is message meant for you.
            // Also reset timeout
            else if (message instanceof OKMessage) {
                OKMessage okMessage = (OKMessage) message;
                if (okMessage.id == id) {
                    electionState = true;
                    try {
                        datagramSocket.setSoTimeout(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } // Set the coordinator on client and close the TCP server if it was open.
            else if (message instanceof CoordinatorMessage) {
                CoordinatorMessage coordinatorMessage = (CoordinatorMessage) message;
                System.out.println("The coordinator is: " + coordinatorMessage.id);
                coordinator = coordinatorMessage.id;
                electionState = false;

                if (coordinatorMessage.id != id) {
                    TCPAlive = false;
                    try {
                        coordinatorServer.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                      if (coordinatorServer == null || coordinatorServer.isClosed()) {
                        // Start the server
                        try {
                            coordinatorServer = new ServerSocket(coordinatorPort);
                        } catch (BindException bException) {
                            while (coordinatorServer == null || !coordinatorServer.isBound()) {
                                try {
                                    coordinatorServer = new ServerSocket(coordinatorPort);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // Set the TCPAlive flag to true.
                    TCPAlive = true;
                    // Start the TCP accept thread
                    TCPAcceptThread rt = new TCPAcceptThread();
                    rt.start();
                }

            } // Raise chatmessage event with the chatmessage from the chatmap which has the checked sequence number if sequenceCheckResponseMessage is received.
            else if (message instanceof SequenceCheckResponseMessage) {
                SequenceCheckResponseMessage sequenceCheckResponseMessage = (SequenceCheckResponseMessage) message;
                if (chatMessageListener != null) {
                    chatMessageListener.onIncomingChatMessage(chatMap.get(sequenceCheckResponseMessage.sequenceNumber));
                }
            } else {
                System.out.println("Unknown message type");
            }
        }
    }

    // Connect to the TCP server.
    public Socket connectToServer() {
        Socket server = null;
        try {
            server = new Socket("localhost", coordinatorPort);
        }
        catch (ConnectException e) {
            clientList.remove(coordinator);
            sendListMessage(clientList.getClientList());
            sendElectionMessage();
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (server != null && server.isConnected()) {
                System.out.println("Server connected: " + server.getInetAddress());
                return server;
            }
        }
        return null;
    }

    // Send TCP message to get the sequence number.
    public int getSequenceNumber() {
        // Connect to the TCP server.
        Socket server = connectToServer();
        InputStream in = null;
        Message recievedMessage = null;
        byte[] buffer = new byte[65536];

        if (server != null) {
            try {
                // Send SEQUENCE REQUEST message
                SendTCPMessage(server, new SequenceRequestMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
    
            try {
                in = server.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }
    
            try {
                // Open read stream
                in.read(buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
    
            try {
                // Deserialize message
                recievedMessage = messageSerializer.deserializeMessage(buffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
    
            // Print the recieved sequence number
            System.out.println("Receive: " + ((SequenceMessage) recievedMessage).sequenceNumber);
    
            try {
                // Close the connection
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Return the sequence number
            return ((SequenceMessage) recievedMessage).sequenceNumber;
        }
        else {
            return -1;
        }
    }

    public void checkSequenceNumber(int sequence) {
        Socket server = connectToServer();
        if (server != null) {
            try {
                // Send SEQUENCE CHECK message
                SendTCPMessage(server, new SequenceCheckMessage(id, sequence));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                // Close the connection
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // SendMessage class that serializes message and creates a datagram packet which then is sent on the multicast socket.
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

    // Send SequenceCheckResponseMessage
    public void sendSequenceCheckResponseMessage(int sequence) {
        sendMessage(new SequenceCheckResponseMessage(sequence));
    }

    // Send ChatMessage
    public void sendChatMessage(String chat) {
        int sequence = getSequenceNumber();
        if (sequence != -1) {
            sendMessage(new ChatMessage(id, sequence, chat));
        }
        else {
            System.out.println("Something went wrong while sending the chat message!");
        }
    }

    // Send JoinMessage
    public void sendJoinMessage() {
        sendMessage(new JoinMessage(id));
    }

    // SendJoinMessage
    public void sendLeaveMessage() {
        sendMessage(new LeaveMessage(id));
    }

    // Send JoinMessage
    public void sendListMessage(Set<Integer> clientList) {
        sendMessage(new ListMessage(id, clientList));
    }

    // Send ElectionMessage and set timeout to 3000.
    public void sendElectionMessage() {
        sendMessage(new ElectionMessage(id));
        try {
            datagramSocket.setSoTimeout(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Send OKMessage

    public void sendOKMessage(int id) {
        sendMessage(new OKMessage(id));
    }
    // Send CoordinatorMessage

    public void sendCoordinatorMessage() {
        sendMessage(new CoordinatorMessage(id));
    }

    // Set the needed listeners.
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
}
