package uk.ac.ebi.ena.sra.cram.encoding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import uk.ac.ebi.ena.sra.cram.SequenceBaseProvider;
import uk.ac.ebi.ena.sra.cram.format.CramRecord;
import uk.ac.ebi.ena.sra.cram.format.ReadAnnotation;
import uk.ac.ebi.ena.sra.cram.format.ReadFeature;
import uk.ac.ebi.ena.sra.cram.format.ReadTag;
import uk.ac.ebi.ena.sra.cram.io.BitInputStream;
import uk.ac.ebi.ena.sra.cram.io.BitOutputStream;
import uk.ac.ebi.ena.sra.cram.io.NullBitOutputStream;

public class CramRecordCodec implements BitCodec<CramRecord> {
	public BitCodec<Long> inSeqPosCodec;
	public BitCodec<Long> recordsToNextFragmentCodec;
	public BitCodec<Long> readlengthCodec;
	public BitCodec<List<ReadFeature>> variationsCodec;
	public SequenceBaseProvider sequenceBaseProvider;
	// public BitCodec<byte[]> basesCodec;
	// public BitCodec<byte[]> qualitiesCodec;

	public BitCodec<Byte> baseCodec;
	public ByteArrayBitCodec qualityCodec;

	public String sequenceName;
	public long prevPosInSeq = 1L;
	public long defaultReadLength = 0L;

	public BitCodec<ReadAnnotation> readAnnoCodec;
	public BitCodec<Integer> readGroupCodec;
	public BitCodec<Long> nextFragmentIDCodec;

	public BitCodec<Byte> mappingQualityCodec;

	public boolean storeMappedQualityScores = false;
	public BitCodec<Byte> heapByteCodec;

	public Map<String, BitCodec<byte[]>> tagCodecMap;
	public BitCodec<String> tagKeyAndTypeCodec;

	public BitCodec<Byte> flagsCodec;

	private static Logger log = Logger.getLogger(CramRecordCodec.class);
	
	private static int debugRecordEndMarkerLen = 0 ;
	private static long debugRecordEndMarker = ~(-1 << debugRecordEndMarkerLen);

	@Override
	public CramRecord read(BitInputStream bis) throws IOException {
		long marker = bis.readLongBits(debugRecordEndMarkerLen) ;
		if (marker != debugRecordEndMarker) {
			throw new RuntimeException("Debug marker for beginning of record not found.") ;
		}
		
		CramRecord record = new CramRecord();

		byte b = flagsCodec.read(bis);
		record.setFlags(b);

		if (!record.isLastFragment()) {
			if (!record.detached) {
				record.setRecordsToNextFragment(recordsToNextFragmentCodec.read(bis));
			} else {
				CramRecord mate = new CramRecord();
				mate.setReadMapped(bis.readBit());
				mate.setNegativeStrand(bis.readBit());
				mate.setFirstInPair(bis.readBit());
				mate.setReadName(readZeroTerminatedString(heapByteCodec, bis));
				mate.setSequenceName(readZeroTerminatedString(heapByteCodec, bis));
				mate.setAlignmentStart(Long.valueOf(readZeroTerminatedString(heapByteCodec, bis)));
				record.insertSize = Integer.valueOf(readZeroTerminatedString(heapByteCodec, bis));

				mate.setFirstInPair(!record.isFirstInPair());
				if (record.isFirstInPair())
					record.next = mate;
				else
					record.previous = mate;

				record.setReadName(mate.getReadName());
			}
		}

		int readLen;
		if (bis.readBit())
			readLen = readlengthCodec.read(bis).intValue();
		else
			readLen = (int) defaultReadLength;
		record.setReadLength(readLen);

		if (record.isReadMapped()) {
			long position = prevPosInSeq + inSeqPosCodec.read(bis);
			prevPosInSeq = position;
			record.setAlignmentStart(position);

			boolean imperfectMatch = bis.readBit();
			if (imperfectMatch) {
				List<ReadFeature> features = variationsCodec.read(bis);
				record.setReadFeatures(features);
			}

			if (storeMappedQualityScores) {
				byte[] scores = qualityCodec.read(bis, readLen);
				// byte[] scores = new byte[readLen];
				// readNonEmptyByteArray(bis, scores, qualityCodec);
				record.setQualityScores(scores);
			}

			record.setMappingQuality(mappingQualityCodec.read(bis));
		} else {
			long position = prevPosInSeq + inSeqPosCodec.read(bis);
			prevPosInSeq = position;
			record.setAlignmentStart(position);

			byte[] bases = new byte[readLen];
			readNonEmptyByteArray(bis, bases, baseCodec);
			record.setReadBases(bases);

			byte[] scores = qualityCodec.read(bis, readLen);
			// byte[] scores = new byte[readLen];
			// readNonEmptyByteArray(bis, scores, qualityCodec);
			record.setQualityScores(scores);
		}

		record.setReadGroupID(readGroupCodec.read(bis));

		while (bis.readBit()) {
			if (record.tags == null)
				record.tags = new ArrayList<ReadTag>();
			String tagKeyAndType = tagKeyAndTypeCodec.read(bis);
			BitCodec<byte[]> codec = tagCodecMap.get(tagKeyAndType);
			byte[] valueBytes = codec.read(bis);
			char type = tagKeyAndType.charAt(3);
			Object value = ReadTag.restoreValueFromByteArray(type, valueBytes);

			ReadTag tag = new ReadTag(tagKeyAndType.substring(0, 2), type, value);
			record.tags.add(tag);
		}

		// if (bis.readBit()) {
		// List<ReadAnnotation> anns = new ArrayList<ReadAnnotation>();
		// do {
		// anns.add(readAnnoCodec.read(bis));
		// } while (bis.readBit());
		// if (!anns.isEmpty())
		// record.setAnnotations(anns);
		// }
		
		marker = bis.readLongBits(debugRecordEndMarkerLen) ;
		if (marker != debugRecordEndMarker) {
			System.out.println(record.toString());
			throw new RuntimeException("Debug marker for end of record not found.") ;
		}

		return record;
	}

