package net.sf.cram.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferUtils {

	public static final int readUnsignedITF8(InputStream is) throws IOException {
		int b1 = is.read();
		if (b1 == -1) throw new EOFException() ;

		if ((b1 & 128) == 0)
			return b1;

		if ((b1 & 64) == 0)
			return ((b1 & 127) << 8) | is.read();

		if ((b1 & 32) == 0) {
			int b2 = is.read();
			int b3 = is.read();
			return ((b1 & 63) << 16) | b2 << 8 | b3;
		}

		if ((b1 & 16) == 0)
			return ((b1 & 31) << 24) | is.read() << 16 | is.read() << 8
					| is.read();

		return ((b1 & 15) << 28) | is.read() << 20 | is.read() << 12
				| is.read() << 4 | (15 & is.read());
	}
	
	public static final int writeUnsignedITF8(int value, OutputStream os) throws IOException {
		if ((value >>> 7) == 0) {
			os.write(value);
			return 8;
		}

		if ((value >>> 14) == 0) {
			os.write( ((value >> 8) | 128));
			os.write( (value & 0xFF));
			return 16;
		}

		if ((value >>> 21) == 0) {
			os.write( ((value >> 16) | 192));
			os.write( ((value >> 8) & 0xFF));
			os.write( (value & 0xFF));
			return 24;
		}

		if ((value >>> 28) == 0) {
			os.write( ((value >> 24) | 224));
			os.write( ((value >> 16) & 0xFF));
			os.write( ((value >> 8) & 0xFF));
			os.write( (value & 0xFF));
			return 32 ;
		}

		os.write( ((value >> 28) | 240));
		os.write( ((value >> 20) & 0xFF));
		os.write( ((value >> 12) & 0xFF));
		os.write( ((value >> 4) & 0xFF));
		os.write( (value & 0xFF));
		return 32 ;
	}

	public static final int readUnsignedITF8(byte[] data) {
		ByteBuffer buf = ByteBuffer.wrap(data);
		int value = ByteBufferUtils.readUnsignedITF8(buf);
		buf.clear();

		return value;
	}

	public static final byte[] writeUnsignedITF8(int value) {
		ByteBuffer buf = ByteBuffer.allocate(10);
		ByteBufferUtils.writeUnsignedITF8(value, buf);

		buf.flip();
		byte[] array = new byte[buf.limit()];
		buf.get(array);

		buf.clear();
		return array;
	}

	public static final int readUnsignedITF8(ByteBuffer buf) {
		int b1 = 0xFF & buf.get();

		if ((b1 & 128) == 0)
			return b1;

		if ((b1 & 64) == 0)
			return ((b1 & 127) << 8) | (0xFF & buf.get());

		if ((b1 & 32) == 0) {
			int b2 = 0xFF & buf.get();
			int b3 = 0xFF & buf.get();
			return ((b1 & 63) << 16) | b2 << 8 | b3;
		}

		if ((b1 & 16) == 0)
			return ((b1 & 31) << 24) | (0xFF & buf.get()) << 16
					| (0xFF & buf.get()) << 8 | (0xFF & buf.get());

		return ((b1 & 15) << 28) | (0xFF & buf.get()) << 20
				| (0xFF & buf.get()) << 12 | (0xFF & buf.get()) << 4
				| (15 & buf.get());
	}

	public static final void writeUnsignedITF8(int value, ByteBuffer buf) {
		if ((value >>> 7) == 0) {
			buf.put((byte) value);
			return;
		}

		if ((value >>> 14) == 0) {
			buf.put((byte) ((value >> 8) | 128));
			buf.put((byte) (value & 0xFF));
			return;
		}

		if ((value >>> 21) == 0) {
			buf.put((byte) ((value >> 16) | 192));
			buf.put((byte) ((value >> 8) & 0xFF));
			buf.put((byte) (value & 0xFF));
			return;
		}

		if ((value >>> 28) == 0) {
			buf.put((byte) ((value >> 24) | 224));
			buf.put((byte) ((value >> 16) & 0xFF));
			buf.put((byte) ((value >> 8) & 0xFF));
			buf.put((byte) (value & 0xFF));
			return;
		}

		buf.put((byte) ((value >> 28) | 240));
		buf.put((byte) ((value >> 20) & 0xFF));
		buf.put((byte) ((value >> 12) & 0xFF));
		buf.put((byte) ((value >> 4) & 0xFF));
		buf.put((byte) (value & 0xFF));
	}

	public static void main(String[] args) {
		ByteBuffer buf = ByteBuffer.allocate(5);

		// Read 192 but expecting 16384
		writeUnsignedITF8(16384, buf);
		buf.flip();
		int v = readUnsignedITF8(buf);
		System.out.println(v);

		long time = System.nanoTime();
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			buf.clear();
			writeUnsignedITF8(i, buf);
			buf.flip();
			int value = readUnsignedITF8(buf);
			if (i != value)
				throw new RuntimeException("Read " + value + " but expecting "
						+ i);

			if (System.nanoTime() - time > 1000 * 1000 * 1000) {
				time = System.nanoTime();
				System.out.println("i=" + i);
			}
		}

		System.out.println("Done.");
	}
}
