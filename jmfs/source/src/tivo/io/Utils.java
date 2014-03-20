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
package tivo.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import tivo.disk.AppleDisk;
import tivo.disk.Storage;
import tivo.disk.TivoDisk;
import tivo.view.PhysicalAddress;
import tivo.view.View;

public class Utils {
	public static final int SIZEOF_INT = Integer.SIZE/8;
	
    public static class DataFormat {
    	private final ThreadLocal<String> add = new ThreadLocal<String>() {
			@Override
			protected String initialValue() {
				return "";
			}
    	};
    	
        private String  begin;
        private String  end;
        private String  delimiter;
        private String	prefix;

        public DataFormat( String begin, String end, String delimiter, String prefix ) {
            this.begin      = begin;
            this.end        = end;
            this.delimiter  = delimiter;
            this.prefix		= prefix;
        }

		public String getBegin() {
			return begin;
		}

		public String getEnd() {
			return end;
		}

		public String getDelimiter() {
			return delimiter;
		}

		public String getPrefix() {
			return prefix;
		}
    }
    
	private Utils() {}

	private static final Pattern	PRINTF_FORMAT	= Pattern.compile( "(?<=[^%]|^)(%)([^%\\s]+)", Pattern.DOTALL );
	private static final char[]		MULTIPLIERS		= { 'b', 'K', 'M', 'G', 'T' };
    private static final DataFormat FORMAT_FULL		= new DataFormat( "\n\t", "\n", "\n\t", "\t" );
    private static final DataFormat FORMAT_BRIEF	= new DataFormat( " ", "", ", ", "" );
    private static final DataFormat FORMAT_NULL		= new DataFormat( "", "", ", ", "" );

    private static DataFormat format = FORMAT_BRIEF;

    public static void setFormatFull() {
        format = FORMAT_FULL;
    }

    public static void setFormatBrief() {
        format = FORMAT_BRIEF;
    }

    public static DataFormat getFormat() {
        return format;
    }

	public static String formatSecondsToTime( long seconds ) {
		int sec = (int)seconds % 60;
		seconds /= 60;
		int min = (int)seconds % 60;
		seconds /= 60;
		int hour = (int)seconds % 24;
		int day = (int)seconds / 24;
		
		return	((day > 0) ? String.format( "%1$d days ", day ) : "")
			+	String.format( "%1$02d:%2$02d:%3$02d", hour, min, sec );
	}
	
	public static String formatSizeHuman( long size ) {
		double	value = size;
		char	mult = '\0';
		
		for( int i = 0; i < MULTIPLIERS.length; i++ ) {
			mult = MULTIPLIERS[i];
			if( i > 0 )
				value /= 1024;
			if( value < 999 )
				break;
		}
		
		return String.format( "%1$.02f%2$c", value, mult );
	}

	public static long getUnsigned( int value ) {
		return ((long)value) & 0xFFFFFFFFL;
	}
	
	public static void writeZeroes( int size, DataOutput out ) throws Exception {
		byte[] zeroes = new byte[size];
		out.write( zeroes );
	}
	
	public static long blocksToBytes( int blocks ) {
		return blocksToBytes( getUnsigned(blocks) );
	}
	
	public static long blocksToBytes( long blocks ) {
		return blocks * AppleDisk.BLOCK_SIZE;
	}
	
	public static long bytesToBlocks( long bytes ) {
		return bytes / AppleDisk.BLOCK_SIZE;
	}

	public static long blocksToBytes( PhysicalAddress blocks ) {
		return (blocks == null) ? -1 : blocksToBytes( blocks.getAddress() );
	}
	
	public static long calculateCrc( byte[] buf, int checksumOffset ) {
		Crc crc = new Crc();

		crc.update( buf, 0, checksumOffset );
		crc.update( Checksummable.MAGIC );
		crc.update( buf, checksumOffset + Checksummable.CHECKSUM_SIZE );

		return crc.getValue();
	}

	private static <T extends Readable<T>> T read( DataInput in, T r, byte[] buf, int offset, int size ) throws Exception {
		T t = r;
		
		if( size > 0 ) {
			if( in != null ) // may be data already in the buffer?
				in.readFully( buf, offset, size );
			
			MemoryCacheImageInputStream bin = new MemoryCacheImageInputStream(new ByteArrayInputStream( buf, offset, size ));
			bin.setByteOrder( TivoDisk.BYTE_ORDER );
			t = r.readData(bin);
		}
		
		return t;
	}
	
	private static <T extends Readable<T>, K extends T> T read( DataInput in, byte[] buf, int offset, K k ) throws Exception {
		T r = k;
		int readAheadSize = r.getReadAheadSize();
		if( buf == null ) {
			buf = new byte[ readAheadSize ];
			offset = 0;
		}
		r = read( in, r, buf, offset, readAheadSize );
		
		int readSize = r.getReadSize();
		if( in != null ) { // data already in the provided buffer, so don't read again
			byte[] copyBuff = new byte[ readAheadSize + readSize ];
			if( readAheadSize > 0 )
				System.arraycopy( buf, 0, copyBuff, 0, readAheadSize );
			buf = copyBuff;
		}	
		T t = read( in, r, buf, offset+readAheadSize, readSize );
		if( t instanceof Checksummable ) {
			Checksummable c = (Checksummable)t;
			c.setValidCrc( calculateCrc(buf, c.getChecksumOffset()) == c.getChecksum() );
		}
		
		return t;
	}
	
