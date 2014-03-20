package tivo.disk;

import java.io.DataInputStream;

// just to keep large default data out of the main PartitionEntry code
public class EmptyPartitionMap extends PartitionEntry {
	private static final int		DEFAULT_STATUS;
	private static final byte[] 	DEFAULT_PROCESSOR_BYTES;
	private static final byte[] 	DEFAULT_PAD;

	private static final String		PADDING_FILE_1			= "PartitionEntry_padding1";
	private static final String		PADDING_FILE_2			= "PartitionEntry_padding2";
	private static final String		DEFAULT_STATUS_STRING	= "Valid,Allocated,NotInUse,NotBoot,Readable,Writable,PositionalBoot";
	
	static {
		DEFAULT_PROCESSOR_BYTES	= readBytes( PADDING_FILE_1, PROCESSOR_SIZE	);
		DEFAULT_PAD				= readBytes( PADDING_FILE_2, PAD_SIZE		);
		DEFAULT_STATUS			= encodeStatus( DEFAULT_STATUS_STRING );
	}
	
	public EmptyPartitionMap() {
		super();
		
		setSignature				( PartitionEntry.APPLE_PARTITION_SIGNATURE );
		setSignaturePad				( 0 );
		setPartitionMapSizeBlocks	( 1 );
		setStartBlock				( 1 );
		setSizeBlocks				( 63 );
		setName						( "Apple" );
		setType						( "Apple_partition_map" );
		setLogicalStartBlock		( 0 );
		setLogicalSizeBlocks		( getSizeBlocks() );
		setStatus					( DEFAULT_STATUS );
		setBootStartBlock			( 0 );
		setBootSizeBytes			( 0 );
		setBootLoadAddress			( 0 );
		setBootLoadAddress2			( 0 );
		setBootEntryAddress			( 0 );
		setBootEntryAddress2		( 0 );
		setBootCRC					( 0 );
		setProcessorBytes			( DEFAULT_PROCESSOR_BYTES.clone() );
		setPad						( DEFAULT_PAD.clone() );
	}
	
	private static byte[] readBytes( String name, int len ) throws RuntimeException {
		DataInputStream in = null;
		try  {
			in = new DataInputStream(EmptyPartitionMap.class.getClassLoader().getResourceAsStream( name ));
			
			byte[] buf = new byte[ len ];
			in.readFully(buf);
			
			return buf;
		}                                                             
		catch( Exception e ) {
			throw new RuntimeException( e );
		}
		finally {
			if( in != null ) { try { in.close(); } catch( Exception e1 ) {} }
		}
	}
}
