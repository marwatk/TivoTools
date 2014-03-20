package tivo.mfs.db.attribute;

import java.io.DataInput;
import java.io.DataOutput;

import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;

public class ObjectRef implements Readable<ObjectRef>, Writable {
	private int	fsid;
	private int	type;
	
	public int getReadAheadSize() {
		return 0;
	}
	public int getReadSize() {
		return SIZE;
	}
	public ObjectRef readData(DataInput in) throws Exception {
		fsid = in.readInt();
		type = in.readInt();
		
		return this;
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeInt( fsid );
		out.writeInt( type );
		
		return out;
	}






	
	public int getFsid() {
		return fsid;
	}
	public void setFsid(int fsid) {
		this.fsid = fsid;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"fsid=%d\0"
				+	"type=%d"
					, fsid
					, type
				)
			) 
			.append( '}' );
		return sb.toString();
	}


	
	private static final int SIZE = Integer.SIZE/8 + Integer.SIZE/8;
}
