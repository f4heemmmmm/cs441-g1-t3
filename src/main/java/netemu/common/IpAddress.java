package netemu.common;

/**
 * A 1-byte IP address (e.g. 0x12, 0x22).
 */
public final class IpAddress {

    public static final int SIZE = 1;

    private final int value; // 0–255

    public IpAddress(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("IP must be 0-255, got: " + value);
        }
        this.value = value;
    }

    public int value() { return value; }

    /** High nibble determines LAN: 0x1_→LAN1, 0x2_→LAN2, 0x3_→LAN3. */
    public int lanId() {
        return (value >> 4) & 0x0F;
    }

    public void encode(byte[] buf, int offset) {
        buf[offset] = (byte) value;
    }

    public static IpAddress decode(byte[] buf, int offset) {
        return new IpAddress(ByteUtil.u8(buf, offset));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpAddress ip)) return false;
        return value == ip.value;
    }

    @Override
    public int hashCode() { return value; }

    @Override
    public String toString() { return ByteUtil.hexByte(value); }
}
