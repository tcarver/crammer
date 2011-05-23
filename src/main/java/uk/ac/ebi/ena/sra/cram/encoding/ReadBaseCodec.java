package uk.ac.ebi.ena.sra.cram.encoding;

import java.io.IOException;

import uk.ac.ebi.ena.sra.cram.format.ReadBase;
import uk.ac.ebi.ena.sra.cram.io.BitInputStream;
import uk.ac.ebi.ena.sra.cram.io.BitOutputStream;
import uk.ac.ebi.ena.sra.cram.io.NullBitOutputStream;

public class ReadBaseCodec implements BitCodec<ReadBase> {
	public BitCodec<Byte> qualityScoreCodec;

	@Override
	public ReadBase read(BitInputStream bis) throws IOException {
		// position is not read here because we need to keep track of previous
		// values read from the codec. See ReadFeatureCodec.
		long position = -1L;
		byte qualityScore = qualityScoreCodec.read(bis);

		ReadBase readBase = new ReadBase();
		readBase.setPosition((int) position);
		readBase.setQualityScore(qualityScore);

		return readBase;
	}

	@Override
	public long write(BitOutputStream bos, ReadBase object) throws IOException {
		long len = 0L;

		len += qualityScoreCodec.write(bos, object.getQualityScore());

		return len;
	}

	@Override
	public long numberOfBits(ReadBase readBase) {
		try {
			return write(NullBitOutputStream.INSTANCE, readBase);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
