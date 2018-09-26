package se.miun.distsys.listeners;

import se.miun.distsys.messages.CoordinatorMessage;


public interface CoordinatorMessageListener {
    public void onIncomingCoordinatorMessage(CoordinatorMessage coordinnatorMessage);
}
