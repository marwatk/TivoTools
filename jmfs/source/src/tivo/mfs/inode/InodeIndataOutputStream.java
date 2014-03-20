package tivo.mfs.inode;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

// through-write, byte-array backed stream.
// ByteArrayOutputStream won't work because it does not write into specific buffer
public class InodeIndataOutputStream extends OutputStream {
	private byte[]	buf;
	private int		bufPtr = 0;
	private byte[]	singleByteBuffer = new byte[1];
	
	public InodeIndataOutputStream( byte[] indata ) {
		this.buf = indata;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		int toWrite = Math.min( buf.length - bufPtr, len );
		System.arraycopy( b, off, buf, bufPtr, toWrite );
		bufPtr += toWrite;
		if( toWrite < len )
			throw new EOFException( "End of inode" );
	}
	




	





	@Override
	public void close() throws IOException {
		buf					= null;
		singleByteBuffer	= null;
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(int b) throws IOException {
		singleByteBuffer[0] = (byte)b;
		write( singleByteBuffer, 0, 1 );
	}

	@Override
	protected void finalize() throws Throwable {
		flush();
		close();
		super.finalize();
	}
}
