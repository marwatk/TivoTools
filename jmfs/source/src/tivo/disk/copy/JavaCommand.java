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


public abstract class JavaCommand extends CopyCommand {
	protected abstract int copy() throws Exception;
	
	public static byte[] allocateLargestBuffer( long maxSize, long minSize ) {
		System.runFinalization();
		System.gc();
	
		int		maxLength	= (int)	Math.min(	Runtime.getRuntime().freeMemory(),
									Math.min(	maxSize,
												Integer.MAX_VALUE
									));
		int		size		= maxLength;
		int		minLength	= (int)minSize;
		int		nextTarget	= minLength;
		byte[]	b			= null;
		byte[]	minBuffer	= new byte[ minLength ];
		
		while( Math.abs(size - nextTarget) > 1 ) {
			b = null;
			System.runFinalization();
			System.gc();
			try { b = new byte[ size ]; } catch( Throwable t ) {}
			if( b == null ) {
				nextTarget = minLength;
				maxLength = size;
			}
			else {
				nextTarget = maxLength;
				minLength = size;
			}
		}
		
		return (b == null) ? minBuffer : b;
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}


	@Override
	protected int execute() throws Exception {
		Utils.log( "Executing %s: inOffset=%d, outOffset=%d, length=%d\n", getClass().getSimpleName(), 
				Utils.bytesToBlocks( inOffset ), Utils.bytesToBlocks( outOffset ), Utils.bytesToBlocks( length ) );
		
		long time = System.currentTimeMillis();
		long copied = 0;
		double rate = 100d/length;
		
		while( length > 0 ) {
			int	copyLen	= copy();
			length -= copyLen;
			copied += copyLen;
			
			long elapsed = System.currentTimeMillis() - time;
			long speed = (elapsed > 0) ? (copied*1000)/elapsed : 0;
			
			Utils.log( "Copy progress: %.01f%c speed %s/s. Estimated time left %s                \r",
						copied*rate, '%',
						Utils.formatSizeHuman(speed), Utils.formatSizeHuman(length), Utils.formatSecondsToTime(length/speed) );
		}
		Utils.log("\n");
		
		return 0;
	}





	protected RandomAccessFile	in;
	protected RandomAccessFile	out;
	protected long				inOffset;
	protected long				outOffset;
	protected long				length;
	protected byte[]			buf;
	
	private void close() {
		try { if( in != null ) in.close(); in = null; } catch( Throwable t ) {} 
		try { if( out != null ) out.close(); out = null; } catch( Throwable t ) {} 
	}
}