	public static <T extends Readable<T>, K extends T> T read( DataInput in, K k ) throws Exception {
		return read( in, null, 0, (T)k );
	}
	
	public static <T extends Readable<T>, K extends T> T read( byte[] buf, int offset, K k ) throws Exception {
		return read( null, buf, offset, (T)k );
	}
	
	public static <T extends Readable<T>> T read( DataInput in, Class<? extends T> cls ) throws Exception {
		return read( in, null, 0, (T)cls.newInstance() );
	}
	
	public static <T extends Readable<T>> T read( byte[] buf, int offset, Class<? extends T> cls ) throws Exception {
		return read( null, buf, offset, (T)cls.newInstance() );
	}
	
	public static <T extends Writable> byte[] write( DataOutput out, T w ) throws Exception {
		byte[] buf = write( w );
		out.write( buf );
		return buf;
	}

	public static <T extends Writable> byte[] write( T w ) throws Exception {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		MemoryCacheImageOutputStream bout = new MemoryCacheImageOutputStream(b);
		bout.setByteOrder( TivoDisk.BYTE_ORDER );
			
		w.write(bout);
		bout.flush();
		byte[] buf = b.toByteArray();
		
		if( w instanceof Checksummable ) {
			Checksummable c = (Checksummable)w;
			long crc = Utils.calculateCrc( buf, c.getChecksumOffset() );
			c.setChecksum( crc );
			b.reset();
			w.write(bout);
			bout.flush();
			buf = b.toByteArray();
		}

		bout.close();

		return buf;
	}

	public static <T extends Writable> void writeObjects( DataOutput out, int exactDataSize, Iterable<T> objs )
			throws Exception {
		if( objs != null ) {
			for( T t : objs ) {
				byte[] b = Utils.write( t );
				if( exactDataSize < b.length )
					break;
				out.write( b );
				exactDataSize -= b.length;
			}
		}
//		Utils.writeZeroes( exactDataSize, out ); // padding
	}
	
	public static PrintStream log( String fmt, Object... args ) {
		PrintStream p = printf( System.out, fmt, args );
		p.flush();
		return p;
	}
	
	public static PrintStream log( JavaLog log, String fmt, Object... args ) {
		PrintStream	p	= System.out;
		String		msg	= printf( fmt, args );
		
		p.print( msg );
		p.flush();
		if( log != null )
			log.info( msg );
			
		return p;
	}
	
	public static PrintStream printf( PrintStream out, String fmt, Object... args ) {
		out.print( format( false, fmt, args ) );
		return out;
	}
	
	/*
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					""
					, null
				)
			) 
			.append( " }" );
		return sb.toString();
	 */
	public static String printf( String fmt, Object... args ) {
		return format( true, fmt, args );
	}

	public static String format( String fmt, Object... args ) {
		return format( FORMAT_NULL, true, fmt, args );
	}

	public static String format( boolean appendBegin, String fmt, Object... args ) {
		return format( format, appendBegin, fmt, args );
	}

	public static String format( DataFormat format, boolean appendBegin, String fmt, Object... args ) {
		String prefix = format.add.get();
		format.add.set( format.prefix + prefix );
		try {
			fmt = fmt.replace( "\0", format.delimiter + prefix );
			Matcher m = PRINTF_FORMAT.matcher( fmt );
			StringBuffer sb = new StringBuffer( fmt );
	
			int i = 0;
			int off = 0;
			int last = 0;
	
			if( args != null ) {
				while( (i < args.length) && m.find() ) {
					if( args[i] instanceof Collection )
						args[i] = formatCollection( format, appendBegin, (Collection<?>)args[i] );
					i++;
					int len = sb.length();
					int start = m.start()+off+1;
					last = m.end();
					sb	.insert( start, '$' )
						.insert( start, i );
					off += sb.length() - len;
				}
			}
			
			String subfmt = (appendBegin ? format.begin : "" ) + prefix + sb.substring( 0, last+off );
			return String.format( subfmt, args ) + fmt.substring( last ) + format.end + prefix;
		}
		finally {
			format.add.set( prefix );
		}
	}
	
	private static String formatCollection( DataFormat format, boolean appendBegin, Collection<?> c ) {
		StringBuffer	sb	= new StringBuffer();
		Object[]		os	= new Object[c.size()];
		
		int i = 0;
		for( Object o : c ) {
			if( sb.length() > 0 )
				sb.append( '\0' );
			sb.append( "%s" );
			os[i++] = o;
		}
		
		return "[" + format( format, appendBegin, sb.toString(), os ) + "]";
	}
	
