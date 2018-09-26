package se.miun.distsys.listeners;

import se.miun.distsys.messages.ElectionMessage;


public interface ElectionMessageListener {
    public void onIncomingElectionMessage(ElectionMessage electionMessage);
}
