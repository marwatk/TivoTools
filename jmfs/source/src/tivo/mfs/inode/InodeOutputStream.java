/* 
	Java MFS (jmfs) - Copyright (C) 2010 Artem Erchov
	Contact: comer0@gmail.com 

	This file is part of jmfs.

	jmfs is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	jmfs is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package tivo.mfs.inode;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;

import tivo.disk.AppleDisk;
import tivo.io.Utils;
import tivo.view.Extent;
import tivo.view.View;

/*	thread-UNsafe.
	if underlying storage is manipulated outside of the stream
	while it's open, the result is unpredictable (i.e. garbage).
*/
public class InodeOutputStream extends OutputStream {
	private byte[]				singleByteBuffer = new byte[1];
	private View				view;
	private Iterator<Extent>	dataBlocks;
	private Extent				currentBlock = null; // pointer to the current extent
	private long				blockIdx = 0; // block index in the current extent
	private int					byteCount = 0; // count how many bytes left in the current block - will be reset to AppleDisk.BLOCK_SIZE on first write 
	private RandomAccessFile	out = null;
	
	public InodeOutputStream( View view, List<Extent> datablocks ) throws IOException {
		this( view, datablocks, 0 );
	}
	
	public InodeOutputStream( View view, List<Extent> datablocks, int offset ) throws IOException {
		this.view		= view;
		this.dataBlocks	= datablocks.iterator();
		
		skip( offset );
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		while( byteCount < len ) {
			out.write(b, off, (int)byteCount );
			len -= byteCount;
			off += byteCount;
			out = seekNextDatablock();
		}
		
		out.write(b, off, len);
		byteCount -= len;
	}
	
	public long skip( long len ) throws IOException {
		long total = 0;
		
		while( byteCount <= len ) { // '=' is for 0 and 512
			len -= byteCount;
			total += byteCount;
			out = seekNextDatablock();
		}
		if( len > 0 )
			out.seek( out.getFilePointer() + len );
		byteCount -= len;
		
		return total;
	}

	
	
	
	
	
	@Override
	public void close() throws IOException {
		singleByteBuffer	= null;
		view				= null;
		dataBlocks			= null;
		currentBlock		= null;
		out					= null;
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
	
	
	
	
	private RandomAccessFile seekNextDatablock() throws IOException {
		blockIdx++;
		while( (currentBlock == null) || (blockIdx >= currentBlock.getLength()) ) { // who knows, may be there are 0-length blocks?
			if( !dataBlocks.hasNext() )
				throw new EOFException( "End of inode" );
			currentBlock = dataBlocks.next();
			blockIdx = 0;
		}
		/*	every block needs to be seeked because what appears
			to be consecutive logical blocks may be disjointed
			physical blocks on real disk device/file.
		*/
		byteCount = AppleDisk.BLOCK_SIZE;
		return Utils.seekToLogicalBlock( view, currentBlock.getStartBlock()+blockIdx );
	}
}
