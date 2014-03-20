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
package tivo.disk;

import java.io.DataInput;
import java.io.DataOutput;

import tivo.io.Utils;
import tivo.io.Writable;


public class PartitionEntry implements Writable, Cloneable {
	protected static final int	NAME_SIZE		= 32;
	protected static final int	TYPE_SIZE		= 32;
	protected static final int	PROCESSOR_SIZE	= 16;
	protected static final int	PAD_SIZE		= 188 * Short.SIZE/8;

	public static final String	MFS_TYPE					= "MFS";
	public static final int		APPLE_PARTITION_SIGNATURE	= 0x504D;

	public static final int LOGICAL_SIZE_OFFSET = 2*Short.SIZE/8 + 4*Integer.SIZE/8 + NAME_SIZE + TYPE_SIZE;
	public static final int LOGICAL_SIZE_SIZE	= Integer.SIZE/8;
	public static final int SIZE_OFFSET 		= 2*Short.SIZE/8 + 2*Integer.SIZE/8;
	public static final int SIZE_SIZE			= Integer.SIZE/8;

	public static enum PartitionStatus {
		Valid				( "Invalid" 		),
		Allocated			( "Free"			),
		Busy				( "NotInUse"		),
		Boot				( "NotBoot"			),
		Readable			( "Non-readable"	),
		Writable			( "Non-writable"	),
		BootNonPositional	( "PositionalBoot"	)
		;

		private String unsetName;

		private PartitionStatus( String unsetName ) { this.unsetName = unsetName; }

		public int getMask() {
			return 1 << ordinal();
		}
		public String getUnsetName() {
			return unsetName;
		}		
	}
	
	
	public PartitionEntry( DataInput in ) throws Exception {
		/*
			TYPE Partition = 
			RECORD
				000: pmSig:         Integer;       {partition signature}
				002: pmSigPad:      Integer;       {reserved}
				004: pmMapBlkCnt:   LongInt;       {number of blocks in partition map}
				008: pmPyPartStart: LongInt;       {first physical block of partition}
				012: pmPartBlkCnt:  LongInt;       {number of blocks in partition}
				016: pmPartName:    PACKED ARRAY [0..31] OF Char; {partition name}
				048: pmParType:     PACKED ARRAY [0..31] OF Char; {partition type}
				080: pmLgDataStart: LongInt;       {first logical block of data area}
				084: pmDataCnt:     LongInt;       {number of blocks in data area}
				088: pmPartStatus:  LongInt;       {partition status information - used in A/UX only}
				092: pmLgBootStart: LongInt;       {first logical block of boot code}
				096: pmBootSize:    LongInt;       {size of boot code, in bytes}
				100: pmBootAddr:    LongInt;       {boot code load address}
				104: pmBootAddr2:   LongInt;       {reserved}
				108: pmBootEntry:   LongInt;       {boot code entry point}
				112: pmBootEntry2:  LongInt;       {reserved}
				116: pmBootCksum:   LongInt;       {boot code checksum}
				120: pmProcessor:   PACKED ARRAY [0..15] OF Char; {processor type}
				136: pmPad:         ARRAY [0..187] OF Integer;    {reserved} (188*2)
				512
			END;
		*/
		signature					=	in.readUnsignedShort();
		signaturePad				=	in.readUnsignedShort();
		partitionMapSizeBlocks		=	in.readInt();
		startBlock					=	Utils.getUnsigned( in.readInt() );
		sizeBlocks					=	Utils.getUnsigned( in.readInt() );
										in.readFully( nameBytes );
										in.readFully( typeBytes );
		logicalStartBlock			=	Utils.getUnsigned( in.readInt() );
		logicalSizeBlocks			=	Utils.getUnsigned( in.readInt() );
		status						=	in.readInt();
		bootStartBlock				=	in.readInt();
		bootSizeBytes				=	in.readInt();
		bootLoadAddress				=	in.readInt();
		bootLoadAddress2			=	in.readInt();
		bootEntryAddress			=	in.readInt();
		bootEntryAddress2			=	in.readInt();
		bootCRC						=	in.readInt();
										in.readFully( processorBytes );
										in.readFully( pad );
	}
	
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public int getSignature() {
		return signature;
	}

	public void setSignature(int signature) {
		this.signature = signature;
	}

	public int getSignaturePad() {
		return signaturePad;
	}

	public void setSignaturePad(int signaturePad) {
		this.signaturePad = signaturePad;
	}

	public int getPartitionMapSizeBlocks() {
		return partitionMapSizeBlocks;
	}

	public void setPartitionMapSizeBlocks(int partitionMapSizeBlocks) {
		this.partitionMapSizeBlocks = partitionMapSizeBlocks;
	}

	public long getStartBlock() {
		return startBlock;
	}

	public void setStartBlock(long startBlock) {
		this.startBlock = startBlock;
	}

	public long getSizeBlocks() {
		return sizeBlocks;
	}

	public void setSizeBlocks(long sizeBlocks) {
		this.sizeBlocks = sizeBlocks;
	}

