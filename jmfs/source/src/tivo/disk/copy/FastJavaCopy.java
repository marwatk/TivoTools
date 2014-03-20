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

public class FastJavaCopy extends JavaCommand {

	protected FastJavaCopy() {}

	protected FastJavaCopy(String in, long inOffset, String out, long outOffset,
			long length) throws Exception {
		if(out == null)
			out = in;
		if( out.equalsIgnoreCase(in) && ((inOffset + length) > outOffset) )
			throw new Exception( "Can not use " + getClass().getSimpleName() + " if source and target overlap" );
				
		this.in			= new RandomAccessFile( in, "r" );
		this.out		= new RandomAccessFile( out, "rw" );
		this.inOffset	= Utils.blocksToBytes( inOffset );
		this.outOffset	= Utils.blocksToBytes( outOffset );
		this.length		= Utils.blocksToBytes( length );
		
		super.buf = super.allocateLargestBuffer( length, Utils.blocksToBytes(1) );
		
		this.in.seek( inOffset );
		this.out.seek( outOffset );
	}

	@Override
	protected int copy() throws Exception {
		int	copyLen	= 0;
		
		if( length > 0 ) {
			copyLen	= (buf.length < length) ? buf.length : (int)length;
			in.readFully( buf, 0, copyLen );
			out.write( buf, 0, copyLen );
		}
		
		return copyLen;
	}
}
