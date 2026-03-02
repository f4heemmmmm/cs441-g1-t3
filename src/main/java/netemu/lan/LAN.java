package netemu.lan;

import netemu.common.Log;
import netemu.common.Ansi;
import netemu.common.EthernetFrame;

import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class LAN {

    private final Log log;
    private final int lanID;
    private final int portNumber;
    private final ConcurrentHashMap<String, InetSocketAddress> endpoints = new ConcurrentHashMap<>();

    public LAN(int lanID, int portNumber) {
        this.lanID = lanID;
        this.portNumber = portNumber;
        this.log = new Log("LAN" + lanID, Ansi.CYAN);
    }

    public void start() throws IOException {
        DatagramSocket socket = new DatagramSocket(portNumber);
        Ansi.banner("LAN" + lanID + " Emulator (port " + portNumber + ")", Ansi.CYAN);
        log.info("Listening on port " + portNumber);

        byte[] buffer = new byte[1024];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

            String message = new String(data);
            InetSocketAddress sender = new InetSocketAddress(packet.getAddress(), packet.getPort());

            if (message.startsWith("REGISTER ")) {
                handleRegister(message, packet.getAddress(), sender);
            } else {
                handleFrame(data, sender, socket);
            }
        }
    }

    private void handleRegister(String message, InetAddress address, InetSocketAddress senderAddress) {
        String[] parts = message.trim().split("\\s+");
        if (parts.length >= 3) {
            String mac = parts[1];
            int devicePortNumber = Integer.parseInt(parts[2]);
            InetSocketAddress endpoint = new InetSocketAddress(InetAddress.getLoopbackAddress(), devicePortNumber);
            endpoints.put(mac, endpoint);
            log.info("Device " + mac + " joined (port " + devicePortNumber + ")");
        }
    }

    private void handleFrame(byte[] data, InetSocketAddress senderAddress, DatagramSocket socket) {
        try {
            EthernetFrame frame = EthernetFrame.decode(data);
            log.rx("Received " + frame);

            String srcMac = frame.sourceMACAddress().value();
            for (var entry : endpoints.entrySet()) {
                if (!entry.getKey().equals(srcMac)) {
                    InetSocketAddress target = entry.getValue();
                    DatagramPacket out = new DatagramPacket(data, data.length, target);
                    socket.send(out);
                    log.info("  -> forwarded to " + entry.getKey() + " (port " + target.getPort() + ")");
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast frame: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: LanEmulator <lanId> <port>");
            System.exit(1);
        }
        int lanId = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        new LAN(lanId, port).start();
    }
}
