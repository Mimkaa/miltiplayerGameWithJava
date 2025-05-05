package ch.unibas.dmi.dbis.cs108.example.ClientServerStuff;


import java.net.InetAddress;

/**
 * A simple carrier for a Message + destination.
 */
public class OutgoingMessage {
  public final Message msg;
  public final InetAddress address;
  public final int port;

  public OutgoingMessage(Message msg, InetAddress address, int port) {
    this.msg     = msg;
    this.address = address;
    this.port    = port;
  }
}

