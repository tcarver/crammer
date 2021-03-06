package net.sf.cram.encoding;

import java.io.InputStream;
import java.util.Map;


import net.sf.cram.EncodingID;
import net.sf.cram.EncodingParams;
import net.sf.cram.io.ByteBufferUtils;
import net.sf.cram.io.ExposedByteArrayOutputStream;

public class GolombIntegerEncoding implements Encoding<Integer> {
	public static final EncodingID ENCODING_ID = EncodingID.GOLOMB;
	private int m;

	public GolombIntegerEncoding() {
	}
	
	public GolombIntegerEncoding(int m) {
		this.m = m;
	}

	@Override
	public EncodingID id() {
		return ENCODING_ID;
	}

	public static EncodingParams toParam(int m) {
		GolombIntegerEncoding e = new GolombIntegerEncoding();
		e.m = m;
		return new EncodingParams(ENCODING_ID, e.toByteArray());
	}

	@Override
	public byte[] toByteArray() {
		return ByteBufferUtils.writeUnsignedITF8(m);
	}

	@Override
	public void fromByteArray(byte[] data) {
		m = ByteBufferUtils.readUnsignedITF8(data);
	}

	@Override
	public BitCodec<Integer> buildCodec(Map<Integer, InputStream> inputMap,
			Map<Integer, ExposedByteArrayOutputStream> outputMap) {
		return new GolombIntegerCodec(m);
	}

}
