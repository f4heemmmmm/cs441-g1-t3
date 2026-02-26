package com.networkemulator.ethernet;


import java.nio.ByteBuffer;

public class EthernetFrame{
    public static byte[] buildFrame(short src , short dest , byte[] data){
        ByteBuffer buffer = ByteBuffer.allocate(5 + data.length);

        buffer.putShort(src);   // 2 bytes
        buffer.putShort(dest);  // 2 bytes
        buffer.put((byte)data.length);  //1 byte
        buffer.put(data);       // payload

        return buffer.array();
    }

    public static short getSource(byte[] frame){
        return ByteBuffer.wrap(frame).getShort(0);
    }

    public static short getDestination(byte[] frame){
        return ByteBuffer.wrap(frame).getShort(2);
    }

    public static byte[] getData(byte[] frame){
        int length = frame[4];
        byte[] data = new byte[length];
        System.arraycopy(frame , 5 , data , 0 , length);
        return data;
    }

}
