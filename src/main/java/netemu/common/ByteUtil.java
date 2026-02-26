package netemu.common;

/**
 * Safe byte-array parsing helpers. All methods bounds-check before reading.
 */
public final class ByteUtil {

    private ByteUtil() {}

    /** Read one unsigned byte at {@code offset}. */
    public static int u8(byte[] buf, int offset) {
        checkBounds(buf, offset, 1);
        return buf[offset] & 0xFF;
    }

    /** Read a big-endian long (8 bytes) starting at {@code offset}. */
    public static long readLong(byte[] buf, int offset) {
        checkBounds(buf, offset, 8);
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (buf[offset + i] & 0xFFL);
        }
        return v;
    }

    /** Write a big-endian long (8 bytes) into {@code buf} at {@code offset}. */
    public static void writeLong(byte[] buf, int offset, long value) {
        checkBounds(buf, offset, 8);
        for (int i = 7; i >= 0; i--) {
            buf[offset + i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
    }

    /** Copy {@code len} bytes from {@code src} starting at {@code srcOff} into a new array. */
    public static byte[] slice(byte[] src, int srcOff, int len) {
        checkBounds(src, srcOff, len);
        byte[] dst = new byte[len];
        System.arraycopy(src, srcOff, dst, 0, len);
        return dst;
    }

    /** Format a single byte as hex, e.g. "0x12". */
    public static String hexByte(int b) {
        return String.format("0x%02x", b & 0xFF);
    }

    /** Parse "0x12" or "12" to an int. */
    public static int parseHexByte(String s) {
        String stripped = s.startsWith("0x") || s.startsWith("0X") ? s.substring(2) : s;
        return Integer.parseInt(stripped, 16);
    }

    public static void checkBoundsPublic(byte[] buf, int offset, int len) {
        checkBounds(buf, offset, len);
    }

    private static void checkBounds(byte[] buf, int offset, int len) {
        if (buf == null) throw new IllegalArgumentException("buffer is null");
        if (offset < 0 || len < 0 || offset + len > buf.length) {
            throw new IndexOutOfBoundsException(
                    "bounds check failed: offset=" + offset + " len=" + len + " bufLen=" + buf.length);
        }
    }
}
