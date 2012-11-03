package net.sf.cram.encoding;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

import net.sf.cram.CramRecord;
import net.sf.cram.DataSeriesType;
import net.sf.cram.EncodingKey;
import net.sf.cram.ReadTag;
import net.sf.cram.encoding.read_features.BaseChange;
import net.sf.cram.encoding.read_features.BaseQualityScore;
import net.sf.cram.encoding.read_features.DeletionVariation;
import net.sf.cram.encoding.read_features.InsertBase;
import net.sf.cram.encoding.read_features.InsertionVariation;
import net.sf.cram.encoding.read_features.ReadBase;
import net.sf.cram.encoding.read_features.ReadFeature;
import net.sf.cram.encoding.read_features.SoftClipVariation;
import net.sf.cram.encoding.read_features.SubstitutionVariation;

public class Reader {
	public Charset charset = Charset.forName("UTF8");
	public boolean captureMappedQS = false;
	public boolean captureUnmappedQS = false;
	public boolean captureReadNames = false;

	@DataSeries(key = EncodingKey.BF_BitFlags, type = DataSeriesType.INT)
	public DataReader<Integer> bitFlagsC;

	@DataSeries(key = EncodingKey.RL_ReadLength, type = DataSeriesType.INT)
	public DataReader<Integer> readLengthC;

	@DataSeries(key = EncodingKey.AP_AlignmentPositionOffset, type = DataSeriesType.INT)
	public DataReader<Integer> alStartC;

	@DataSeries(key = EncodingKey.RG_ReadGroup, type = DataSeriesType.INT)
	public DataReader<Integer> readGroupC;

	@DataSeries(key = EncodingKey.RN_ReadName, type = DataSeriesType.BYTE_ARRAY)
	public DataReader<byte[]> readNameC;

	@DataSeries(key = EncodingKey.NF_RecordsToNextFragment, type = DataSeriesType.INT)
	public DataReader<Integer> distanceC;

	@DataSeries(key = EncodingKey.TC_TagCount, type = DataSeriesType.BYTE)
	public DataReader<Byte> tagCountC;

	@DataSeries(key = EncodingKey.TN_TagNameAndType, type = DataSeriesType.BYTE_ARRAY)
	public DataReader<byte[]> tagNameAndTypeC;

	@DataSeriesMap(name = "TAG")
	public Map<String, DataReader<byte[]>> tagValueCodecs;

	@DataSeries(key = EncodingKey.FN_NumberOfReadFeatures, type = DataSeriesType.INT)
	public DataReader<Integer> nfc;

	@DataSeries(key = EncodingKey.FP_FeaturePosition, type = DataSeriesType.INT)
	public DataReader<Integer> fp;

	@DataSeries(key = EncodingKey.FC_FeatureCode, type = DataSeriesType.BYTE)
	public DataReader<Byte> fc;

	@DataSeries(key = EncodingKey.BA_Base, type = DataSeriesType.BYTE)
	public DataReader<Byte> bc;

	@DataSeries(key = EncodingKey.QS_QualityScore, type = DataSeriesType.BYTE)
	public DataReader<Byte> qc;

	@DataSeries(key = EncodingKey.BS_BaseSubstitutionCode, type = DataSeriesType.BYTE)
	public DataReader<Byte> bsc;

	@DataSeries(key = EncodingKey.IN_Insertion, type = DataSeriesType.BYTE_ARRAY)
	public DataReader<byte[]> inc;

	@DataSeries(key = EncodingKey.DL_DeletionLength, type = DataSeriesType.INT)
	public DataReader<Integer> dlc;

	@DataSeries(key = EncodingKey.MQ_MappingQualityScore, type = DataSeriesType.BYTE)
	public DataReader<Byte> mqc;

	@DataSeries(key = EncodingKey.MF_MateBitFlags, type = DataSeriesType.BYTE)
	public DataReader<Byte> mbfc;

	@DataSeries(key = EncodingKey.NS_NextFragmentReferenceSequenceID, type = DataSeriesType.INT)
	public DataReader<Integer> mrc;

