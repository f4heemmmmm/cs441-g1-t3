package com.networkemulator.router;

import com.networkemulator.ethernet.EthernetFrame;
import com.networkemulator.ip.IPPacket;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Router extends Thread{
    private int lan1Port;
    private int lan2Port;
    private int lan3Port;

    public Router(int lan1Port , int lan2Port , int lan3Port){
        this.lan1Port = lan1Port;
        this.lan2Port = lan2Port;
        this.lan3Port = lan3Port;
    }

    @Override
    public void run(){
        new Thread(() -> listen(lan1Port)).start();
        new Thread(() -> listen(lan2Port)).start();
        new Thread(() -> listen(lan3Port)).start();
    }

    private void listen(int port){
        try {
            DatagramSocket socket = new DatagramSocket(port);

            while (true) {
                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer , buffer.length);
                socket.receive(packet);

                byte[] frame = packet.getData();

                byte[] ipPacket = EthernetFrame.getData(frame);

                byte destIP = IPPacket.getDestination(ipPacket);

                int targetPort = getTargetPort(destIP);

                DatagramPacket forwardPacket = new DatagramPacket(
                        frame,
                        frame.length,
                        InetAddress.getByName("127.0.0.1"),
                        targetPort
                );

                socket.send(forwardPacket);

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private int getTargetPort(byte destIP){
        if(destIP == 0x12 || destIP == 0x13)
            return lan1Port;

        if (destIP == 0x22)
            return lan2Port;

        return lan3Port;
    }
}
