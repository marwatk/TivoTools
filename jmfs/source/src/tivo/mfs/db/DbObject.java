package tivo.mfs.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;

public class DbObject extends  ValueObject<SubObject> implements Readable<DbObject>, Writable {
	private int		unk1;
	private long	length = -1; // int
	
	public int getReadAheadSize() {
		return SIZE;
	}
	public int getReadSize() {
		return (int)((length - SIZE) & Integer.MAX_VALUE);
	}
	public DbObject readData(DataInput in) throws Exception {
		if( length < 0 ) {
			unk1	= in.readInt();
			length	= Utils.getUnsigned( in.readInt() );
		}
		else {
			List<SubObject>	v = new ArrayList<SubObject>();
			for( long len = (length - SIZE); len > 0; ) {
				SubObject so = Utils.read(in, SubObject.class);
				v.add( so );
				len -= so.getReadAheadSize() + so.getReadSize();
			}
			value = v;
		}
		return this;	
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeInt	( unk1			);
		out.writeInt	( (int)length	);
		
		Utils.writeObjects( out, getReadSize(), value );
		
		return out;
	}
	
	
	
	public int getUnk1() {
		return unk1;
	}
	public void setUnk1(int unk1) {
		this.unk1 = unk1;
	}
	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}



	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"unk1=%d\0"
				+	"length=%d\0"
				+	"value=%s"
					, unk1
					, length
					, value
				)
			) 
			.append( '}' );
		return sb.toString();
	}




	private static final int SIZE = Integer.SIZE/8 + Integer.SIZE/8;
}