	public byte[] getNameBytes() {
		return nameBytes;
	}

	public void setNameBytes(byte[] nameBytes) {
		this.nameBytes = Utils.alignToSize( nameBytes, NAME_SIZE );
		this.nameBytes[ NAME_SIZE-1 ] = 0; // must be null-terminated
		name = null; // mark it 'dirty'
	}

	public String getName() {
		if( (name == null) && (nameBytes != null ) )
			name = Utils.decodeString( nameBytes );
		return name;
	}

	public void setName(String name) {
		setNameBytes( Utils.encodeString( name, NAME_SIZE ) );
	}

	public byte[] getTypeBytes() {
		return typeBytes;
	}

	public void setTypeBytes(byte[] typeBytes) {
		this.typeBytes = Utils.alignToSize( typeBytes, TYPE_SIZE );
		this.typeBytes[ TYPE_SIZE-1 ] = 0; // must be null-terminated
		type = null;
	}

	public String getType() {
		if( (type == null) && (typeBytes != null ) )
			type = Utils.decodeString( typeBytes );
		return type;
	}

	public void setType(String type) {
		setTypeBytes( Utils.encodeString( type, TYPE_SIZE ) );
	}

	public long getLogicalStartBlock() {
		return logicalStartBlock;
	}

	public void setLogicalStartBlock(long logicalStartBlock) {
		this.logicalStartBlock = logicalStartBlock;
	}

	public long getLogicalSizeBlocks() {
		return logicalSizeBlocks;
	}

