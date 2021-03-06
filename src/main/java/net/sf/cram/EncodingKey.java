package net.sf.cram;

public enum EncodingKey {
	BF_BitFlags, AP_AlignmentPositionOffset, FP_FeaturePosition, 
	FC_FeatureCode, QS_QualityScore, DL_DeletionLength, BA_Base, TN_TagNameAndType, 
	NF_RecordsToNextFragment, RL_ReadLength, RG_ReadGroup, MQ_MappingQualityScore, 
	RN_ReadName, NP_NextFragmentAlignmentStart, TS_InsetSize, FN_NumberOfReadFeatures, 
	BS_BaseSubstitutionCode, IN_Insertion, TC_TagCount, MF_MateBitFlags, 
	NS_NextFragmentReferenceSequenceID, CF_CompressionBitFlags, TM_TestMark, TV_TagValue;

	public static final EncodingKey byFirstTwoChars(String chars) {
		for (EncodingKey k : values()) {
			if (k.name().startsWith(chars))
				return k;
		}
		return null;
	}
	
	public static final byte[] toTwoBytes (EncodingKey key) {
		return new byte[]{(byte)key.name().charAt(0), (byte)key.name().charAt(1)} ;
	}
}