	@DataSeries(key = EncodingKey.NP_NextFragmentAlignmentStart, type = DataSeriesType.INT)
	public DataReader<Integer> malsc;

	@DataSeries(key = EncodingKey.TS_InsetSize, type = DataSeriesType.INT)
	public DataReader<Integer> tsc;

	public void read(CramRecord r) throws IOException {
		r.setFlags(bitFlagsC.readData());
		r.setReadLength(readLengthC.readData());
		r.alignmentStartOffsetFromPreviousRecord = alStartC.readData();
		r.setReadGroupID(readGroupC.readData());

		if (captureReadNames) {
			r.setReadName(new String(readNameC.readData(), charset));
		}

		// mate record:
		if (!r.isLastFragment()) {
			if (r.detached) {
				r.setMateFlags(mbfc.readData());
				if (!captureReadNames)
					r.setReadName(new String(readNameC.readData(), charset));

				r.mateSequnceID = mrc.readData();
				r.mateAlignmentStart = malsc.readData();
				r.templateSize = tsc.readData();
			} else
				r.setRecordsToNextFragment(distanceC.readData());

		}

		// tag records:
		if (r.tags != null) {
			int tagCount = tagCountC.readData();
			r.tags = new ArrayList<>();
			for (int i = 0; i < tagCount; i++) {
				byte[] name = tagNameAndTypeC.readData();
				String tagId = new String(new byte[] { name[0], name[1], ':',
						name[2] }, charset);
				DataReader<byte[]> dataReader = tagValueCodecs.get(tagId);
				ReadTag tag = ReadTag.deriveTypeFromKeyAndType(
						tagId,
						ReadTag.deriveTypeFromKeyAndType(tagId,
								dataReader.readData()));
				r.tags.add(tag);
			}
		}

		if (r.isReadMapped()) {
			// writing read features:
			java.util.List<ReadFeature> rf = new ArrayList<>();
			r.setReadFeatures(rf);
			int size = nfc.readData();
			int prevPos = 0;
			for (int i = 0; i < size; i++) {
				Byte operator = fc.readData();

				int pos = prevPos + fp.readData();
				prevPos = pos;

				switch (operator) {
				case ReadBase.operator:
					ReadBase rb = new ReadBase(pos, bc.readData(),
							qc.readData());
					rf.add(rb);
					break;
				case SubstitutionVariation.operator:
					SubstitutionVariation sv = new SubstitutionVariation();
					sv.setPosition(pos);
					sv.setBaseChange(new BaseChange(bsc.readData()));
					rf.add(sv);
					break;
				case InsertionVariation.operator:
					InsertionVariation iv = new InsertionVariation(pos,
							inc.readData());
					rf.add(iv);
					break;
				case SoftClipVariation.operator:
					SoftClipVariation fv = new SoftClipVariation(pos,
							inc.readData());
					rf.add(fv);
					break;
				case DeletionVariation.operator:
					DeletionVariation dv = new DeletionVariation(pos,
							dlc.readData());
					rf.add(dv);
					break;
				case InsertBase.operator:
					InsertBase ib = new InsertBase(pos, bc.readData());
					rf.add(ib);
					break;
				case BaseQualityScore.operator:
					BaseQualityScore bqs = new BaseQualityScore(pos,
							qc.readData());
					rf.add(bqs);
					break;
				default:
					throw new RuntimeException(
							"Unknown read feature operator: " + operator);
				}
			}

			// mapping quality:
			r.setMappingQuality(mqc.readData());
			if (r.forcePreserveQualityScores) {
				byte[] qs = new byte[r.getReadLength()];
				for (int i = 0; i < qs.length; i++)
					qs[i] = qc.readData();
				r.setQualityScores(qs);
			}
		} else {
			byte[] bases = new byte[r.getReadLength()];
			for (int i = 0; i < bases.length; i++)
				bases[i] = bc.readData();
			r.setReadBases(bases);

			if (r.forcePreserveQualityScores) {
				byte[] qs = new byte[r.getReadLength()];
				for (int i = 0; i < qs.length; i++)
					qs[i] = qc.readData();
				r.setQualityScores(qs);
			}
		}
	}
}