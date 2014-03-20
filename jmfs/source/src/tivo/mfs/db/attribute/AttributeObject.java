package tivo.mfs.db.attribute;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import tivo.io.Utils;
import tivo.mfs.db.Attribute;

public class AttributeObject extends Attribute<ObjectRef> {
	@Override
	public AttributeObject readData(DataInput in) throws Exception {
		if( getDataSize() < 0 )
			return (AttributeObject)super.readData(in);
		
		List<ObjectRef> v = new ArrayList<ObjectRef>();
		
		for( int len = super.getDataSize(); len > 0; ) {
			ObjectRef or = Utils.read(in, ObjectRef.class);
			v.add( or );
			len -= or.getReadAheadSize() + or.getReadSize();
		}
		
		value = v;
		
		readExtra( in );
		return this;
	}

	@Override
	public DataOutput write(DataOutput out) throws Exception {
		super.write( out );
		
		Utils.writeObjects( out, super.getReadSize(), value );
		writeExtra( out );
		
		return out;
	}
}
