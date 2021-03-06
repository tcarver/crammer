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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.cram.encoding.read_features.ReadFeature;
import net.sf.picard.PicardException;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.picard.reference.ReferenceSequenceFile;
import net.sf.picard.reference.ReferenceSequenceFileFactory;
import net.sf.picard.sam.SamPairUtil;
import net.sf.picard.util.Log;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMTag;

public class Utils {

	private static Log log = Log.getInstance(Utils.class);

	public static byte[] transformSequence(byte[] bases, boolean compliment,
			boolean reverse) {
		byte[] result = new byte[bases.length];
		for (int i = 0; i < bases.length; i++) {
			byte base = bases[i];

			int index = reverse ? bases.length - i - 1 : i;

			result[index] = compliment ? complimentBase(base) : base;
		}
		return result;
	}

	public static final byte complimentBase(byte base) {
		switch (base) {
		case 'A':
			return 'T';
		case 'C':
			return 'G';
		case 'G':
			return 'C';
		case 'T':
			return 'A';
		case 'N':
			return 'N';

		default:
			throw new RuntimeException("Unkown base: " + base);
		}
	}

	public static Byte[] autobox(byte[] array) {
		Byte[] newArray = new Byte[array.length];
		for (int i = 0; i < array.length; i++)
			newArray[i] = array[i];
		return newArray;
	}

	public static Integer[] autobox(int[] array) {
		Integer[] newArray = new Integer[array.length];
		for (int i = 0; i < array.length; i++)
			newArray[i] = array[i];
		return newArray;
	}

	public static void changeReadLength(SAMRecord record, int newLength) {
		if (newLength == record.getReadLength())
			return;
		if (newLength < 1 || newLength >= record.getReadLength())
			throw new IllegalArgumentException("Cannot change read length to "
					+ newLength);

		List<CigarElement> newCigarElements = new ArrayList<CigarElement>();
		int len = 0;
		for (CigarElement ce : record.getCigar().getCigarElements()) {
			switch (ce.getOperator()) {
			case D:
				break;
			case S:
				// dump = true;
				// len -= ce.getLength();
				// break;
			case M:
			case I:
			case X:
				len += ce.getLength();
				break;

			default:
				throw new IllegalArgumentException(
						"Unexpected cigar operator: " + ce.getOperator()
								+ " in cigar " + record.getCigarString());
			}

			if (len <= newLength) {
				newCigarElements.add(ce);
				continue;
			}
			CigarElement newCe = new CigarElement(ce.getLength()
					- (record.getReadLength() - newLength), ce.getOperator());
			if (newCe.getLength() > 0)
				newCigarElements.add(newCe);
			break;
		}

		byte[] newBases = new byte[newLength];
		System.arraycopy(record.getReadBases(), 0, newBases, 0, newLength);
		record.setReadBases(newBases);

		byte[] newScores = new byte[newLength];
		System.arraycopy(record.getBaseQualities(), 0, newScores, 0, newLength);

		record.setCigar(new Cigar(newCigarElements));
	}

	public static void reversePositionsInRead(CramRecord record) {
		if (record.getReadFeatures() == null
				|| record.getReadFeatures().isEmpty())
			return;
		for (ReadFeature f : record.getReadFeatures())
			f.setPosition((int) (record.getReadLength() - f.getPosition() - 1));

		Collections.reverse(record.getReadFeatures());
	}

	public static byte[] getBasesFromReferenceFile(String referenceFilePath,
			String seqName, int from, int length) {
		ReferenceSequenceFile referenceSequenceFile = ReferenceSequenceFileFactory
				.getReferenceSequenceFile(new File(referenceFilePath));
		ReferenceSequence sequence = referenceSequenceFile.getSequence(seqName);
		byte[] bases = referenceSequenceFile.getSubsequenceAt(
				sequence.getName(), from, from + length).getBases();
		return bases;
	}

