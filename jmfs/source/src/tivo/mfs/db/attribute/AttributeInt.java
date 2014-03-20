package tivo.mfs.db.attribute;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import tivo.io.Utils;
import tivo.mfs.db.Attribute;

public class AttributeInt extends Attribute<Integer> {
	@Override
	public AttributeInt readData(DataInput in) throws Exception {
		if( getDataSize() < 0 )
			return (AttributeInt)super.readData(in);
		
		List<Integer> v = new ArrayList<Integer>();
		
		for( int len = super.getDataSize(); len > 0; len -= Utils.SIZEOF_INT )
			v.add( in.readInt() );
		
		value = v;
		
		readExtra( in );
		return this;
	}

	@Override
	public DataOutput write(DataOutput out) throws Exception {
		super.write( out );
		
		int	total = super.getReadSize(); // may be more than DataSize due to rounding
		for( Integer i : value ) {
			if( i == null )
				continue;
				
			out.writeInt( i );
			
			total -= Utils.SIZEOF_INT;
			if( total < Utils.SIZEOF_INT )
				break;
		}
		total -= writeExtra( out );
		Utils.writeZeroes( total, out ); // padding
		
		return out;
	}
}
