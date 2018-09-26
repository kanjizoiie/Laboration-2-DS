package se.miun.distsys.listeners;

import se.miun.distsys.messages.OKMessage;


public interface OKMessageListener {
    public void onIncomingOKMessage(OKMessage okMessage);
}
