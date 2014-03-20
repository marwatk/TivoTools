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
package tivo.mfs.inode;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import tivo.io.Checksummable;
import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;
import tivo.view.Extent;

public class Inode implements Readable<Inode>, Writable, Checksummable {
	public static final int		SIZE			= (10 * Integer.SIZE/8) + (2 * Byte.SIZE/8) + (Short.SIZE/8) + (4 * Integer.SIZE/8);
	public static final int		DATABLOCK_SIZE	= (Long.SIZE/8) + (Integer.SIZE/8);
	public static final int		CRC_OFFSET		= SIZE - (3 * Integer.SIZE/8);
	public static final long	MFS_FSID_HASH	= 0x106D9L;
	public static final long	ROOT_FSID		= 1L;

	private static final int INODE_CHAINED	= 0x80000000;
	
	private long			fsid;			// This FSID 
	private long			refcount;		// References to this FSID 
	private long			unk1;
	private long			unk2;			// Seems to be related to last ?transaction? block used 
	private long			number;			// Should be *sectornum - 1122) / 2 
	private long			unk3;			// Also block size? 
	private long			size;			// In bytes or blocksize sized blocks 
	private long			blocksize;
	private long			blockused;
	private long			lastmodified;	// In seconds since epoch 
	private InodeType		type;			// For files not referenced by filesystem 
	private byte			unk6;			// Always 8? 
	private short			beef;			// Placeholder 
	private long			sig;			// Seems to be 0x91231ebc 
	private long			checksum;
	private long			flags;			// It seems to be flags at least. 
	private long			numblocks;		// Number of data blocks.  0 = in this sector
	private List<Extent>	datablocks; 

	private boolean validCrc = false;	
	private byte[]	inblockData;
	private long	id;

	
	public static long getBase( long fsid, long inodeCount ) {
		return (fsid * MFS_FSID_HASH) & (inodeCount - 1L);
	}
	
	public static long getNext( long inodeId, long inodeCount ) {
		return (inodeId + 1) & (inodeCount - 1L);
	}


	
	public int getReadAheadSize() {
		return SIZE;
	}

	public int getReadSize() {
		return 512 - SIZE; // + (int)numblocks * (2 * Integer.SIZE/8);
	}

	public Inode readData(DataInput in) throws Exception {
		if( datablocks != null ) {
			int toRead = getReadSize();
			if( numblocks > 0 ) {
				for( int i = 0; i < numblocks; i++ ) {
					datablocks.add( new Extent(
							in.readLong(),
							Utils.getUnsigned( in.readInt() )
						)
					);
					toRead -= DATABLOCK_SIZE;
				}
			}
			toRead = (toRead < 0) ? 0 : toRead;
			inblockData = new byte[ toRead ];
			in.readFully( inblockData );
		}
		else {
			fsid			= Utils.getUnsigned( in.readInt() );			
			refcount		= Utils.getUnsigned( in.readInt() );		
			unk1			= Utils.getUnsigned( in.readInt() );           
			unk2			= Utils.getUnsigned( in.readInt() );			
			number			= Utils.getUnsigned( in.readInt() );			
			unk3			= Utils.getUnsigned( in.readInt() );			
			size			= Utils.getUnsigned( in.readInt() );			
			blocksize		= Utils.getUnsigned( in.readInt() );      
			blockused		= Utils.getUnsigned( in.readInt() );      
			lastmodified	= Utils.getUnsigned( in.readInt() );	
			type			= InodeType.fromInt( in.readUnsignedByte() );
			unk6			= in.readByte();
			beef			= in.readShort();
			sig				= Utils.getUnsigned( in.readInt() );
			checksum		= Utils.getUnsigned( in.readInt() );
			flags			= Utils.getUnsigned( in.readInt() );
			numblocks		= Utils.getUnsigned( in.readInt() );

			datablocks = new ArrayList<Extent>( (int)numblocks );
		}

		return this;
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeInt	( (int)fsid			);			
		out.writeInt	( (int)refcount		);		
		out.writeInt	( (int)unk1			);           
		out.writeInt	( (int)unk2			);			
		out.writeInt	( (int)number		);			
		out.writeInt	( (int)unk3			);			
		out.writeInt	( (int)size			);			
		out.writeInt	( (int)blocksize	);      
		out.writeInt	( (int)blockused	);      
		out.writeInt	( (int)lastmodified	);	
		out.writeByte	( type.toInt()		);
		out.writeByte	( unk6				);
		out.writeShort	( beef				);
		out.writeInt	( (int)sig			);
		out.writeInt	( (int)checksum		);
		out.writeInt	( (int)flags		);
		out.writeInt	( (int)numblocks	);
		
		if( datablocks != null ) {
			for( Extent x : datablocks ) {
				out.writeLong( x.getStartBlock() );
				out.writeInt( (int)x.getLength() );
			}
		}
		if( inblockData != null )
			out.write( inblockData );
		
		return out;
	}

	public long getChecksum() {
		return checksum;
	}

