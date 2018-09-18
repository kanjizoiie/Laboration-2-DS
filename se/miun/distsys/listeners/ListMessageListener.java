package se.miun.distsys.listeners;

import se.miun.distsys.messages.ListMessage;


public interface ListMessageListener {
    public void onIncomingListMessage(ListMessage listMessage);
}
