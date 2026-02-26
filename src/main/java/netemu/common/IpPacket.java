package netemu.common;

/**
 * IP packet: srcIP(1) + dstIP(1) + protocol(1) + dataLen(1) + data(0-255).
 * Header size = 4 bytes.
 */
public final class IpPacket {

    public static final int HEADER_SIZE = 4;
    public static final int MAX_DATA = 252; // keep total inside Ethernet max
    public static final int PROTO_PING = 1;
    public static final int PROTO_DATA = 0;

    private final IpAddress srcIp;
    private final IpAddress dstIp;
    private final int protocol;
    private final byte[] data;

    public IpPacket(IpAddress srcIp, IpAddress dstIp, int protocol, byte[] data) {
        if (srcIp == null || dstIp == null) throw new IllegalArgumentException("IPs must not be null");
        if (data == null) throw new IllegalArgumentException("data must not be null");
        if (data.length > MAX_DATA) throw new IllegalArgumentException("data too long: " + data.length);
        if (protocol < 0 || protocol > 255) throw new IllegalArgumentException("protocol out of range");
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.protocol = protocol;
        this.data = data.clone();
    }

    public IpAddress srcIp() { return srcIp; }
    public IpAddress dstIp() { return dstIp; }
    public int protocol() { return protocol; }
    public byte[] data() { return data.clone(); }
    public int dataLen() { return data.length; }

    /** Encode to bytes (to be placed in EthernetFrame.data). */
    public byte[] encode() {
        byte[] buf = new byte[HEADER_SIZE + data.length];
        srcIp.encode(buf, 0);
        dstIp.encode(buf, 1);
        buf[2] = (byte) protocol;
        buf[3] = (byte) data.length;
        System.arraycopy(data, 0, buf, HEADER_SIZE, data.length);
        return buf;
    }

    /** Decode from bytes. */
    public static IpPacket decode(byte[] buf) {
        if (buf == null) throw new IllegalArgumentException("buffer is null");
        if (buf.length < HEADER_SIZE) {
            throw new IllegalArgumentException(
                    "Malformed IpPacket: reason=too short (" + buf.length + " < " + HEADER_SIZE + ")");
        }
        IpAddress src = IpAddress.decode(buf, 0);
        IpAddress dst = IpAddress.decode(buf, 1);
        int proto = ByteUtil.u8(buf, 2);
        int dataLen = ByteUtil.u8(buf, 3);
        if (buf.length < HEADER_SIZE + dataLen) {
            throw new IllegalArgumentException(
                    "Malformed IpPacket: reason=data truncated (need " +
                            (HEADER_SIZE + dataLen) + " got " + buf.length + ")");
        }
        byte[] data = ByteUtil.slice(buf, HEADER_SIZE, dataLen);
        return new IpPacket(src, dst, proto, data);
    }

    public String protoName() {
        return switch (protocol) {
            case PROTO_PING -> "PING";
            case PROTO_DATA -> "DATA";
            default -> String.valueOf(protocol);
        };
    }

    @Override
    public String toString() {
        return "IP [" + srcIp + " -> " + dstIp + ", proto=" + protoName() + ", " + data.length + " bytes]";
    }
}