	public static void capitaliseAndCheckBases(byte[] bases, boolean strict) {
		for (int i = 0; i < bases.length; i++) {
			switch (bases[i]) {
			case 'A':
			case 'C':
			case 'G':
			case 'T':
			case 'N':
				break;
			case 'a':
				bases[i] = 'A';
				break;
			case 'c':
				bases[i] = 'C';
				break;
			case 'g':
				bases[i] = 'G';
				break;
			case 't':
				bases[i] = 'T';
				break;
			case 'n':
				bases[i] = 'N';
				break;

			default:
				if (strict)
					throw new RuntimeException("Illegal base at " + i + ": "
							+ bases[i]);
				else
					bases[i] = 'N';
				break;
			}
		}
	}

	/**
	 * Copied from net.sf.picard.sam.SamPairUtil. This is a more permissive
	 * version of the method, which does not reset alignment start and reference
	 * for unmapped reads.
	 * 
	 * @param rec1
	 * @param rec2
	 * @param header
	 */
	public static void setLooseMateInfo(final SAMRecord rec1,
			final SAMRecord rec2, final SAMFileHeader header) {
		if (rec1.getReferenceName() != SAMRecord.NO_ALIGNMENT_REFERENCE_NAME
				&& rec1.getReferenceIndex() == SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX)
			rec1.setReferenceIndex(header.getSequenceIndex(rec1
					.getReferenceName()));
		if (rec2.getReferenceName() != SAMRecord.NO_ALIGNMENT_REFERENCE_NAME
				&& rec2.getReferenceIndex() == SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX)
			rec2.setReferenceIndex(header.getSequenceIndex(rec2
					.getReferenceName()));

		// If neither read is unmapped just set their mate info
		if (!rec1.getReadUnmappedFlag() && !rec2.getReadUnmappedFlag()) {

			rec1.setMateReferenceIndex(rec2.getReferenceIndex());
			rec1.setMateAlignmentStart(rec2.getAlignmentStart());
			rec1.setMateNegativeStrandFlag(rec2.getReadNegativeStrandFlag());
			rec1.setMateUnmappedFlag(false);
			rec1.setAttribute(SAMTag.MQ.name(), rec2.getMappingQuality());

			rec2.setMateReferenceIndex(rec1.getReferenceIndex());
			rec2.setMateAlignmentStart(rec1.getAlignmentStart());
			rec2.setMateNegativeStrandFlag(rec1.getReadNegativeStrandFlag());
			rec2.setMateUnmappedFlag(false);
			rec2.setAttribute(SAMTag.MQ.name(), rec1.getMappingQuality());
		}
		// Else if they're both unmapped set that straight
		else if (rec1.getReadUnmappedFlag() && rec2.getReadUnmappedFlag()) {
			rec1.setMateNegativeStrandFlag(rec2.getReadNegativeStrandFlag());
			rec1.setMateUnmappedFlag(true);
			rec1.setAttribute(SAMTag.MQ.name(), null);
			rec1.setInferredInsertSize(0);

			rec2.setMateNegativeStrandFlag(rec1.getReadNegativeStrandFlag());
			rec2.setMateUnmappedFlag(true);
			rec2.setAttribute(SAMTag.MQ.name(), null);
			rec2.setInferredInsertSize(0);
		}
		// And if only one is mapped copy it's coordinate information to the
		// mate
		else {
			final SAMRecord mapped = rec1.getReadUnmappedFlag() ? rec2 : rec1;
			final SAMRecord unmapped = rec1.getReadUnmappedFlag() ? rec1 : rec2;

			mapped.setMateReferenceIndex(unmapped.getReferenceIndex());
			mapped.setMateAlignmentStart(unmapped.getAlignmentStart());
			mapped.setMateNegativeStrandFlag(unmapped
					.getReadNegativeStrandFlag());
			mapped.setMateUnmappedFlag(true);
			mapped.setInferredInsertSize(0);

			unmapped.setMateReferenceIndex(mapped.getReferenceIndex());
			unmapped.setMateAlignmentStart(mapped.getAlignmentStart());
			unmapped.setMateNegativeStrandFlag(mapped
					.getReadNegativeStrandFlag());
			unmapped.setMateUnmappedFlag(false);
			unmapped.setInferredInsertSize(0);
		}

		boolean firstIsFirst = rec1.getAlignmentStart() < rec2
				.getAlignmentStart();
		int insertSize = firstIsFirst ? SamPairUtil.computeInsertSize(rec1,
				rec2) : SamPairUtil.computeInsertSize(rec2, rec1);

		rec1.setInferredInsertSize(firstIsFirst ? insertSize : -insertSize);
		rec2.setInferredInsertSize(firstIsFirst ? -insertSize : insertSize);

	}

