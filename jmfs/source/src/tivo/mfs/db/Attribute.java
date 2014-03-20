package tivo.mfs.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;
import tivo.mfs.db.attribute.AttributeFile;
import tivo.mfs.db.attribute.AttributeInt;
import tivo.mfs.db.attribute.AttributeObject;
import tivo.mfs.db.attribute.AttributeString;

public abstract class Attribute<T> extends ValueObject<T> implements Readable<Attribute<T>>, Writable, TypedObject {
	private	Type	valueType;
	private	int		type;
	private int		valueLength;
	
	private		int		dataSize = -1;
	private		int		parentId;
	protected	byte[]	extra;
	
	public static enum Type {
		INT			(0),
		CONSTANT	(1),
		STRING		(2),
		MULTISTRING	(3),
		OBJECT		(4),
		MULTIOBJECT	(5),
		MULTIFILE	(6),
		FILE		(7),
		;
		
		public static Type fromInt( int value ) {
			for( Type t : Type.values() ) {
				if( t.value == value )
					return t;
			}
			throw new IllegalArgumentException( "Unknown type '" + value + "'" );
		}
		

		public int toInt() { return value; }
		
		private Type( int value ) { this.value = value; }
		private int value;
	}


	public static Attribute<?> getInstance( DataInput in ) throws Exception {
		Type			vType	= Type.fromInt( in.readUnsignedByte() >> 5 );
		Attribute<?>	res		= null;
		
		switch( vType ) {
			case MULTIFILE:
			case FILE:
				res = new AttributeFile();
				break;
			case CONSTANT:
			case INT:
				res = new AttributeInt();
				break;
			case STRING:
			case MULTISTRING:
				res = new AttributeString();
				break;
			case OBJECT:
			case MULTIOBJECT:
				res = new AttributeObject();
				break;
			default:
				throw new IllegalArgumentException( "Unknown type '" + vType + "'" );
		}
		
		res.valueType = vType;
		
		return res;			
	}
	
	protected Attribute() {}
	
	public Attribute( int parentId ) {
		this.parentId = parentId;
	}

	public int getReadAheadSize() {
		return SIZE-(valueType == null ? 0 : 1); // 1 byte was hopefully read at 'getInstance' 
	}
	
	public int getReadSize() {
		return (int)(Utils.roundUp( valueLength, 4 ) - SIZE);
	}
	
	public int getTotalSize() {
		return getReadSize() + SIZE;
	}
	
	public Attribute<T> readData(DataInput in) throws Exception {
		type		= in.readUnsignedByte();
		valueLength	= in.readUnsignedShort();
			
		dataSize	= valueLength - SIZE;
			
		return this;
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeByte	( valueType.toInt()<<5	);
		out.writeByte	( type					);
		out.writeShort	( valueLength			);
		
		return out;
	}
	
	public Type getValueType() {
		return valueType;
	}

	public void setValueType(Type valueType) {
		this.valueType = valueType;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public String getTypeName() {
		return Schema.toString( parentId, type );
	}

	public int getValueLength() {
		return valueLength;
	}

	public void setValueLength(int valueLength) {
		this.valueLength = valueLength;
	}
	public int getParentId() {
		return parentId;
	}

	public void setParentId(int parentId) {
		this.parentId = parentId;
	}







	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"type='%s'\0"
				+	"value=%s"
					, Schema.toString( parentId, type )
					, getValueForPrint()
				)
			) 
			.append( '}' );
		return sb.toString();
	}
	
	public String toStringFull() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"valueType='%s'\0"
				+	"type=%d ('%s')\0"
				+	"valueLength=%d\0"
				+	"value=%s"
					, valueType
					, type, Schema.toString( parentId, type )
					, valueLength
					, getValueForPrint()
				)
			) 
			.append( '}' );
		return sb.toString();
	}











	protected int writeExtra( DataOutput out ) throws IOException {
		if( extra != null ) {
			out.write(extra);
			return extra.length;
		}
		return 0;
	}

	protected byte[] readExtra( DataInput in ) throws IOException {
		int extraSize = getReadSize() - getDataSize();
		if( extraSize < 1 )
			extraSize  =0;
		extra = new byte[ extraSize ];
		in.readFully( extra );
		
		return extra;
	} 

	protected int getDataSize() {
		return dataSize;
	}

	private List<?> getValueForPrint() {
		if( !(this instanceof AttributeInt) )
			return super.getValue();
			
		List<Integer>	v = ((AttributeInt)this).getValue();
		List<String>	p = new ArrayList<String>();
		
		for( Integer i : v ) {
			String s = DbEnum.getEnum( parentId, type, i );
			p.add( (s == null) ? String.valueOf(i) : s );
		}
		
		return p;
	}

	private static final int SIZE = Byte.SIZE/8 + Byte.SIZE/8 + Short.SIZE/8;
}
