package netemu.common;

/**
 * ANSI escape codes for colorized terminal output.
 */
public final class Ansi {

    private Ansi() {}

    // Reset
    public static final String RESET   = "\u001B[0m";

    // Styles
    public static final String BOLD    = "\u001B[1m";
    public static final String DIM     = "\u001B[2m";

    // Foreground colors
    public static final String RED     = "\u001B[31m";
    public static final String GREEN   = "\u001B[32m";
    public static final String YELLOW  = "\u001B[33m";
    public static final String BLUE    = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN    = "\u001B[36m";
    public static final String WHITE   = "\u001B[37m";

    // Bright foreground
    public static final String BRIGHT_RED     = "\u001B[91m";
    public static final String BRIGHT_GREEN   = "\u001B[92m";
    public static final String BRIGHT_YELLOW  = "\u001B[93m";
    public static final String BRIGHT_CYAN    = "\u001B[96m";
    public static final String BRIGHT_WHITE   = "\u001B[97m";

    /** Wrap text in color, auto-resetting after. */
    public static String color(String color, String text) {
        return color + text + RESET;
    }

    /** Bold + color wrapper. */
    public static String bold(String color, String text) {
        return BOLD + color + text + RESET;
    }

    /** Print a horizontal rule. */
    public static String rule(int width) {
        return DIM + "\u2500".repeat(width) + RESET;
    }

    /** Format a startup banner box. */
    public static String banner(String title, String... lines) {
        int maxLen = title.length();
        for (String line : lines) {
            maxLen = Math.max(maxLen, line.length());
        }
        int width = maxLen + 4;

        StringBuilder sb = new StringBuilder();
        sb.append(BOLD).append(CYAN);
        sb.append("\u250C").append("\u2500".repeat(width)).append("\u2510\n");
        sb.append("\u2502  ").append(BRIGHT_WHITE).append(title);
        sb.append(" ".repeat(maxLen - title.length()));
        sb.append(CYAN).append("  \u2502\n");
        if (lines.length > 0) {
            sb.append("\u251C").append("\u2500".repeat(width)).append("\u2524\n");
            for (String line : lines) {
                sb.append("\u2502").append(RESET).append("  ").append(line);
                sb.append(" ".repeat(maxLen - line.length()));
                sb.append(BOLD).append(CYAN).append("  \u2502\n");
            }
        }
        sb.append("\u2514").append("\u2500".repeat(width)).append("\u2518");
        sb.append(RESET);
        return sb.toString();
    }

    /** Format a CLI help entry: command in cyan, description in white. */
    public static String helpEntry(String command, String description) {
        return "  " + CYAN + command + RESET + DIM + "  " + description + RESET;
    }
}
