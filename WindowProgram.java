import se.miun.distsys.GroupCommuncation;
import se.miun.distsys.listeners.ChatMessageListener;
import se.miun.distsys.listeners.JoinMessageListener;
import se.miun.distsys.listeners.LeaveMessageListener;
import se.miun.distsys.messages.ChatMessage;
import se.miun.distsys.messages.JoinMessage;
import se.miun.distsys.messages.LeaveMessage;

import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;



import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import javax.swing.JList;
public class WindowProgram implements ActionListener, JoinMessageListener, LeaveMessageListener, ChatMessageListener {

	// the main function, this is what is ran on startup.
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WindowProgram window = new WindowProgram();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	int id = 0;

	JFrame frame;
	JTextPane txtpnChat = new JTextPane();
	JTextPane txtpnMessage = new JTextPane();
	JScrollPane scrollPane = new JScrollPane();
	JButton btnSendChatMessage = new JButton();

	GroupCommuncation gc = null;	

	public WindowProgram() {
		initializeFrame();
		gc = new GroupCommuncation();	
		gc.setChatMessageListener(this);
		gc.setJoinMessageListener(this);
		gc.setLeaveMessageListener(this);
		frame.setTitle(Integer.toString(gc.getId()));

		System.out.println("Group Communcation Started");

		// send a join message to all other clients.
		gc.sendJoinMessage();
	}

	private void initializeFrame() {
		// create frame for the window
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		frame.getContentPane().add(scrollPane);


		scrollPane.setViewportView(txtpnChat);

		txtpnChat.setEditable(false);	
		txtpnChat.setText("--== Group Chat ==--");

		frame.getContentPane().add(txtpnMessage);
		
		btnSendChatMessage.setText("Send Message");
		btnSendChatMessage.addActionListener(this);
		btnSendChatMessage.setActionCommand("send");
		
		frame.getContentPane().add(btnSendChatMessage);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
	        public void windowClosing(WindowEvent winEvt) {
				gc.sendLeaveMessage();
	            gc.shutdown();
	        }
	    });
	}

	// button event function
	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equalsIgnoreCase("send")) {
			if (txtpnMessage.getText().length() > 0) {
				gc.sendChatMessage(txtpnMessage.getText());
				txtpnMessage.setText("");
			}
		}		
	}
	
	// on incoming chat message event function
	@Override
	public void onIncomingChatMessage(ChatMessage chatMessage) {	
		txtpnChat.setText(chatMessage.id + ": " + chatMessage.chat + "\n" + txtpnChat.getText());				
	}
	
	// on incoming join message event function
	@Override
	public void onIncomingJoinMessage(JoinMessage joinMessage) {
		if (joinMessage.id != id) {
			txtpnChat.setText(joinMessage.id + " joined." + "\n" + txtpnChat.getText());	
		}	
	}

	// on incoming leave message event function
	@Override
	public void onIncomingLeaveMessage(LeaveMessage leaveMessage) {
		if (leaveMessage.id != id) {
			txtpnChat.setText(leaveMessage.id + " left." + "\n" + txtpnChat.getText());	
		}	
	}
}
