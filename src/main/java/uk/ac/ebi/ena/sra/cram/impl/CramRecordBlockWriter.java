package uk.ac.ebi.ena.sra.cram.impl;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import uk.ac.ebi.ena.sra.cram.format.ByteFrequencies;
import uk.ac.ebi.ena.sra.cram.format.CramCompression;
import uk.ac.ebi.ena.sra.cram.format.CramRecordBlock;
import uk.ac.ebi.ena.sra.cram.format.DiByteFrequencies;
import uk.ac.ebi.ena.sra.cram.format.Encoding;
import uk.ac.ebi.ena.sra.cram.format.IntFrequencies;

class CramRecordBlockWriter {
	private OutputStream delegate;

	public CramRecordBlockWriter(OutputStream delegate) {
		super();
		this.delegate = delegate;
	}

	public long write(CramRecordBlock block) throws IOException {
		DataOutputStream dos = new DataOutputStream(delegate);

		dos.write("BLOCKBEGIN".getBytes());

		dos.writeUTF(block.getSequenceName());
		dos.writeInt(block.getSequenceLength());

		dos.writeLong(block.getFirstRecordPosition());
		dos.writeLong(block.getRecordCount());
		dos.writeInt(block.getReadLength());
		dos.writeBoolean(block.isPositiveStrandBasePositionReversed());
		dos.writeBoolean(block.isNegativeStrandBasePositionReversed());
		dos.writeBoolean(block.isUnmappedReadQualityScoresIncluded());
		dos.writeBoolean(block.isSubstitutionQualityScoresIncluded());
		dos.writeBoolean(block.isMaskedQualityScoresIncluded());
		dos.writeBoolean(block.losslessQualityScores);

		dos.write("COMPRESSIONBEGIN".getBytes());
		writeCramCompression(dos, block.getCompression());
		dos.write("BLOCKEND".getBytes());
		dos.flush();
		return dos.size() * 8;
	}

	private static void writeArray(DataOutputStream dos, int[] array) throws IOException {
		dos.writeInt(array.length);
		for (int i = 0; i < array.length; i++)
			dos.writeInt(array[i]);
	}

	private static void writeArray(DataOutputStream dos, byte[] array) throws IOException {
		dos.writeInt(array.length);
		for (int i = 0; i < array.length; i++)
			dos.writeByte(array[i]);
	}

	private static final void writeCramCompression(DataOutputStream os, CramCompression compression) throws IOException {

		writeEncoding(os, compression.getInSeqPosEncoding());
		writeEncoding(os, compression.getInReadPosEncoding());
		writeEncoding(os, compression.getReadLengthEncoding());
		writeEncoding(os, compression.getDelLengthEncoding());
		writeEncoding(os, compression.getRecordsToNextFragmentEncoding());

		writeArray(os, compression.getBaseAlphabet());
		writeArray(os, compression.getBaseFrequencies());

		writeArray(os, compression.getScoreAlphabet());
		writeArray(os, compression.getScoreFrequencies());

		writeArray(os, compression.getStopBaseAlphabet());
		writeArray(os, compression.getStopBaseFrequencies());

		writeArray(os, compression.getStopScoreAlphabet());
		writeArray(os, compression.getStopScoreFrequencies());

		writeArray(os, compression.getReadFeatureAlphabet());
		writeArray(os, compression.getReadFeatureFrequencies());

		writeArray(os, compression.getReadLengthAlphabet());
		writeArray(os, compression.getReadLengthFrequencies());

		writeArray(os, compression.getReadAnnotationIndexes());
		writeArray(os, compression.getReadAnnotationFrequencies());

		writeArray(os, compression.getReadGroupIndexes());
		writeArray(os, compression.getReadGroupFrequencies());

		writeArray(os, compression.getMappingQualityAlphabet());
		writeArray(os, compression.getMappingQualityFrequencies());

		writeArray(os, compression.getHeapByteAlphabet());
		writeArray(os, compression.getHeapByteFrequencies());

		os.writeInt(compression.tagKeyAlphabet.length);
		for (int i = 0; i < compression.tagKeyAlphabet.length; i++) {
			String tagKey = compression.tagKeyAlphabet[i];
			// always 4 bytes, for example 'MD:Z'!
			os.write(tagKey.getBytes());
			os.writeInt(compression.tagKeyFrequency[i]);

			ByteFrequencies byteFrequencies = compression.tagByteFrequencyMap.get(tagKey);
			byte[] alphabet = byteFrequencies.getValues();
			writeArray(os, alphabet);
			int[] freqs = byteFrequencies.getFrequencies();
			writeArray(os, freqs);

			IntFrequencies byteLengths = compression.tagByteLengthMap.get(tagKey);
			writeArray(os, byteLengths.getValues());
			writeArray(os, byteLengths.getFrequencies());
		}

		byte[] alphabet = compression.flagStats.getValues();
		writeArray(os, alphabet);
		int[] freqs = compression.flagStats.getFrequencies();
		writeArray(os, freqs);
		
//		write (os, compression.score2) ;
	}

	private static final void writeEncoding(DataOutputStream os, Encoding encoding) throws IOException {
		os.writeByte(encoding.getAlgorithm().ordinal());
		os.writeUTF(encoding.getParameters());
	}
	
	private static final void write(DataOutputStream os, DiByteFrequencies f) throws IOException {
		byte[][] values = f.getValues() ;
		int[] freqs = f.getFrequencies() ;
		
		os.writeInt(values.length) ;
		for (int i=0; i<values.length; i++) {
			os.write(values[i]) ;
			os.writeInt(freqs[i]) ;
		}
	}
}
