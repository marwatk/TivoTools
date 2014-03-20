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
import tivo.zone.ZoneHeader64;

public class MfsHeader64 extends MfsHeader {
	public static final int PARTITION_LIST_SIZE  = 132;
	public static final int SIZE                 = (16 * Integer.SIZE/8) + (4 * Long.SIZE/8) + PARTITION_LIST_SIZE + ZoneHeader64.SIZE;

    private int     fill1;
    private int     fill2;
    private int     fill3;
    private int     fill4;
    private int     fill5;
    private int     fill6;
    private int     zoneMapType;
    private long    unkStart;
    private long    fill7;
    private int     unkSize;
    private int     fill8;
    private int     fill9;
    private int     fill10;
    private int     fill11;
    private int     fill12;
    private int     pad;

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
    public MfsHeader64 readData( DataInput in ) throws Exception {
		super.readData( in ); // will skip or read if necessary
		
        fill1       =  in.readInt();
        fill2       =  in.readInt();
        fill3       =  in.readInt();
        fill4       =  in.readInt();
        fill5       =  in.readInt();
        fill6       =  in.readInt();
        super.setPartitionList( readPartitionList( in ) );
        super.setTotalSectors(  in.readLong() );
        super.setLogStart(   in.readLong() );
        zoneMapType =  in.readInt();
        super.setLogStamp(   in.readInt() );
        unkStart    =  in.readLong();
        fill7       =  in.readLong();
        super.setZoneMap( new ZoneHeader64( in ) );
        unkSize     =  in.readInt();
        super.setLogSectors(in.readInt() );
        fill8       =  in.readInt();
        fill9       =  in.readInt();
        fill10      =  in.readInt();
        fill11      =  in.readInt();
        fill12      =  in.readInt();
        pad         =  in.readInt();

		return this;
    }

    @Override
	public void setPartitionList(byte[] partitionList) {
    	partitionList = Utils.alignToSize( partitionList, PARTITION_LIST_SIZE );
    	partitionList[ PARTITION_LIST_SIZE-1 ] = 0; // must be null-terminated
		super.setPartitionList(partitionList);
	}

	public int getFill1() {
        return fill1;
    }

    public void setFill1(int fill1) {
        this.fill1 = fill1;
    }

    public int getFill2() {
        return fill2;
    }

    public void setFill2(int fill2) {
        this.fill2 = fill2;
    }

    public int getFill3() {
        return fill3;
    }

    public void setFill3(int fill3) {
        this.fill3 = fill3;
    }

    public int getFill4() {
        return fill4;
    }

    public void setFill4(int fill4) {
        this.fill4 = fill4;
    }

    public int getFill5() {
        return fill5;
    }

    public void setFill5(int fill5) {
        this.fill5 = fill5;
    }

    public int getFill6() {
        return fill6;
    }

    public void setFill6(int fill6) {
        this.fill6 = fill6;
    }

    public int getZoneMapType() {
        return zoneMapType;
    }

    public void setZoneMapType(int zoneMapType) {
        this.zoneMapType = zoneMapType;
    }

    public long getUnkStart() {
        return unkStart;
    }

    public void setUnkStart(long unkStart) {
        this.unkStart = unkStart;
    }

    public long getFill7() {
        return fill7;
    }

    public void setFill7(long fill7) {
        this.fill7 = fill7;
    }

    public int getUnkSize() {
        return unkSize;
    }

    public void setUnkSize(int unkSize) {
        this.unkSize = unkSize;
    }

    public int getFill8() {
        return fill8;
    }

    public void setFill8(int fill8) {
        this.fill8 = fill8;
    }

    public int getFill9() {
        return fill9;
    }

    public void setFill9(int fill9) {
        this.fill9 = fill9;
    }

    public int getFill10() {
        return fill10;
    }

    public void setFill10(int fill10) {
        this.fill10 = fill10;
    }

    public int getFill11() {
        return fill11;
    }

    public void setFill11(int fill11) {
        this.fill11 = fill11;
    }

    public int getFill12() {
        return fill12;
    }

    public void setFill12(int fill12) {
        this.fill12 = fill12;
    }

    public int getPad() {
        return pad;
    }

    public void setPad(int pad) {
        this.pad = pad;
    }

   @Override
   public DataOutput write(DataOutput out) throws Exception {
      super.write(out);

      out.writeInt	( fill1     );
      out.writeInt	( fill2     );
      out.writeInt	( fill3     );
      out.writeInt	( fill4     );
      out.writeInt	( fill5     );
      out.writeInt	( fill6     );
      out.write		( getPartitionList() );
      out.writeLong	( getTotalSectors() );
      out.writeLong	( getLogStart() );
      out.writeInt	( zoneMapType  );
      out.writeInt	( getLogStamp() );
      out.writeLong	( unkStart     );
      out.writeLong	( fill7        );
      getZoneMap().write( out );
      out.writeInt	( unkSize      );
      out.writeInt	( getLogSectors() );
      out.writeInt	( fill8     );
      out.writeInt	( fill9     );
      out.writeInt	( fill10 );
      out.writeInt	( fill11 );
      out.writeInt	( fill12 );
      out.writeInt	( pad    );
      
      return out;
   }
}