	public void setLogicalSizeBlocks(long logicalSizeBlocks) {
		this.logicalSizeBlocks = logicalSizeBlocks;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getBootStartBlock() {
		return bootStartBlock;
	}

	public void setBootStartBlock(int bootStartBlock) {
		this.bootStartBlock = bootStartBlock;
	}

	public int getBootSizeBytes() {
		return bootSizeBytes;
	}

	public void setBootSizeBytes(int bootSizeBytes) {
		this.bootSizeBytes = bootSizeBytes;
	}

	public int getBootLoadAddress() {
		return bootLoadAddress;
	}

	public void setBootLoadAddress(int bootLoadAddress) {
		this.bootLoadAddress = bootLoadAddress;
	}

	public int getBootLoadAddress2() {
		return bootLoadAddress2;
	}

	public void setBootLoadAddress2(int bootLoadAddress2) {
		this.bootLoadAddress2 = bootLoadAddress2;
	}

	public int getBootEntryAddress() {
		return bootEntryAddress;
	}

	public void setBootEntryAddress(int bootEntryAddress) {
		this.bootEntryAddress = bootEntryAddress;
	}

	public int getBootEntryAddress2() {
		return bootEntryAddress2;
	}

	public void setBootEntryAddress2(int bootEntryAddress2) {
		this.bootEntryAddress2 = bootEntryAddress2;
	}

	public int getBootCRC() {
		return bootCRC;
	}

	public void setBootCRC(int bootCRC) {
		this.bootCRC = bootCRC;
	}

	public byte[] getProcessorBytes() {
		return processorBytes;
	}

	public void setProcessorBytes(byte[] processorBytes) {
		this.processorBytes = Utils.alignToSize( processorBytes, PROCESSOR_SIZE );
		this.processorBytes[ PROCESSOR_SIZE-1 ] = 0; // must be null-terminated
		processor = null;
	}

	public String getProcessor() {
		if( (processor == null) && (processorBytes != null) )
			processor = Utils.decodeString( processorBytes );
		return processor;
	}

	public void setProcessor(String processor) {
		setProcessorBytes( Utils.encodeString( processor, PROCESSOR_SIZE ) );
	}

	public byte[] getPad() {
		return pad;
	}

	public void setPad(byte[] pad) {
		this.pad = Utils.alignToSize( pad, PAD_SIZE );
	}

	public boolean isSignatureValid() {
		return signature == APPLE_PARTITION_SIGNATURE;
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeShort	( signature );
		out.writeShort	( signaturePad );
		out.writeInt	( partitionMapSizeBlocks );
		out.writeInt	( (int)startBlock );
		out.writeInt	( (int)sizeBlocks );
		out.write		( nameBytes );
		out.write		( typeBytes );
		out.writeInt	( (int)logicalStartBlock );
		out.writeInt	( (int)logicalSizeBlocks );
		out.writeInt	( status );
		out.writeInt	( bootStartBlock );
		out.writeInt	( bootSizeBytes );
		out.writeInt	( bootLoadAddress );
		out.writeInt	( bootLoadAddress2 );
		out.writeInt	( bootEntryAddress );
		out.writeInt	( bootEntryAddress2 );
		out.writeInt	( bootCRC );
		out.write		( processorBytes );
		out.write		( pad );
										
		return out;
	}
	
	public PartitionEntry clone() {
		PartitionEntry pe = new PartitionEntry();

		pe.signature 				= signature;
		pe.signaturePad				= signaturePad;
		pe.partitionMapSizeBlocks	= partitionMapSizeBlocks;
		pe.startBlock				= startBlock;
		pe.sizeBlocks				= sizeBlocks;
		pe.nameBytes				= nameBytes.clone();
		pe.typeBytes				= typeBytes.clone();
		pe.logicalStartBlock		= logicalStartBlock;
		pe.logicalSizeBlocks		= logicalSizeBlocks;
		pe.status					= status;
		pe.bootStartBlock			= bootStartBlock;
		pe.bootSizeBytes			= bootSizeBytes;
		pe.bootLoadAddress			= bootLoadAddress;
		pe.bootLoadAddress2			= bootLoadAddress2;
		pe.bootEntryAddress			= bootEntryAddress;
		pe.bootEntryAddress2		= bootEntryAddress2;
		pe.bootCRC					= bootCRC;
		pe.processorBytes			= processorBytes.clone();
		pe.pad						= pad.clone();

		return pe;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
							"number=%d\0"
						+	"signature=0x%04X\0"
						+	"signaturePad=0x%04X\0"
						+	"partitionMapSizeBlocks=%d (bytes %d)\0"
						+	"startBlock=%d (byte offset 0x%08X)\0"
						+	"sizeBlocks=%d (bytes %,d, next offset 0x%08X)\0"			
						+	"name='%s'\0"
						+	"type='%s'\0"
						+	"logicalStartBlock=%d (physical %d, logical offset 0x%08X, physical offset 0x%08X)\0"	
						+	"logicalSizeBlocks=%d (bytes %d)\0"
						+	"status='%s'\0"
						+	"bootStartBlock=%d (physical %d, logical offset 0x%08X, physical offset 0x%08X)\0"	
						+	"bootSizeBytes=%d\0"
						+	"bootLoadAddress=%08X:%08X\0"
						+	"bootEntryAddress=%08X:%08X\0"
						+	"bootCRC=0x%08X"
						+	"\0pad=%s"
						+	"\0processorBytes=%s"
							, number
							, signature
							, signaturePad
							, partitionMapSizeBlocks, Utils.blocksToBytes( partitionMapSizeBlocks )
							, startBlock, Utils.blocksToBytes( startBlock )
							, sizeBlocks, Utils.blocksToBytes( sizeBlocks ), Utils.blocksToBytes( startBlock+sizeBlocks )
							, getName()
							, getType()
							, logicalStartBlock, logicalStartBlock+startBlock, Utils.blocksToBytes( logicalStartBlock ), Utils.blocksToBytes( logicalStartBlock+startBlock )
							, logicalSizeBlocks, Utils.blocksToBytes( logicalSizeBlocks )
							, decodeStatus( status )
							, bootStartBlock, bootStartBlock+startBlock, Utils.blocksToBytes( bootStartBlock ), Utils.blocksToBytes( bootStartBlock+startBlock )
							, bootSizeBytes
							, bootLoadAddress, bootLoadAddress2
							, bootEntryAddress, bootEntryAddress2
							, bootCRC
							, Utils.dump( pad )
							, Utils.dump( processorBytes )
				)
			) 
			.append( " }" );
		return sb.toString();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	private int		signature;
	private int		signaturePad;
	private int		partitionMapSizeBlocks;
	private long	startBlock;
	private long	sizeBlocks;
	private byte[]	nameBytes					= new byte[ NAME_SIZE ];
	private String	name;
	private byte[]	typeBytes					= new byte[ TYPE_SIZE ];
	private String	type;
	private long	logicalStartBlock;
	private long	logicalSizeBlocks;
	private int		status;
	private int		bootStartBlock;
	private int		bootSizeBytes;
	private int		bootLoadAddress;
	private int		bootLoadAddress2;
	private int		bootEntryAddress;
	private int		bootEntryAddress2;
	private int		bootCRC;
	private byte[]	processorBytes				= new byte[ PROCESSOR_SIZE ];
	private String	processor;
	private byte[]	pad							= new byte[ PAD_SIZE ];

	private int		number;
	
	protected PartitionEntry() {}

	protected static String decodeStatus( int partitionStatus ) {
		StringBuffer sb = new StringBuffer();

		for( PartitionStatus s : PartitionStatus.values() )
			sb.append( ((partitionStatus & s.getMask()) != 0) ? s.name() : s.getUnsetName() ).append(',');

		return sb.toString();
	}

	protected static int encodeStatus( String partitionStatus ) {
		int result = 0;
		
		if( partitionStatus == null )
			return 0;
		String[] statuses = partitionStatus.split( "\\s*,\\s*" );
		if( statuses == null )
			return 0;
			
		for( String status : statuses ) {
			if( status == null )
				continue;
			status = status.trim();
			if( status.isEmpty() )
				continue;
			PartitionStatus s = null;
			try {
				s = PartitionStatus.valueOf( status );
			}
			catch( Throwable t ) {
			}
			if( s != null )
				result |= s.getMask();
		}
		
		return result;
	}
}
