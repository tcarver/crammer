package uk.ac.ebi.ena.sra.cram.format.compression;

import uk.ac.ebi.ena.sra.cram.encoding.GolombRiceCodec;

class GolombRiceCodecStub extends GolombRiceCodec implements NumberCodecStub {

	public GolombRiceCodecStub() {
		super(1);
	}

	@Override
	public EncodingAlgorithm getEncoding() {
		return EncodingAlgorithm.GOLOMB_RICE;
	}

	@Override
	public String getStringRepresentation() {
		return String.format("%d,%d,%d", getLog2m(), getOffset(),
				isQuotientBit() ? 1 : 0);
	}

	@Override
	public void initFromString(String spec) throws CramCompressionException {
		String[] params = StringRepresentation.parse(spec);
		switch (params.length) {
		case 1:
			int log2m = StringRepresentation.toInt(params[0]);
			setLog2m(log2m);
			break ;
		case 3:
			log2m = StringRepresentation.toInt(params[0]);
			setLog2m(log2m);
			boolean quotientBit = StringRepresentation.toBoolean(params[1]);
			setQuotientBit(quotientBit);
			long offset = StringRepresentation.toLong(params[2]);
			setOffset(offset);
			break ;
		default:
			throw new CramCompressionException(
					"Not supported number of parameters to golomb-rice codec: "
							+ params.length);
		}
	}
	
	@Override
	public Object[] getParameters() {
		return new Object[]{getLog2m(), getOffset(), isQuotientBit()};
	}
	@Override
	public void setParameters(Object[] params) {
		setLog2m((Integer) params[0]);
		setOffset((Long) params[1]);
		setQuotientBit((Boolean) params[2]);
	}
}