	public static int computeInsertSize(CramRecord firstEnd,
			CramRecord secondEnd) {
		if (firstEnd.segmentUnmapped || secondEnd.segmentUnmapped) {
			return 0;
		}
		if (firstEnd.sequenceId != secondEnd.sequenceId) {
			return 0;
		}

		final int firstEnd5PrimePosition = firstEnd.negativeStrand ? firstEnd
				.calcualteAlignmentEnd() : firstEnd.getAlignmentStart();
		final int secondEnd5PrimePosition = secondEnd.negativeStrand ? secondEnd
				.calcualteAlignmentEnd() : secondEnd.getAlignmentStart();

		int adjustment = (secondEnd5PrimePosition >= firstEnd5PrimePosition) ? +1
				: -1;
		// this seems to correlate with reality more, although Picard disagrees: 
		adjustment = -adjustment ;
		
		return secondEnd5PrimePosition - firstEnd5PrimePosition + adjustment;
	}

	public static IndexedFastaSequenceFile createIndexedFastaSequenceFile(
			File file) throws RuntimeException, FileNotFoundException {
		if (IndexedFastaSequenceFile.canCreateIndexedFastaReader(file)) {
			IndexedFastaSequenceFile ifsFile = new IndexedFastaSequenceFile(
					file);

			return ifsFile;
		} else
			throw new RuntimeException(
					"Reference fasta file is not indexed or index file not found. Try executing 'samtools faidx "
							+ file.getAbsolutePath() + "'");
	}

	public static ReferenceSequence getReferenceSequenceOrNull(
			ReferenceSequenceFile rsFile, String name) {
		ReferenceSequence rs = null;
		try {
			return rsFile.getSequence(name);
		} catch (PicardException e) {
			return null;
		}
	}

	private static final Pattern chrPattern = Pattern.compile("chr.*",
			Pattern.CASE_INSENSITIVE);

	public static byte[] getBasesOrNull(ReferenceSequenceFile rsFile,
			String name, int start, int len) {
		ReferenceSequence rs = getReferenceSequenceOrNull(rsFile, name);
		if (rs == null && name.equals("M")) {
			rs = getReferenceSequenceOrNull(rsFile, "MT");
		}

		if (rs == null && name.equals("MT")) {
			rs = getReferenceSequenceOrNull(rsFile, "M");
		}

		boolean chrPatternMatch = chrPattern.matcher(name).matches();
		if (rs == null) {
			if (chrPatternMatch)
				rs = getReferenceSequenceOrNull(rsFile, name.substring(3));
			else
				rs = getReferenceSequenceOrNull(rsFile, "chr" + name);
		}
		if (rs == null)
			return null;

		if (len < 1)
			return rs.getBases();
		else
			return rsFile.getSubsequenceAt(rs.getName(), 1, len).getBases();
	}

	public static byte[] getReferenceSequenceBases(
			ReferenceSequenceFile referenceSequenceFile, String seqName)
			throws RuntimeException {
		long time1 = System.currentTimeMillis();
		byte[] refBases = Utils.getBasesOrNull(referenceSequenceFile, seqName,
				1, 0);
		if (refBases == null)
			throw new RuntimeException("Reference sequence " + seqName
					+ " not found in the fasta file "
					+ referenceSequenceFile.toString());

		long time2 = System.currentTimeMillis();
		log.debug(String.format("Reference sequence %s read in %.2f seconds.",
				seqName, (time2 - time1) / 1000f));

		Utils.capitaliseAndCheckBases(refBases, false);

		long time3 = System.currentTimeMillis();
		log.debug(String.format(
				"Reference sequence normalized in %.2f seconds.",
				(time3 - time2) / 1000f));
		return refBases;
	}

