package com.networkemulator.node;


import com.networkemulator.ethernet.EthernetFrame;
import com.networkemulator.ip.IPPacket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Node extends Thread{
    private final short mac;
    private final byte ip;
    private final int lanPort;

    public Node(short mac , byte ip , int lanPort){
        this.mac = mac;
        this.ip = ip;
        this.lanPort = lanPort;
    }

    public void sendMessage(byte destIP , short destMac , String message) throws Exception {
        byte[] ipPacket = IPPacket.buildPacket(
                ip,
                destIP,
                (byte)0x01, // protocol 0x01 = normal data
                message.getBytes()
        );

        byte[] ethernetFrame = EthernetFrame.buildFrame(
                mac,
                destMac,
                ipPacket
        );

        DatagramSocket socket = new DatagramSocket();

        DatagramPacket packet = new DatagramPacket(
                ethernetFrame,
                ethernetFrame.length,
                InetAddress.getByName("127.0.0.1"),
                lanPort
        );

        socket.send(packet);
        socket.close();
    }

    @Override
    public void run() {
        try {
            DatagramSocket socket = new DatagramSocket(lanPort);

            while (true) {
                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer , buffer.length);
                socket.receive(packet);

                byte[] frame = packet.getData();

                short destMac = EthernetFrame.getDestination(frame);

                if(destMac != mac) continue;

                byte[] ipPacket = EthernetFrame.getData(frame);

                byte destIP = IPPacket.getDestination(ipPacket);

                if (destIP != ip) continue;

                byte[] data = IPPacket.getData(ipPacket);

                System.out.println("Node " + ip + " received: " + new String(data) );

            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
