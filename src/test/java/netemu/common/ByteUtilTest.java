package netemu.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ByteUtilTest {

    @Test
    void u8ReadsUnsigned() {
        byte[] buf = {(byte) 0xFF, 0x00, 0x7F};
        assertEquals(255, ByteUtil.u8(buf, 0));
        assertEquals(0, ByteUtil.u8(buf, 1));
        assertEquals(127, ByteUtil.u8(buf, 2));
    }

    @Test
    void u8BoundsCheck() {
        byte[] buf = {1};
        assertThrows(IndexOutOfBoundsException.class, () -> ByteUtil.u8(buf, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteUtil.u8(buf, -1));
    }

    @Test
    void readWriteLongRoundtrip() {
        byte[] buf = new byte[16];
        long val = 0x123456789ABCDEF0L;
        ByteUtil.writeLong(buf, 4, val);
        assertEquals(val, ByteUtil.readLong(buf, 4));
    }

    @Test
    void readWriteLongZero() {
        byte[] buf = new byte[8];
        ByteUtil.writeLong(buf, 0, 0L);
        assertEquals(0L, ByteUtil.readLong(buf, 0));
    }

    @Test
    void readWriteLongNegative() {
        byte[] buf = new byte[8];
        ByteUtil.writeLong(buf, 0, -1L);
        assertEquals(-1L, ByteUtil.readLong(buf, 0));
    }

    @Test
    void readLongBoundsCheck() {
        byte[] buf = new byte[4];
        assertThrows(IndexOutOfBoundsException.class, () -> ByteUtil.readLong(buf, 0));
    }

    @Test
    void sliceCopiesCorrectly() {
        byte[] buf = {10, 20, 30, 40, 50};
        byte[] s = ByteUtil.slice(buf, 1, 3);
        assertArrayEquals(new byte[]{20, 30, 40}, s);
    }

    @Test
    void sliceBoundsCheck() {
        byte[] buf = {1, 2};
        assertThrows(IndexOutOfBoundsException.class, () -> ByteUtil.slice(buf, 1, 3));
    }

    @Test
    void hexByte() {
        assertEquals("0x00", ByteUtil.hexByte(0));
        assertEquals("0x12", ByteUtil.hexByte(0x12));
        assertEquals("0xff", ByteUtil.hexByte(0xFF));
    }

    @Test
    void parseHexByteWithPrefix() {
        assertEquals(0x12, ByteUtil.parseHexByte("0x12"));
        assertEquals(0xFF, ByteUtil.parseHexByte("0xFF"));
        assertEquals(0x00, ByteUtil.parseHexByte("0x00"));
    }

    @Test
    void parseHexByteWithoutPrefix() {
        assertEquals(0x12, ByteUtil.parseHexByte("12"));
        assertEquals(0xFF, ByteUtil.parseHexByte("FF"));
    }

    @Test
    void nullBufferThrows() {
        assertThrows(IllegalArgumentException.class, () -> ByteUtil.u8(null, 0));
    }
}