	/**
	 * A rip off samtools bam_md.c
	 * 
	 * @param record
	 * @param ref
	 * @param flag
	 * @return
	 */
	public static void calculateMdAndNmTags(SAMRecord record, byte[] ref,
			boolean calcMD, boolean calcNM) {
		Cigar cigar = record.getCigar();
		List<CigarElement> cigarElements = cigar.getCigarElements();
		byte[] seq = record.getReadBases();
		int start = record.getAlignmentStart() - 1;
		int i, x, y, u = 0;
		int nm = 0;
		StringBuffer str = new StringBuffer();

		for (i = y = 0, x = start; i < cigarElements.size(); ++i) {
			CigarElement ce = cigarElements.get(i);
			int j, l = ce.getLength();
			CigarOperator op = ce.getOperator();
			if (op == CigarOperator.MATCH_OR_MISMATCH || op == CigarOperator.EQ
					|| op == CigarOperator.X) {
				for (j = 0; j < l; ++j) {
					int z = y + j;

					if (ref.length <= x + j)
						break; // out of boundary

					int c1 = 0;
					int c2 = 0;
					// try {
					c1 = seq[z];
					c2 = ref[x + j];
					// } catch (ArrayIndexOutOfBoundsException e) {
					// System.err.println("Offending record: ");
					// System.err.println(record.getSAMString());
					// System.err.printf("z=%d; x=%d; j=%d; i=%d; y=%d, l=%d\n",
					// z, x, j, i, y, l);
					// System.err.printf("Cigar op=%s\n", op.name());
					// throw e ;
					// }

					if ((c1 == c2 && c1 != 15 && c2 != 15) || c1 == 0) {
						// a match
						++u;
					} else {
						str.append(u);
						str.appendCodePoint(ref[x + j]);
						u = 0;
						++nm;
					}
				}
				if (j < l)
					break;
				x += l;
				y += l;
			} else if (op == CigarOperator.DELETION) {
				str.append(u);
				str.append('^');
				for (j = 0; j < l; ++j) {
					if (ref[x + j] == 0)
						break;
					str.appendCodePoint(ref[x + j]);
				}
				u = 0;
				if (j < l)
					break;
				x += l;
				nm += l;
			} else if (op == CigarOperator.INSERTION
					|| op == CigarOperator.SOFT_CLIP) {
				y += l;
				if (op == CigarOperator.INSERTION)
					nm += l;
			} else if (op == CigarOperator.SKIPPED_REGION) {
				x += l;
			}
		}
		str.append(u);

		if (calcMD)
			record.setAttribute(SAMTag.MD.name(), str.toString());
		if (calcNM)
			record.setAttribute(SAMTag.NM.name(), nm);
	}

	public static int[][] sortByFirst(int[] array1, int[] array2) {
		int[][] sorted = new int[array1.length][2];
		for (int i = 0; i < array1.length; i++) {
			sorted[i][0] = array1[i];
			sorted[i][1] = array2[i];
		}

		Arrays.sort(sorted, intArray_2_Comparator);

		int[][] result = new int[2][array1.length];
		for (int i = 0; i < array1.length; i++) {
			result[0][i] = sorted[i][0];
			result[1][i] = sorted[i][1];
		}

		return result;
	}

	private static Comparator<int[]> intArray_2_Comparator = new Comparator<int[]>() {

		@Override
		public int compare(int[] o1, int[] o2) {
			int result = o1[0] - o2[0];
			if (result != 0)
				return -result;

			return -(o1[1] - o2[1]);
		}
	};
}
