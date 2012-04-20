package uk.ac.ebi.ena.sra.cram.io;

import java.io.IOException;

public interface BitInputStream {

	public boolean readBit() throws IOException;

	public int readBits(int len) throws IOException;

	public long readLongBits(int len) throws IOException;
	
	public boolean endOfStream () throws IOException ;
	
	public boolean putBack(long b, int numBits) ;
}
