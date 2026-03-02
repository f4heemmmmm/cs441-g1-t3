package netemu.common;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class Log {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final String prefix;
    private final String color;

    public Log(String name, String color) {
        this.prefix = "[" + name + "]";
        this.color = color;
    }

    public void info(String msg) {
        System.out.println(color + timestamp() + " " + prefix + " " + msg + Ansi.RESET);
    }

    public void warn(String msg) {
        System.out.println(Ansi.YELLOW + timestamp() + " " + prefix + " WARN: " + msg + Ansi.RESET);
    }

    public void error(String msg) {
        System.out.println(Ansi.RED + timestamp() + " " + prefix + " ERROR: " + msg + Ansi.RESET);
    }

    public void security(String msg) {
        System.out.println(Ansi.RED + Ansi.BOLD + timestamp() + " " + prefix + " SECURITY: " + msg + Ansi.RESET);
    }

    public void rx(String msg) {
        info("RX " + msg);
    }

    public void tx(String msg) {
        info("TX " + msg);
    }

    private String timestamp() {
        return LocalTime.now().format(FMT);
    }
}
