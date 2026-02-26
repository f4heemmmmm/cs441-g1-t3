package netemu.common;

/**
 * Ping protocol carried inside IP data (protocol=1).
 *
 * PingRequest: type(1)=0x01, seq(1), timestamp(8)
 * PingReply:   type(1)=0x02, seq(1), timestamp(8)
 * Total: 10 bytes
 */
public final class PingMessage {

    public static final int SIZE = 10;
    public static final byte TYPE_REQUEST = 0x01;
    public static final byte TYPE_REPLY = 0x02;

    private final byte type;
    private final int seq;
    private final long timestamp;

    public PingMessage(byte type, int seq, long timestamp) {
        if (type != TYPE_REQUEST && type != TYPE_REPLY) {
            throw new IllegalArgumentException("Invalid ping type: " + type);
        }
        if (seq < 0 || seq > 255) {
            throw new IllegalArgumentException("seq out of range: " + seq);
        }
        this.type = type;
        this.seq = seq;
        this.timestamp = timestamp;
    }

    public byte type() { return type; }
    public int seq() { return seq; }
    public long timestamp() { return timestamp; }

    public boolean isRequest() { return type == TYPE_REQUEST; }
    public boolean isReply() { return type == TYPE_REPLY; }

    public byte[] encode() {
        byte[] buf = new byte[SIZE];
        buf[0] = type;
        buf[1] = (byte) seq;
        ByteUtil.writeLong(buf, 2, timestamp);
        return buf;
    }

    public static PingMessage decode(byte[] buf) {
        if (buf == null) throw new IllegalArgumentException("buffer is null");
        if (buf.length < SIZE) {
            throw new IllegalArgumentException(
                    "Malformed PingMessage: reason=too short (" + buf.length + " < " + SIZE + ")");
        }
        byte type = buf[0];
        int seq = ByteUtil.u8(buf, 1);
        long ts = ByteUtil.readLong(buf, 2);
        return new PingMessage(type, seq, ts);
    }

    public String typeName() {
        return (type == TYPE_REQUEST) ? "REQUEST" : "REPLY";
    }

    @Override
    public String toString() {
        return "Ping " + typeName() + " [seq=" + seq + "]";
    }
}
