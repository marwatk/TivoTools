package tivo.mfs.db.attribute;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Arrays;

import tivo.io.Utils;
import tivo.mfs.db.Attribute;

public class AttributeString extends Attribute<String> {
	
	@Override
	public AttributeString readData(DataInput in) throws Exception {
		if( getDataSize() < 0 )
			return (AttributeString)super.readData(in);
		
		byte[] data = new byte[ super.getDataSize() ];
		in.readFully( data );
		value = Arrays.asList( Utils.decodeStrings( data ) );
		
		readExtra( in );
		return this;
	}

	@Override
	public DataOutput write(DataOutput out) throws Exception {
		super.write( out );
		
		byte[]	b;
		int		total = super.getReadSize(); // may be more than DataSize due to rounding
		
		for( String s : value ) {
			if( s == null )
				s = "";
			b = Utils.encodeString( s, null );
			if( b == null )
				b = new byte[0];
			out.write( b, 0, Math.min(b.length, total-1) );
			out.writeByte(0);
			total -= b.length + 1;
			if( total < 1 )
				break;
		}
		total -= writeExtra( out );
		Utils.writeZeroes( total, out ); // padding
		
		return out;
	}
}
