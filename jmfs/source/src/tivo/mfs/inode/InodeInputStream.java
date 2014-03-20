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
import java.io.InputStream;
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
public class InodeInputStream extends InputStream {
	private byte[]				singleByteBuffer = new byte[1];
	private View				view;
	private Iterator<Extent>	dataBlocks;
	private Extent				currentBlock = null; // pointer to the current extent
	private long				blockIdx = 0; // block index in the current extent
	private int					byteCount = 0; // count how many bytes left in the current block - will be reset to AppleDisk.BLOCK_SIZE on first write 
	private RandomAccessFile	in = null;
	
	public InodeInputStream( View view, List<Extent> datablocks ) throws IOException {
		this( view, datablocks, 0 );
	}
	
	public InodeInputStream( View view, List<Extent> datablocks, int offset ) throws IOException {
		this.view		= view;
		this.dataBlocks	= datablocks.iterator();
		skip( offset );
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if( in == null )
			return -1;
		
		int total = off;
		int n = 0;
		
		while( (in != null) && (byteCount < len) ) {
			n = in.read(b, off, (int)byteCount );
			len -= n;
			off += n;
			byteCount -= n;
			if( byteCount < 1 )
				in = seekNextDatablock();
			n = 0;
		}
		
		if( (in != null) && (len > 0) ) {
			n = in.read(b, off, len);
			byteCount -= n;
		}
		
		total = (off - total) + n;
		
		return total;
	}
	
	@Override
	public long skip( long len ) throws IOException {
		long total = 0;
		
		while( byteCount <= len ) { // '=' is for 0 and 512
			len -= byteCount;
			total += byteCount;
			in = seekNextDatablock();
		}
		if( (len > 0) && (in != null) )
			len = in.skipBytes( (int)len );
		byteCount -= len;
		
		return total;
	}

	
	
	
	
	
	
	
	
	
	@Override
	public int read(byte[] b) throws IOException {
		return (b == null) ? 0 : read(b, 0, b.length);
	}

	@Override
	public int read() throws IOException {
		while( read( singleByteBuffer, 0, 1 ) < 1 );
		return ((int)singleByteBuffer[0]) & 0xFF;
	}

	@Override
	protected void finalize() throws Throwable {
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
