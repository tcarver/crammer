package uk.ac.ebi.ena.sra.cram.format;

import java.io.Serializable;

import uk.ac.ebi.ena.sra.cram.encoding.BaseChange;

public class SubstitutionVariation implements Serializable, ReadFeature {

	private int position;
	private byte base;
	private byte refernceBase;
	private byte qualityScore;
	private BaseChange baseChange;

	public static final byte operator = 'S';

	@Override
	public byte getOperator() {
		return operator;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public byte getBase() {
		return base;
	}

	public void setBase(byte base) {
		this.base = base;
	}

	public byte getRefernceBase() {
		return refernceBase;
	}

	public void setRefernceBase(byte refernceBase) {
		this.refernceBase = refernceBase;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SubstitutionVariation))
			return false;

		SubstitutionVariation v = (SubstitutionVariation) obj;

		if (position != v.position)
			return false;
		if (base != v.base)
			return false;
		if (refernceBase != v.refernceBase)
			return false;

		return true;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getSimpleName() + "[");
		sb.append("position=").append(position);
		sb.append("; base=").append((char) base);
		sb.append("; quality score=").append(qualityScore);
		sb.append("; referenceBase=").append((char) refernceBase);
		sb.append("] ");
		return sb.toString();
	}

	public byte getQualityScore() {
		return qualityScore;
	}

	public void setQualityScore(byte qualityScore) {
		this.qualityScore = qualityScore;
	}

	public BaseChange getBaseChange() {
		return baseChange;
	}

	public void setBaseChange(BaseChange baseChange) {
		this.baseChange = baseChange;
	}
}