	@Override
	public long write(BitOutputStream bos, CramRecord record) throws IOException {
		bos.write(debugRecordEndMarker, debugRecordEndMarkerLen) ;
		
		long len = 0L;

		len += flagsCodec.write(bos, record.getFlags());

		if (!record.isLastFragment()) {
			if (record.getRecordsToNextFragment() > 0) {
				len += recordsToNextFragmentCodec.write(bos, record.getRecordsToNextFragment());
			} else {

				CramRecord mate = record.next == null ? record.previous : record.next;
				bos.write(mate.isReadMapped());
				bos.write(mate.isNegativeStrand());
				bos.write(mate.isFirstInPair());
				len += writeZeroTerminatedString(record.getReadName(), heapByteCodec, bos);
				len += writeZeroTerminatedString(mate.getSequenceName(), heapByteCodec, bos);
				len += writeZeroTerminatedString(String.valueOf(mate.getAlignmentStart()), heapByteCodec, bos);
				len += writeZeroTerminatedString(String.valueOf(record.insertSize), heapByteCodec, bos);
			}
		}

		if (record.getReadLength() != defaultReadLength) {
			bos.write(true);
			len += readlengthCodec.write(bos, record.getReadLength());
		} else
			bos.write(false);
		len++;

		if (record.isReadMapped()) {
			if (record.getAlignmentStart() - prevPosInSeq < 0) {
				log.error("Negative relative position in sequence: prev=" + prevPosInSeq);
				log.error(record.toString());
			}
			len += inSeqPosCodec.write(bos, record.getAlignmentStart() - prevPosInSeq);
			prevPosInSeq = record.getAlignmentStart();

			List<ReadFeature> vars = record.getReadFeatures();
			if (vars == null || vars.isEmpty())
				bos.write(false);
			else {
				bos.write(true);
				len += variationsCodec.write(bos, vars);
			}
			len++;

			if (storeMappedQualityScores)
				// len += writeNonEmptyByteArray(bos, record.getQualityScores(),
				// qualityCodec);
				len += qualityCodec.write(bos, record.getQualityScores());

			mappingQualityCodec.write(bos, record.getMappingQuality());
		} else {
			if (record.getAlignmentStart() - prevPosInSeq < 0) {
				log.error("Negative relative position in sequence: prev=" + prevPosInSeq);
				log.error(record.toString());
			}
			len += inSeqPosCodec.write(bos, record.getAlignmentStart() - prevPosInSeq);
			prevPosInSeq = record.getAlignmentStart();

			len += writeNonEmptyByteArray(bos, record.getReadBases(), baseCodec);
//			len += writeNonEmptyByteArray(bos, record.getQualityScores(), qualityCodec);
			len += qualityCodec.write(bos, record.getQualityScores());
		}

		len += readGroupCodec.write(bos, record.getReadGroupID());

		if (record.tags != null && !record.tags.isEmpty()) {
			for (ReadTag tag : record.tags) {
				bos.write(true);
				len++;
				tagKeyAndTypeCodec.write(bos, tag.getKeyAndType());
				BitCodec<byte[]> codec = tagCodecMap.get(tag.getKeyAndType());
				long bits = codec.write(bos, tag.getValueAsByteArray());
				len += bits;
			}
		}
		bos.write(false);
		len++;

		// Collection<ReadAnnotation> annotations = record.getAnnotations();
		// if (annotations == null || annotations.isEmpty()) {
		// bos.write(false);
		// len++;
		// } else {
		// for (ReadAnnotation a : annotations) {
		// bos.write(true);
		// len++;
		// len += readAnnoCodec.write(bos, a);
		// }
		// bos.write(false);
		// len++;
		// }
		
		bos.write(debugRecordEndMarker, debugRecordEndMarkerLen) ;

		return len;
	}

	@Override
	public long numberOfBits(CramRecord record) {
		try {
			return write(NullBitOutputStream.INSTANCE, record);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static int writeNonEmptyByteArray(BitOutputStream bos, byte[] array, BitCodec<Byte> codec)
			throws IOException {
		if (array == null || array.length == 0)
			throw new RuntimeException("Expecting a non-empty array.");

		int len = 0;
		for (byte b : array)
			len += codec.write(bos, b);
		return len;
	}

	private static byte[] readNonEmptyByteArray(BitInputStream bis, byte[] array, BitCodec<Byte> codec)
			throws IOException {
		for (int i = 0; i < array.length; i++)
			array[i] = codec.read(bis);

		return array;
	}

	private static long writeZeroTerminatedString(String string, BitCodec<Byte> codec, BitOutputStream bos)
			throws IOException {
		long len = 0;
		for (byte b : string.getBytes()) {
			len += codec.write(bos, b);
		}

		len += codec.write(bos, (byte) 0);
		return len;
	}

	private static final int maxBufferSize = 1024;
	private static java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.allocate(maxBufferSize);

	private static String readZeroTerminatedString(BitCodec<Byte> codec, BitInputStream bis) throws IOException {
		byteBuffer.clear();
		for (int i = 0; i < maxBufferSize; i++) {
			byte b = codec.read(bis);
			if (b == 0)
				break;
			byteBuffer.put(b);
		}
		if (byteBuffer.position() >= maxBufferSize)
			throw new RuntimeException("Buffer overflow while reading string. ");

		byteBuffer.flip();
		byte[] bytes = new byte[byteBuffer.limit()];
		byteBuffer.get(bytes);
		return new String(bytes);
	}
}