	public int getChecksumOffset() {
		return CRC_OFFSET;
	}

	public boolean isValidCrc() {
		return validCrc;
	}

	public void setValidCrc(boolean validCrc) {
		this.validCrc = validCrc;
	}



	public long getFsid() {
		return fsid;
	}

	public void setFsid(long fsid) {
		this.fsid = fsid;
	}

	public long getRefcount() {
		return refcount;
	}

	public void setRefcount(long refcount) {
		this.refcount = refcount;
	}

	public long getUnk1() {
		return unk1;
	}

	public void setUnk1(long unk1) {
		this.unk1 = unk1;
	}

	public long getUnk2() {
		return unk2;
	}

	public void setUnk2(long unk2) {
		this.unk2 = unk2;
	}

	public long getNumber() {
		return number;
	}

	public void setNumber(long number) {
		this.number = number;
	}

	public long getUnk3() {
		return unk3;
	}

	public void setUnk3(long unk3) {
		this.unk3 = unk3;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getBlocksize() {
		return blocksize;
	}

	public void setBlocksize(long blocksize) {
		this.blocksize = blocksize;
	}

	public long getBlockused() {
		return blockused;
	}

	public void setBlockused(long blockused) {
		this.blockused = blockused;
	}

	public long getLastmodified() {
		return lastmodified;
	}

	public void setLastmodified(long lastmodified) {
		this.lastmodified = lastmodified;
	}

	public InodeType getType() {
		return type;
	}

	public void setType(InodeType type) {
		this.type = type;
	}

	public byte getUnk6() {
		return unk6;
	}

	public void setUnk6(byte unk6) {
		this.unk6 = unk6;
	}

	public short getBeef() {
		return beef;
	}

	public void setBeef(short beef) {
		this.beef = beef;
	}

	public long getSig() {
		return sig;
	}

	public void setSig(long sig) {
		this.sig = sig;
	}

	public long getFlags() {
		return flags;
	}

	public void setFlags(long flags) {
		this.flags = flags;
	}

	public long getNumblocks() {
		return numblocks;
	}

	public void setNumblocks(long numblocks) {
		this.numblocks = numblocks;
	}

	public List<Extent> getDatablocks() {
		return datablocks;
	}

	public void setDatablocks(List<Extent> datablocks) {
		this.datablocks = datablocks;
	}

	public void setChecksum(long checksum) {
		this.checksum = checksum;
	}
	
	public byte[] getInblockData() {
		return inblockData;
	}

	public void setInblockData( byte[] inblockData ) {
		this.inblockData = inblockData;
	}

	public long getId() {
		return id;
	}

	public void setId( long id ) {
		this.id = id;
	}

	public boolean isChained() {
		return (getFlags() & INODE_CHAINED) != 0;
	}

	public long getDataSizeBytes() {
		return getSize() * ((getBlocksize() == 0) ? 1L : getBlocksize());
	}
	
	public long getUsedSizeBytes() {
		return getBlockused() * ((getBlocksize() == 0) ? 1L : getBlocksize());
	}
	
	public boolean isDataInBlock() {
		return (getNumblocks() < 1) && (getDataSizeBytes() <= getReadSize());
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
									"fsid=%d\0"
								+	"refcount=%d\0"
								+	"unk1=%d\0"
								+	"unk2=%d\0"
								+	"number=%d\0"
								+	"unk3=%d\0"
								+	"size=%d\0"
								+	"blocksize=%d\0"
								+	"blockused=%d\0"
								+	"lastmodified=%d\0"
								+	"type='%s'\0"
								+	"unk6=%d\0"
								+	"beef=0x%04X\0"
								+	"sig=0x%08X\0"
								+	"checksum=%d\0"
								+	"flags=%d\0"
								+	"numblocks=%d\0"
								+	"datablocks=%s\0"
								+	"validCrc=%B\0"
								+	"id=%d"
	                    
									, fsid
									, refcount
									, unk1
									, unk2
									, number
									, unk3
									, size
									, blocksize
									, blockused
									, lastmodified
									, type
									, unk6
									, beef
									, sig
									, checksum
									, flags
									, numblocks
									, formatDataBlocks()
									, validCrc
									, id
					)
			)
			.append( '}' );
		return sb.toString();
	}

	private String formatDataBlocks() {
		StringBuffer sb = new StringBuffer();

		if( (datablocks == null) || datablocks.isEmpty() ) {
			if( size != 0 )
				sb.append( "{ in-block data }" );
			else
				sb.append( "{ null }" );
		}
		else {
			int i = 0;
			for( Extent x : datablocks ) {
				if( sb.length() > 0 )
					sb.append( "\0" );
				sb.append(  Utils.printf(
								"[%d] {\0"
							+	"\tstartBlock=%d\0"
							+	"\tlength=%d\0"
							+	"}"
								, i++
								, x.getStartBlock()
								, x.getLength()
							)
				);
			}
		}

		return sb.toString();
	}
}
