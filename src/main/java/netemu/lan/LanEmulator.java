package netemu.lan;

import netemu.common.Ansi;
import netemu.common.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LAN emulator: receives frames from endpoints and broadcasts (fan-out)
 * to all registered endpoints on the LAN.
 *
 * Registration message format: "REGISTER <MAC> <port>"
 * Everything else is treated as an Ethernet frame to broadcast.
 */
public class LanEmulator {

    private static final int BUF_SIZE = 1024;
    private static final String REGISTER_PREFIX = "REGISTER ";

    private final int lanId;
    private final int listenPort;
    private final Log log;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // MAC -> UDP port
    private final Map<String, Integer> registered = new ConcurrentHashMap<>();
    private DatagramSocket socket;

    public LanEmulator(int lanId, int listenPort) {
        this.lanId = lanId;
        this.listenPort = listenPort;
        this.log = new Log("LAN" + lanId);
    }

    public void start() throws SocketException {
        socket = new DatagramSocket(listenPort);
        System.out.println(Ansi.banner(
                "LAN" + lanId + " Emulator",
                "UDP port: " + listenPort,
                "Type 'list' or 'quit'"
        ));
        System.out.println();
        log.info("LAN" + lanId + " emulator started, listening on UDP port " + listenPort);

        // Receiver thread
        Thread receiver = new Thread(this::receiveLoop, "LAN" + lanId + "-recv");
        receiver.setDaemon(true);
        receiver.start();

        // CLI thread
        cliLoop();

        socket.close();
        log.info("LAN" + lanId + " emulator stopped.");
    }

    private void receiveLoop() {
        byte[] buf = new byte[BUF_SIZE];
        while (running.get()) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                byte[] data = new byte[pkt.getLength()];
                System.arraycopy(buf, 0, data, 0, pkt.getLength());

                String msg = new String(data);
                if (msg.startsWith(REGISTER_PREFIX)) {
                    handleRegistration(msg);
                } else {
                    broadcast(data, pkt.getPort());
                }
            } catch (IOException e) {
                if (running.get()) {
                    log.warn("Receive error: " + e.getMessage());
                }
            }
        }
    }

    private void handleRegistration(String msg) {
        // Format: "REGISTER <MAC> <port>"
        String[] parts = msg.trim().split("\\s+");
        if (parts.length >= 3) {
            String mac = parts[1];
            int port = Integer.parseInt(parts[2]);
            registered.put(mac, port);
            log.info("Registered endpoint: MAC=" + mac + ", UDP port=" + port);
        } else {
            log.warn("Invalid registration message: " + msg);
        }
    }

    private void broadcast(byte[] data, int senderPort) {
        try {
            InetAddress localhost = InetAddress.getByName("localhost");
            for (Map.Entry<String, Integer> entry : registered.entrySet()) {
                int port = entry.getValue();
                DatagramPacket out = new DatagramPacket(data, data.length, localhost, port);
                socket.send(out);
            }
        } catch (IOException e) {
            log.warn("Broadcast error: " + e.getMessage());
        }
    }

    private void cliLoop() {
        Scanner scanner = new Scanner(System.in);
        while (running.get()) {
            System.out.print(Ansi.bold(Ansi.GREEN, "LAN" + lanId + "> "));
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if ("quit".equalsIgnoreCase(line)) {
                running.set(false);
                socket.close();
                break;
            } else if ("list".equalsIgnoreCase(line)) {
                log.info("Registered endpoints:");
                registered.forEach((mac, port) ->
                        log.info("  MAC=" + mac + " port=" + port));
                if (registered.isEmpty()) {
                    log.info("  (none)");
                }
            } else if ("help".equalsIgnoreCase(line)) {
                System.out.println(Ansi.bold(Ansi.WHITE, "Commands:"));
                System.out.println(Ansi.helpEntry("list", "Show registered endpoints"));
                System.out.println(Ansi.helpEntry("quit", "Exit emulator"));
            } else {
                log.warn("Unknown command: " + line + "  (try: help)");
            }
        }
    }
}