	public static byte[] alignToSize( byte[] buf, int size ) {
		if( buf == null )
			buf = new byte[0];
		byte[] result = buf;
		if( buf.length != size ) {
			result = new byte[ size ];
			System.arraycopy( buf, 0, result, 0, Math.min( size, buf.length ) );
		}
		return result;
	}
	
	public static String dump( byte[] buf ) {
		return (buf == null) ? "" : dump( buf, 0, buf.length );
	}

	public static String dump( byte[] buf, int off, int len ) {
		if( buf == null )
			return "";

		final int		GROUP_WIDTH			= 8;
		final int		GROUPS				= 2;
		final String	SEPARATOR_OFFSET	= "\t";
		final String	SEPARATOR_GROUPS	= "  ";
		final String	SEPARATOR_BYTES		= " ";
		final String	SEPARATOR_CHARS		= "|";
		final char		UNPRINTABLE_CHAR	= '.';
		final String	EMPTY_BYTE			= "  ";
		
		int maxLen = Math.min( buf.length, off + len );
		StringBuffer sb = new StringBuffer();
		
		char[] chars = new char[ GROUP_WIDTH * GROUPS ];
		for( int i = off; i < maxLen;) {
			sb.append( format( "%08X", i ) );
			Arrays.fill( chars, UNPRINTABLE_CHAR );
			for( int g = 0, c = 0; g < GROUPS; g++ ) {
				sb.append( (g == 0) ? SEPARATOR_OFFSET : SEPARATOR_GROUPS );
				for( int j = 0; j < GROUP_WIDTH; j++, i++, c++ ) {
					if( i >= maxLen )
						sb.append( EMPTY_BYTE );
					else {
						sb.append( format( "%02X", buf[i] ));
						if( buf[i] >= 0x20 )
							chars[c] = (char)((int)buf[i] & 0xFF);
					}
					sb.append( SEPARATOR_BYTES );
				}
			}
			sb.append( SEPARATOR_GROUPS ).append( SEPARATOR_CHARS ).append( chars ).append( SEPARATOR_CHARS ).append( '\n' );
		}
		
		return sb.toString();
	} 

	// convert bytes to string without exception
	public static String decodeString( byte[] stringBytes ) {
		return decodeString( stringBytes, 0, (stringBytes == null) ? 0 : stringBytes.length );
	}

	public static String[] decodeStrings( byte[] stringBytes ) {
		return decodeStrings( stringBytes, 0, (stringBytes == null) ? 0 : stringBytes.length );
	}

	public static String decodeString( byte[] stringBytes, int off, int len ) {
		String[] s = decodeStrings( stringBytes, off, len );
		return (s == null) ? null : s[0].trim();
	}
	
	public static String[] decodeStrings( byte[] stringBytes, int off, int len ) {
		String result = null;
		try {
			result = new String( stringBytes, off, len, "ASCII" );
		}
		catch( java.io.UnsupportedEncodingException e ) {
			result = new String( stringBytes, off, len );
		}
		return (result == null) ? null : result.trim().split( "\0" ); /*.replace("\0", ""); */
	}

	// convert bytes to string without exception
	public static byte[] encodeString( String s, Integer size ) {
		byte[] result = null;
		try {
			result = s.getBytes( "ASCII" );
		}
		catch( java.io.UnsupportedEncodingException e ) {
			result = s.getBytes();
		}
		return (size == null) ? result : alignToSize( result, size );
	}
	
	public static PhysicalAddress getValidPhysicalAddress( View view, long logicalBlock ) throws IOException {
		PhysicalAddress		address	= view.toPhysical( logicalBlock );
		if( (address == null) || (address.getStorage() == null) )
			throw new IOException( "Logical block is located beyond the current storage: block=" + logicalBlock + ", view=" + view );
		if( address.getStorage().getImg() == null )
			throw new IOException( "Physical storage for block=" + logicalBlock + " (physical=" + address.getAddress() + ") is invalid" );
		return address;
	}
	
	public static RandomAccessFile seekBlock( PhysicalAddress address ) throws IOException {
		return seekBlock( address.getStorage(), address.getAddress() );
	}
	
	public static RandomAccessFile seekBlock( Storage storage, long block ) throws IOException {
		RandomAccessFile img = (storage == null) ? null : storage.getImg();
		if( img == null )
			throw new EOFException( "File not found - storage has not been initialized" );
		
		img.seek( blocksToBytes(block) );
		return img;
	}
	
	public static RandomAccessFile seekToLogicalBlock( View v, long logicalBlock ) throws IOException {
		return seekBlock( getValidPhysicalAddress( v, logicalBlock ) );
	}
	
	public static long roundUp( long size, long roundBoundary ) {
		return ((size + roundBoundary - 1) / roundBoundary) * roundBoundary;
	}
	
	public static long roundDown( long size, long roundBoundary ) {
		return (size - (size % roundBoundary));
	}
	
	public static void printException( String prefix, Exception e ) {
		String message = (e == null) ? null : e.getMessage();
		if( (message == null) && (e != null) )
			message = e.getClass().getName();
		System.err.println( prefix + message );
		System.err.flush();
	}
}

