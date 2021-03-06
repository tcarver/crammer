/*******************************************************************************
 * Copyright 2012 EMBL-EBI, Hinxton outstation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.sf.cram;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sf.cram.encoding.read_features.DeletionVariation;
import net.sf.cram.encoding.read_features.InsertBase;
import net.sf.cram.encoding.read_features.InsertionVariation;
import net.sf.cram.encoding.read_features.ReadFeature;
import net.sf.cram.encoding.read_features.SoftClipVariation;

public class CramRecord implements Serializable{

	public Collection<ReadTag> tags;

	public int index = 0;
	private int alignmentStart;
	public int alignmentStartOffsetFromPreviousRecord;

	private int readLength;

	public int recordsToNextFragment = -1;

	private byte[] readBases;
	private byte[] qualityScores;

	private List<ReadFeature> readFeatures;

	private int readGroupID = 0;

	// flags:
	public Integer flags = null;
	public boolean multiFragment = false;
	public boolean properPair = false;
	public boolean segmentUnmapped = false;
	public boolean negativeStrand = false;
	public boolean firstSegment = false;
	public boolean lastSegment = false;
	public boolean secondaryALignment = false;
	public boolean vendorFiltered = false;
	public boolean duplicate = false;

	// pointers to the previous and next segments in the template:
	public CramRecord next, previous;

	// mate flags:
	private Byte mateFlags = null;
	public boolean mateUmapped = false;
	public boolean mateNegativeStrand = false;

	// compression flags:
	private Byte compressionFlags = null;
	public boolean hasMateDownStream = false;
	public boolean detached = false;
	public boolean forcePreserveQualityScores = false;

	public int mateSequnceID = 0;
	public int mateAlignmentStart = 0;

	private byte mappingQuality;

	private String sequenceName;
	public int sequenceId;
	private String readName;
	public int templateSize;
	public long counter = 1;

	public int getFlags() {
		if (flags == null) {
			int b = 0;
			b |= multiFragment ? 1 : 0;

			b <<= 1;
			b |= properPair ? 1 : 0;
			b <<= 1;
			b |= segmentUnmapped ? 1 : 0;
			b <<= 1;
			b |= negativeStrand ? 1 : 0;

			b <<= 1;
			b |= firstSegment ? 1 : 0;
			b <<= 1;
			b |= lastSegment ? 1 : 0;
			b <<= 1;
			b |= secondaryALignment ? 1 : 0;
			b <<= 1;
			b |= vendorFiltered ? 1 : 0;
			b <<= 1;
			b |= duplicate ? 1 : 0;

			flags = new Integer(b);
		}
		return flags;
	}

	public void setFlags(int value) {
		int b = value;
		
		duplicate = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		vendorFiltered = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		secondaryALignment = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		lastSegment = ((b & 1) == 0) ? false : true;

		b >>>= 1;
		firstSegment = ((b & 1) == 0) ? false : true;
		b >>>= 1;

		negativeStrand = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		segmentUnmapped = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		properPair = ((b & 1) == 0) ? false : true;

		b >>>= 1;
		multiFragment = ((b & 1) == 0) ? false : true;

		this.flags = value;
	}

	public void resetFlags() {
		flags = null;
	}

	public byte getMateFlags() {
		if (mateFlags == null) {
			byte b = 0;
			b |= mateUmapped ? 1 : 0;
			b <<= 1;
			b |= mateNegativeStrand ? 1 : 0;
			mateFlags = new Byte(b);
		}
		return mateFlags;
	}

	public void setMateFlags(byte value) {
		int b = 0xFF & value;

		mateNegativeStrand = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		mateUmapped = ((b & 1) == 0) ? false : true;

		mateFlags = value;
	}

	public void resetMateFlags() {
		mateFlags = null;
	}

	public byte getCompressionFlags() {
		if (compressionFlags == null) {
			byte b = 0;
			b |= hasMateDownStream ? 1 : 0;
			b <<= 1;
			b |= detached ? 1 : 0;
			b <<= 1;
			b |= forcePreserveQualityScores ? 1 : 0;
			compressionFlags = new Byte(b);
		}
		return compressionFlags;
	}

	public void setCompressionFlags(byte value) {
		int b = 0xFF & value;

		forcePreserveQualityScores = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		detached = ((b & 1) == 0) ? false : true;
		b >>>= 1;
		hasMateDownStream = ((b & 1) == 0) ? false : true;

		compressionFlags = value;
	}

	public void resetCompressionFlags() {
		compressionFlags = null;
	}

	public int getAlignmentStart() {
		return alignmentStart;
	}

	public void setAlignmentStart(int alignmentStart) {
		this.alignmentStart = alignmentStart;
	}

	public boolean isNegativeStrand() {
		return negativeStrand;
	}

	public void setNegativeStrand(boolean negativeStrand) {
		this.negativeStrand = negativeStrand;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CramRecord))
			return false;

		CramRecord r = (CramRecord) obj;

		if (alignmentStart != r.alignmentStart)
			return false;
		if (negativeStrand != r.negativeStrand)
			return false;
		if (vendorFiltered != r.vendorFiltered)
			return false;
		if (segmentUnmapped != r.segmentUnmapped)
			return false;
		if (readLength != r.readLength)
			return false;
		if (lastSegment != r.lastSegment)
			return false;
		if (recordsToNextFragment != r.recordsToNextFragment)
			return false;
		if (firstSegment != r.firstSegment)
			return false;
		if (mappingQuality != r.mappingQuality)
			return false;

		if (!deepEquals(readFeatures, r.readFeatures))
			return false;

		if (!Arrays.equals(readBases, r.readBases))
			return false;
		if (!Arrays.equals(qualityScores, r.qualityScores))
			return false;

		if (!areEqual(flags, r.flags))
			return false;

		if (!areEqual(readName, r.readName))
			return false;

		return true;
	}

	private boolean areEqual(Object o1, Object o2) {
		if (o1 == null && o2 == null)
			return true;
		return o1 != null && o1.equals(o2);
	}

	private boolean deepEquals(Collection<?> c1, Collection<?> c2) {
		if ((c1 == null || c1.isEmpty()) && (c2 == null || c2.isEmpty()))
			return true;
		if (c1 != null)
			return c1.equals(c2);
		return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("[");
		if (readName != null)
			sb.append(readName).append("; ");
		sb.append("flags=").append(getFlags());
		sb.append("; aloffset=").append(alignmentStartOffsetFromPreviousRecord);
		sb.append("; mateoffset=").append(recordsToNextFragment);
		sb.append("; mappingQuality=").append(mappingQuality);

		if (readFeatures != null)
			for (ReadFeature feature : readFeatures)
				sb.append("; ").append(feature.toString());

		if (readBases != null)
			sb.append("; ").append("bases: ").append(new String(readBases));
		if (qualityScores != null)
			sb.append("; ").append("qscores: ")
					.append(new String(qualityScores));

		sb.append("]");
		return sb.toString();
	}

	public int getReadLength() {
		return readLength;
	}

	public void setReadLength(int readLength) {
		this.readLength = readLength;
	}

	public boolean isLastFragment() {
		return lastSegment;
	}

	public void setLastFragment(boolean lastFragment) {
		this.lastSegment = lastFragment;
	}

	public int getRecordsToNextFragment() {
		return recordsToNextFragment;
	}

	public void setRecordsToNextFragment(int recordsToNextFragment) {
		this.recordsToNextFragment = recordsToNextFragment;
	}

	public boolean isReadMapped() {
		return segmentUnmapped;
	}

	public void setReadMapped(boolean readMapped) {
		this.segmentUnmapped = readMapped;
	}

	public List<ReadFeature> getReadFeatures() {
		return readFeatures;
	}

	public void setReadFeatures(List<ReadFeature> readFeatures) {
		this.readFeatures = readFeatures;
	}

	public byte[] getReadBases() {
		return readBases;
	}

	public void setReadBases(byte[] readBases) {
		this.readBases = readBases;
	}

	public byte[] getQualityScores() {
		return qualityScores;
	}

	public void setQualityScores(byte[] qualityScores) {
		this.qualityScores = qualityScores;
	}

	public int getReadGroupID() {
		return readGroupID;
	}

	public void setReadGroupID(int readGroupID) {
		this.readGroupID = readGroupID;
	}

	public boolean isFirstInPair() {
		return firstSegment;
	}

	public void setFirstInPair(boolean firstInPair) {
		this.firstSegment = firstInPair;
	}

	public byte getMappingQuality() {
		return mappingQuality;
	}

	public void setMappingQuality(byte mappingQuality) {
		this.mappingQuality = mappingQuality;
	}

	public boolean isProperPair() {
		return properPair;
	}

	public void setProperPair(boolean properPair) {
		this.properPair = properPair;
	}

	public boolean isDuplicate() {
		return duplicate;
	}

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public String getReadName() {
		return readName;
	}

	public void setReadName(String readName) {
		this.readName = readName;
	}

	public int calcualteAlignmentEnd() {
		if (readFeatures == null || readFeatures.isEmpty())
			return alignmentStart + readLength;
		
		int len = readLength;
		for (ReadFeature f : readFeatures) {
			switch (f.getOperator()) {
			case InsertBase.operator:
				len--;
				break;
			case InsertionVariation.operator:
				len -= ((InsertionVariation) f).getSequence().length;
				break;
			case SoftClipVariation.operator:
				len -= ((SoftClipVariation) f).getSequence().length;
				break;
			case DeletionVariation.operator:
				len += ((DeletionVariation) f).getLength();
				break;

			default:
				break;
			}
		}
		return alignmentStart + len;
	}

}
