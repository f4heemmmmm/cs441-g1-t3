package com.networkemulator;


import com.networkemulator.node.Node;
import com.networkemulator.router.Router;

public class Main {
    public static void main(String[] args) throws Exception{

        int LAN1 = 5001;
        int LAN2 = 5002;
        int LAN3 = 5003;

        Router router = new Router(LAN1 , LAN2 , LAN3);
        router.start();

        Node node1 = new Node((short) 0x0001 , (byte)0x12 , LAN1);
        Node node2 = new Node((short) 0x0002 , (byte)0x13 , LAN1);
        Node node3 = new Node((short) 0x0003 , (byte)0x22 , LAN2);
        Node node4 = new Node((short) 0x0004 , (byte)0x32 , LAN3);

        node1.start();
        node2.start();
        node3.start();
        node4.start();

        Thread.sleep(2000);

        node1.sendMessage((byte) 0x22 , (short)0x0003 ,
                "Hello from Node1 to Node3");

    }
}
