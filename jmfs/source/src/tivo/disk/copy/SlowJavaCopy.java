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
package tivo.disk.copy;

import java.io.RandomAccessFile;

import tivo.io.Utils;

public class SlowJavaCopy extends JavaCommand {

	protected SlowJavaCopy() {}

	public SlowJavaCopy(String in, long inOffset, String out, long outOffset,
			long length) throws Exception {
		if( in != null ) {
			if( (out == null) || in.equalsIgnoreCase(out) ) { // "rw" is more than "r"
				out = in;
				in = null;
			}
			else
				this.in	= new RandomAccessFile( in, "r" );
		}
		else
		if( out == null )
			throw new Exception( "No input and output - copy is impossible" );
		
		if( out != null ) {
			this.out = new RandomAccessFile( out, "rw" );
			if( in == null )
				this.in = this.out;
		}
		
		this.inOffset	= Utils.blocksToBytes( inOffset + length );
		this.outOffset	= Utils.blocksToBytes( outOffset + length );
		this.length		= Utils.blocksToBytes( length );
		
		long maxBuf	= Math.min( length, Math.abs( inOffset - outOffset ) );
		super.buf	= super.allocateLargestBuffer( maxBuf, Utils.blocksToBytes(1) );
	}

	@Override
	public int copy() throws Exception {
		int	copyLen	= 0;
		
		if( length > 0 ) {
			copyLen	= (buf.length < length) ? buf.length : (int)length;
			inOffset -= copyLen;
			outOffset -= copyLen;
			
			in.seek( inOffset );
			in.readFully( buf, 0, copyLen );
			
			out.seek( outOffset );
			out.write( buf, 0, copyLen );
		}
		
		return copyLen;
	}
}
