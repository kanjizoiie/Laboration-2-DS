package se.miun.distsys.listeners;

import se.miun.distsys.messages.LeaveMessage;

public interface LeaveMessageListener {
    public void onIncomingLeaveMessage(LeaveMessage chatMessage);
}
