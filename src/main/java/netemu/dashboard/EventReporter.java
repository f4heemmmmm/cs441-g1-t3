package netemu.dashboard;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Singleton fire-and-forget UDP event sender for the dashboard.
 * Each process calls {@code EventReporter.init("NAME")} once at startup.
 * Then {@code EventReporter.report(type, msg)} sends a JSON event to the
 * dashboard collector on port 9000. If the dashboard is not running,
 * events are silently dropped.
 */
public final class EventReporter {

    private static final int DASHBOARD_PORT = 9000;
    private static volatile String processName;
    private static volatile DatagramSocket socket;

    private EventReporter() {}

    /**
     * Initialize the reporter for this process.
     * Call once in main() before any logging occurs.
     */
    public static void init(String name) {
        processName = name;
        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            // Dashboard reporting is optional; don't break the process
            socket = null;
        }
    }

    /**
     * Report an event to the dashboard. Fire-and-forget.
     *
     * @param type  event type (e.g., FRAME_SENT, IDS_ALERT)
     * @param msg   human-readable message
     */
    public static void report(String type, String msg) {
        if (processName == null || socket == null) return;
        try {
            String json = "{\"process\":\"" + escapeJson(processName)
                    + "\",\"type\":\"" + escapeJson(type)
                    + "\",\"msg\":\"" + escapeJson(msg)
                    + "\",\"ts\":" + System.currentTimeMillis() + "}";
            byte[] data = json.getBytes();
            DatagramPacket pkt = new DatagramPacket(data, data.length,
                    InetAddress.getByName("localhost"), DASHBOARD_PORT);
            socket.send(pkt);
        } catch (Exception ignored) {
            // Fire-and-forget: never disrupt the main process
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
