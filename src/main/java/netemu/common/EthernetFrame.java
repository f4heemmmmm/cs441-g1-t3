package netemu.common;

/**
 * Ethernet frame: dstMAC(2) + srcMAC(2) + dataLen(1) + data(0-255).
 * Max total size = 2 + 2 + 1 + 255 = 260 bytes (spec says 261 for data up to 256,
 * but dataLen is 1 byte so max data = 255).
 */
public final class EthernetFrame {

    public static final int HEADER_SIZE = 5; // dstMAC(2) + srcMAC(2) + dataLen(1)
    public static final int MAX_DATA = 255;

    private final MacAddress dstMac;
    private final MacAddress srcMac;
    private final byte[] data;

    public EthernetFrame(MacAddress dstMac, MacAddress srcMac, byte[] data) {
        if (dstMac == null || srcMac == null) throw new IllegalArgumentException("MACs must not be null");
        if (data == null) throw new IllegalArgumentException("data must not be null");
        if (data.length > MAX_DATA) throw new IllegalArgumentException("data too long: " + data.length);
        this.dstMac = dstMac;
        this.srcMac = srcMac;
        this.data = data.clone();
    }

    public MacAddress dstMac() { return dstMac; }
    public MacAddress srcMac() { return srcMac; }
    public byte[] data() { return data.clone(); }
    public int dataLen() { return data.length; }

    /** Encode to wire bytes. */
    public byte[] encode() {
        byte[] buf = new byte[HEADER_SIZE + data.length];
        dstMac.encode(buf, 0);
        srcMac.encode(buf, 2);
        buf[4] = (byte) data.length;
        System.arraycopy(data, 0, buf, HEADER_SIZE, data.length);
        return buf;
    }

    /** Decode from wire bytes. */
    public static EthernetFrame decode(byte[] buf) {
        if (buf == null) throw new IllegalArgumentException("buffer is null");
        if (buf.length < HEADER_SIZE) {
            throw new IllegalArgumentException(
                    "Malformed EthernetFrame: reason=too short (" + buf.length + " < " + HEADER_SIZE + ")");
        }
        MacAddress dst = MacAddress.decode(buf, 0);
        MacAddress src = MacAddress.decode(buf, 2);
        int dataLen = ByteUtil.u8(buf, 4);
        if (buf.length < HEADER_SIZE + dataLen) {
            throw new IllegalArgumentException(
                    "Malformed EthernetFrame: reason=data truncated (need " +
                            (HEADER_SIZE + dataLen) + " got " + buf.length + ")");
        }
        byte[] data = ByteUtil.slice(buf, HEADER_SIZE, dataLen);
        return new EthernetFrame(dst, src, data);
    }

    /** Human-readable protocol name for the IP protocol byte inside data, if parseable. */
    private String protoHint() {
        if (data.length >= IpPacket.HEADER_SIZE) {
            int proto = data[2] & 0xFF;
            return switch (proto) {
                case IpPacket.PROTO_PING -> " proto=PING";
                case IpPacket.PROTO_DATA -> " proto=DATA";
                default -> " proto=" + proto;
            };
        }
        return "";
    }

    @Override
    public String toString() {
        return "Ethernet [dst=" + dstMac + ", src=" + srcMac + ", " + data.length + " bytes" + protoHint() + "]";
    }
}
