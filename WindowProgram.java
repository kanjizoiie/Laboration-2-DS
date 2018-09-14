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
public class WindowProgram implements ActionListener, JoinMessageListener, LeaveMessageListener, ChatMessageListener {

	JFrame frame;
	JTextPane txtpnChat = new JTextPane();
	JTextPane txtpnMessage = new JTextPane();
	
	GroupCommuncation gc = null;	

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

	public WindowProgram() {
		initializeFrame();
		gc = new GroupCommuncation();	

		gc.setChatMessageListener(this);
		gc.setJoinMessageListener(this);
		gc.setLeaveMessageListener(this);

		System.out.println("Group Communcation Started");
		System.out.println("Sending Join Message");
		gc.sendJoinMessage();
	}

	private void initializeFrame() {
		// create frame for the window
		frame = new JFrame();

		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		frame.getContentPane().add(scrollPane);
		scrollPane.setViewportView(txtpnChat);
		txtpnChat.setEditable(false);	
		txtpnChat.setText("--== Group Chat ==--");
		
		txtpnMessage.setText("Message");
		frame.getContentPane().add(txtpnMessage);
		
		JButton btnSendChatMessage = new JButton("Send Chat Message");
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

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equalsIgnoreCase("send")) {
			gc.sendChatMessage(txtpnMessage.getText());
		}		
	}
	
	@Override
	public void onIncomingChatMessage(ChatMessage chatMessage) {	
		txtpnChat.setText(chatMessage.chat + "\n" + txtpnChat.getText());				
	}

	@Override
	public void onIncomingJoinMessage(JoinMessage chatMessage) {	
		txtpnChat.setText("Someone Joined" + "\n" + txtpnChat.getText());				
	}

	@Override
	public void onIncomingLeaveMessage(LeaveMessage chatMessage) {	
		txtpnChat.setText("Someone Left" + "\n" + txtpnChat.getText());				
	}
}
