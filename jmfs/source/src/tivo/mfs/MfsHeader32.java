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
package tivo.mfs;

import java.io.DataInput;
import java.io.DataOutput;

import tivo.io.Utils;
import tivo.zone.ZoneHeader32;

public class MfsHeader32 extends MfsHeader {
	public static final int PARTITION_LIST_SIZE	= 128;
	public static final int SIZE				= (19 * Integer.SIZE/8) + PARTITION_LIST_SIZE + ZoneHeader32.SIZE;

	private int		off08;
	private int		rootFsId;		// Maybe?
	private int		off14;
	private int		off18;
	private int		off1c;
	private int		off20;
	private int		offa8;
	private int		offb8;
	private int		offbc;
	private int		offc0;
	private int		offd8;
	private int		offdc;
	private int		offe0;
	private int		offe4;
	
	@Override
	public int getReadSize() {
		return SIZE;
	}
	
	@Override
	protected int getPartitionListSize() {
		return PARTITION_LIST_SIZE;
	}

	private byte[] readPartitionList( DataInput in ) throws Exception {
		byte[] partitionList = new byte[ PARTITION_LIST_SIZE ];
		in.readFully( partitionList );
		return partitionList;
	}

    @Override
	public void setPartitionList(byte[] partitionList) {
    	partitionList = Utils.alignToSize( partitionList, PARTITION_LIST_SIZE );
    	partitionList[ PARTITION_LIST_SIZE-1 ] = 0; // must be null-terminated
		super.setPartitionList(partitionList);
	}
	
	@Override
	public MfsHeader32 readData( DataInput in ) throws Exception {
		super.readData( in ); // will skip or read if necessary
		
		off08			=	in.readInt();
		rootFsId		=	in.readInt();		// Maybe?
		off14			=	in.readInt();
		off18			=	in.readInt();
		off1c			=	in.readInt();
		off20			=	in.readInt();
		super.setPartitionList( readPartitionList( in ) );
		super.setTotalSectors(	Utils.getUnsigned( in.readInt() ) ); 
		offa8			=		in.readInt();
		super.setLogStart(	Utils.getUnsigned( in.readInt() ) );
		super.setLogSectors(in.readInt() );
		super.setLogStamp(	in.readInt() );
		offb8			=	in.readInt();
		offbc			=	in.readInt();
		offc0			=	in.readInt();
		super.setZoneMap(	new ZoneHeader32( in ) );
		offd8			=	in.readInt();
		offdc			=	in.readInt();
		offe0			=	in.readInt();
		offe4			=	in.readInt();

		return this;
	}


	public int getOff08() {
		return off08;
	}

	public void setOff08(int off08) {
		this.off08 = off08;
	}

	public int getRootFsId() {
		return rootFsId;
	}

	public void setRootFsId(int rootFsId) {
		this.rootFsId = rootFsId;
	}

	public int getOff14() {
		return off14;
	}

	public void setOff14(int off14) {
		this.off14 = off14;
	}

	public int getOff18() {
		return off18;
	}

	public void setOff18(int off18) {
		this.off18 = off18;
	}

	public int getOff1c() {
		return off1c;
	}

	public void setOff1c(int off1c) {
		this.off1c = off1c;
	}

	public int getOff20() {
		return off20;
	}

	public void setOff20(int off20) {
		this.off20 = off20;
	}

	public int getOffa8() {
		return offa8;
	}

	public void setOffa8(int offa8) {
		this.offa8 = offa8;
	}

	public int getOffb8() {
		return offb8;
	}

	public void setOffb8(int offb8) {
		this.offb8 = offb8;
	}

	public int getOffbc() {
		return offbc;
	}

	public void setOffbc(int offbc) {
		this.offbc = offbc;
	}

	public int getOffc0() {
		return offc0;
	}

	public void setOffc0(int offc0) {
		this.offc0 = offc0;
	}

	public int getOffd8() {
		return offd8;
	}

	public void setOffd8(int offd8) {
		this.offd8 = offd8;
	}

	public int getOffdc() {
		return offdc;
	}

	public void setOffdc(int offdc) {
		this.offdc = offdc;
	}

	public int getOffe0() {
		return offe0;
	}

	public void setOffe0(int offe0) {
		this.offe0 = offe0;
	}

	public int getOffe4() {
		return offe4;
	}

	public void setOffe4(int offe4) {
		this.offe4 = offe4;
	}

	@Override
	public DataOutput write(DataOutput out) throws Exception {
		super.write(out);
		
		out.writeInt( off08		);
		out.writeInt( rootFsId	);
		out.writeInt( off14		);
		out.writeInt( off18		);
		out.writeInt( off1c		);
		out.writeInt( off20		);
		out.write	( getPartitionList() );
		out.writeInt( (int)getTotalSectors()); 
		out.writeInt( offa8		);
		out.writeInt( (int)getLogStart()	);
		out.writeInt( getLogSectors()		);
		out.writeInt( getLogStamp()			);
		out.writeInt( offb8		);
		out.writeInt( offbc		);
		out.writeInt( offc0		);
		getZoneMap().write( out );
		out.writeInt( offd8		);
		out.writeInt( offdc		);
		out.writeInt( offe0		);
		out.writeInt( offe4		);
		
		return out;
	}
}
