package tivo.mfs.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;

public class SubObject extends ValueObject<Attribute<?>> implements Readable<SubObject>, Writable, TypedObject {
	private int 	length = -1;	// short
	private int 	lengthDup;		// short
	private int 	type;			// short
	private int 	flags;			// short
	private short	unk1;
	private short	unk2;
	private long 	id;				// int
	
	public int getReadAheadSize() {
		return SIZE;
	}
	public int getReadSize() {
		return (length - SIZE);
	}
	public SubObject readData(DataInput in) throws Exception {
		if( length < 0 ) {
			length		= in.readUnsignedShort();
			lengthDup	= in.readUnsignedShort();
			type		= in.readUnsignedShort();
			flags		= in.readUnsignedShort();
			unk1		= in.readShort();
			unk2		= in.readShort();
			id			= Utils.getUnsigned( in.readInt() );
		}
		else {
			List<Attribute<?>>	v = new ArrayList<Attribute<?>>();
			for( int len = (length - SIZE); len > 0; ) {
				Attribute<?> a = Attribute.getInstance(in);
				a = Utils.read(in, a);
				a.setParentId( type );
				v.add( a );
				len -= a.getTotalSize(); // includes data and header
			}
			value = v;
		}
		return this;
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeShort	( length	);
		out.writeShort	( lengthDup	);
		out.writeShort	( type		);
		out.writeShort	( flags		);
		out.writeShort	( unk1		);
		out.writeShort	( unk2		);
		out.writeInt	( (int)id	);
		
		Utils.writeObjects( out, getReadSize(), value );
		
		return out;
	}



	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public int getLengthDup() {
		return lengthDup;
	}
	public void setLengthDup(int lengthDup) {
		this.lengthDup = lengthDup;
	}
	public int getType() {
		return type;
	}
	public String getTypeName() {
		return Schema.toString(type);
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getFlags() {
		return flags;
	}
	public void setFlags(int flags) {
		this.flags = flags;
	}
	public short getUnk1() {
		return unk1;
	}
	public void setUnk1(short unk1) {
		this.unk1 = unk1;
	}
	public short getUnk2() {
		return unk2;
	}
	public void setUnk2(short unk2) {
		this.unk2 = unk2;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}



	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"type='%s'\0"
				+	"flags=0x%04X\0"
				+	"id=%d\0"
				+	"value=%s"
					, Schema.toString(type)
					, flags
					, id
					, value
				)
			) 
			.append( '}' );
		return sb.toString();
	}
	
	public String toStringFull() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"length=%d\0"
				+	"lengthDup=%d\0"
				+	"type=%d (%s)\0"
				+	"flags=0x%04X\0"
				+	"unk1=%d\0"
				+	"unk2=%d\0"
				+	"id=%d\0"
				+	"value=%s"
					, length
					, lengthDup
					, type, Schema.toString(type)
					, flags
					, unk1
					, unk2
					, id
					, value
				)
			) 
			.append( '}' );
		return sb.toString();
	}



	private static final int SIZE = (Short.SIZE/8) * 6 + Integer.SIZE/8;
}
