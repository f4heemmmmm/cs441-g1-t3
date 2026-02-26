package com.networkemulator.ip;


import java.nio.ByteBuffer;

public class IPPacket{
    public static byte[] buildPacket(byte src , byte dest , byte protocol , byte[] data){
        ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);

        buffer.put(src);                // 1 byte
        buffer.put(dest);               // 1 byte
        buffer.put(protocol);           // 1 byte
        buffer.put((byte) data.length); // 1 byte
        buffer.put(data);               // payload

        return buffer.array();
    }

    public static byte getSource(byte[] packet){
        return packet[0];
    }

    public static byte getDestination(byte[] packet){
        return packet[1];
    }

    public static byte getProtocol(byte[] packet){
        return packet[2];
    }

    public static byte[] getData(byte[] packet){
        int length = packet[3];
        byte[] data = new byte[length];

        System.arraycopy(packet , 4 , data , 0 , length);

        return data;
    }

}
