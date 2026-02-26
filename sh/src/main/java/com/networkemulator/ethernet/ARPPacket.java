package com.networkemulator.ethernet;

import java.io.Serializable;

public class ARPPacket implements Serializable {
    public String senderMac;
    public int senderIP;
    public int targetIP;
    public boolean isRequest;

    public ARPPacket(
            String senderMac,
            int senderIP,
            int targetIP,
            boolean isRequest
    ){
        this.senderMac = senderMac;
        this.senderIP = senderIP;
        this.targetIP = targetIP;
        this.isRequest = isRequest;
    }
}
