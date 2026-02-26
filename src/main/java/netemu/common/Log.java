package netemu.common;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import netemu.dashboard.EventReporter;

import static netemu.common.Ansi.*;

/**
 * Timestamped, colorized logging utility.
 */
public final class Log {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final String tag;

    public Log(String tag) {
        this.tag = tag;
    }

    public void info(String msg) {
        String ts = DIM + timestamp() + RESET;
        String tg = bold(CYAN, "[" + tag + "]");
        String colorMsg = colorizeInfo(msg);
        System.out.printf("%s %s %s%n", ts, tg, colorMsg);
        EventReporter.report(classifyInfo(msg), msg);
    }

    public void warn(String msg) {
        String ts = DIM + timestamp() + RESET;
        String tg = bold(CYAN, "[" + tag + "]");
        String lvl = bold(YELLOW, "[WARN]");
        System.out.printf("%s %s %s %s%n", ts, tg, lvl, YELLOW + msg + RESET);
        EventReporter.report("WARN", msg);
    }

    public void ids(String msg) {
        String ts = DIM + timestamp() + RESET;
        String tg = bold(CYAN, "[" + tag + "]");
        String lvl = bold(RED, "[IDS]");
        System.out.printf("%s %s %s %s%n", ts, tg, lvl, BRIGHT_RED + msg + RESET);
        EventReporter.report("IDS_ALERT", msg);
    }

    public void firewall(String msg) {
        String ts = DIM + timestamp() + RESET;
        String tg = bold(CYAN, "[" + tag + "]");
        String lvl = bold(MAGENTA, "[FIREWALL]");
        System.out.printf("%s %s %s %s%n", ts, tg, lvl, MAGENTA + msg + RESET);
        EventReporter.report("FIREWALL", msg);
    }

    public void sniff(String msg) {
        String ts = DIM + timestamp() + RESET;
        String tg = bold(CYAN, "[" + tag + "]");
        String lvl = bold(BLUE, "[SNIFF]");
        System.out.printf("%s %s %s %s%n", ts, tg, lvl, BLUE + msg + RESET);
        EventReporter.report("SNIFF", msg);
    }

    /**
     * Colorize info messages based on their content.
     */
    private static String colorizeInfo(String msg) {
        if (msg.contains("Sent frame") || msg.contains("Sent Ping") || msg.contains("Sent raw"))
            return GREEN + msg + RESET;
        if (msg.contains("Received frame") || msg.contains("Received Ping") || msg.contains("Accepted packet"))
            return CYAN + msg + RESET;
        if (msg.contains("Dropped frame") || msg.contains("dropped by IDS"))
            return RED + msg + RESET;
        if (msg.contains("Forwarding:"))
            return YELLOW + msg + RESET;
        if (msg.contains("Registered"))
            return BRIGHT_GREEN + msg + RESET;
        if (msg.contains("Spoofing ON") || msg.contains("Sniffing ON"))
            return BRIGHT_RED + msg + RESET;
        if (msg.contains("Spoofing OFF") || msg.contains("Sniffing OFF"))
            return GREEN + msg + RESET;
        if (msg.contains("Ping reply") || msg.contains("RTT="))
            return BRIGHT_GREEN + msg + RESET;
        if (msg.contains("Ping REQUEST") || msg.contains("Ping flood"))
            return BRIGHT_YELLOW + msg + RESET;
        if (msg.contains("Firewall rule"))
            return MAGENTA + msg + RESET;
        if (msg.contains("IDS"))
            return YELLOW + msg + RESET;
        if (msg.contains("started") || msg.contains("stopped"))
            return BOLD + WHITE + msg + RESET;
        return msg;
    }

    /**
     * Classify an info message into a dashboard event type by inspecting keywords.
     */
    private static String classifyInfo(String msg) {
        if (msg.contains("Sent frame"))      return "FRAME_SENT";
        if (msg.contains("Received frame"))  return "FRAME_RECV";
        if (msg.contains("Dropped frame"))   return "FRAME_DROP";
        if (msg.contains("Forwarding:"))     return "FORWARD";
        if (msg.contains("Registered"))      return "REGISTER";
        if (msg.contains("Spoofing ON"))     return "SPOOF";
        if (msg.contains("Sniffing ON"))     return "SNIFF";
        if (msg.contains("Ping") && msg.contains("REQUEST")) return "PING_SENT";
        if (msg.contains("Ping") && msg.contains("reply"))   return "PING_RECV";
        if (msg.contains("dropped by IDS"))  return "IDS_BLOCK";
        return "INFO";
    }

    private static String timestamp() {
        return LocalTime.now().format(FMT);
    }
}
