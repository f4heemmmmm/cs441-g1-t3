package netemu.dashboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import netemu.common.Ansi;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dashboard server: collects events from all emulator processes via UDP
 * and pushes them to browsers via Server-Sent Events (SSE).
 *
 * <ul>
 *   <li>UDP collector on port 9000</li>
 *   <li>HTTP server on port 8080 (serves dashboard.html at /)</li>
 *   <li>SSE endpoint at /events</li>
 * </ul>
 */
public class DashboardServer {

    private static final int UDP_PORT = 9000;
    private static final int HTTP_PORT = 8080;
    private static final int BUF_SIZE = 4096;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final List<OutputStream> sseClients = new CopyOnWriteArrayList<>();

    public void start() throws Exception {
        // Start HTTP server
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/", this::handleRoot);
        httpServer.createContext("/events", this::handleSSE);
        httpServer.start();

        // Start UDP collector
        DatagramSocket udpSocket = new DatagramSocket(UDP_PORT);
        Thread udpThread = new Thread(() -> udpCollector(udpSocket), "udp-collector");
        udpThread.setDaemon(true);
        udpThread.start();

        System.out.println(Ansi.banner(
                "Network Dashboard",
                "HTTP:  http://localhost:" + HTTP_PORT,
                "UDP collector: port " + UDP_PORT,
                "Type 'quit' to exit"
        ));
        System.out.println();

        // CLI loop
        Scanner scanner = new Scanner(System.in);
        while (running.get()) {
            System.out.print(Ansi.bold(Ansi.GREEN, "DASHBOARD> "));
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if ("quit".equalsIgnoreCase(line)) {
                running.set(false);
                break;
            }
        }

        udpSocket.close();
        httpServer.stop(0);
        System.out.println(Ansi.DIM + "[Dashboard] Stopped." + Ansi.RESET);
    }

    private void udpCollector(DatagramSocket socket) {
        byte[] buf = new byte[BUF_SIZE];
        while (running.get()) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                String json = new String(buf, 0, pkt.getLength(), StandardCharsets.UTF_8);
                broadcast(json);
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[Dashboard] UDP error: " + e.getMessage());
                }
            }
        }
    }

    private void broadcast(String json) {
        String sseData = "data: " + json + "\n\n";
        byte[] bytes = sseData.getBytes(StandardCharsets.UTF_8);
        for (OutputStream out : sseClients) {
            try {
                out.write(bytes);
                out.flush();
            } catch (IOException e) {
                sseClients.remove(out);
            }
        }
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        InputStream is = getClass().getResourceAsStream("/dashboard.html");
        if (is == null) {
            byte[] msg = "dashboard.html not found in classpath".getBytes();
            exchange.sendResponseHeaders(404, msg.length);
            exchange.getResponseBody().write(msg);
            exchange.getResponseBody().close();
            return;
        }
        byte[] html = is.readAllBytes();
        is.close();
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, html.length);
        exchange.getResponseBody().write(html);
        exchange.getResponseBody().close();
    }

    private void handleSSE(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        OutputStream out = exchange.getResponseBody();
        sseClients.add(out);

        // Send a connection confirmation
        try {
            out.write("data: {\"type\":\"CONNECTED\",\"msg\":\"Dashboard connected\"}\n\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            sseClients.remove(out);
        }

        // Keep the connection open - the SSE stream will be written to by broadcast()
        // The connection stays open until the client disconnects or server shuts down
    }

    public static void main(String[] args) throws Exception {
        new DashboardServer().start();
    }
}
