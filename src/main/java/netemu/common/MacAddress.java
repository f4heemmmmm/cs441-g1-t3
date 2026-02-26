package netemu.common;

/**
 * A 2-byte ASCII MAC address (e.g. "N1", "R2", "FF").
 */
public final class MacAddress {

    public static final int SIZE = 2;
    public static final MacAddress BROADCAST = new MacAddress("FF");

    private final String value; // always 2 chars

    public MacAddress(String value) {
        if (value == null || value.length() != SIZE) {
            throw new IllegalArgumentException("MAC must be exactly 2 ASCII chars, got: " + value);
        }
        this.value = value;
    }

    /** Decode from buffer at offset. */
    public static MacAddress decode(byte[] buf, int offset) {
        ByteUtil.checkBoundsPublic(buf, offset, SIZE);
        char c0 = (char) (buf[offset] & 0xFF);
        char c1 = (char) (buf[offset + 1] & 0xFF);
        return new MacAddress("" + c0 + c1);
    }

    /** Encode into buffer at offset. */
    public void encode(byte[] buf, int offset) {
        buf[offset] = (byte) value.charAt(0);
        buf[offset + 1] = (byte) value.charAt(1);
    }

    public String value() { return value; }

    public boolean isBroadcast() {
        return "FF".equals(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MacAddress m)) return false;
        return value.equals(m.value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    public String toString() { return value; }
}
