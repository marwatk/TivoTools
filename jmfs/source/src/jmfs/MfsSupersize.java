package jmfs;

import java.util.Arrays;
import java.util.List;

import jmfs.parameters.Parameter;
import jmfs.parameters.Parameters;
import tivo.Mfs;
import tivo.io.JavaLog;
import tivo.io.Utils;
import tivo.mfs.db.Attribute;
import tivo.mfs.db.DbObject;
import tivo.mfs.inode.Inode;
import tivo.mfs.inode.InodeFinder;
import tivo.mfs.inode.InodeType;

public class MfsSupersize {
	private static final JavaLog log = JavaLog.getLog( MfsSupersize.class );
	
	private static final String		PATH				= "/Config/DiskConfigurations/Active";
	private static final int		SUPER_SIZE			= Integer.MAX_VALUE;
	private static final String		SUPER_SIZE_PARAM	= "size";
	private static final Parameters parms				= new Parameters(
		new Parameter( SUPER_SIZE_PARAM, "s" )
	); 
	
	protected int	superSize = SUPER_SIZE;
	protected Mfs	mfs;
	
	public static void main(String[] args) {
		parms.processParameters( args );
		Object o = (new MfsSupersize(getSuperSize())).supersizeNoException( parms.getFreeOptions() );
		System.err.println("\n" + o.getClass().getSimpleName() + ": done");
	}

	public MfsSupersize supersize( String... args ) throws Exception {
		log.info( "Suzpersize called with arguments: size=%d, disks=%s", superSize, Arrays.asList(args) );
		
		if( mfs == null )
			mfs = new Mfs( true, args );
		Inode i = findByPath( PATH );
		processResult( i );
		
		return this;
	}
	



	
	
	
	
	
	
	
	
	
	



	public MfsSupersize() {
		this( SUPER_SIZE );
	}
	
	public MfsSupersize( int superSize ) {
		this.superSize = superSize;
	}

	protected MfsSupersize( Mfs mfs ) {
		this.mfs = mfs;
	}

	@SuppressWarnings("unchecked")
	protected void processResult( Inode inode ) throws Exception {
		if( inode.getType() != InodeType.DB )
			throw new Exception( "Unexpected inode type '" + inode.getType() + "', expected '" + InodeType.DB + "'" );
		
		DbObject dbo = Utils.read( mfs.getInodeInput(inode), DbObject.class );
		List<?> objs = dbo.getObjects("/DiskConfiguration/MaxDiskSize");
		
		for( Object o : objs ) {
			if( o == null )
				continue;
			((Attribute<Integer>)o).setValue( superSize );
		}
	
		Utils.write( mfs.getInodeOutput(inode), dbo );
		if( inode.isDataInBlock() )
			mfs.writeInode( inode );
	}

	protected Inode findByPath( String path ) throws Exception {
		InodeFinder p = InodeFinder.find( mfs, path );
		if( p.getResult() == null ) {
			String notFound = (p.getFound() == null) ? path : path.substring( p.getFound().length() );
			throw new Exception( "'" + notFound + "' was not not found in '" + path + "'" );
		}
		return p.getResult();
	}

	private MfsSupersize supersizeNoException( List<String> args ) {
		return supersizeNoException( args == null ? null : args.toArray(new String[args.size()]) );
	}
	
	private MfsSupersize supersizeNoException( String[] args ) {
		try  {
			return supersize( args );
		}                                                             
		catch( Exception e ) {
			Utils.printException( getClass().getSimpleName(), e );
			log.info( e, "" );
		}
		return this;
	}
	
	private static int getSuperSize() {
		String	val		= parms.getValue(SUPER_SIZE_PARAM);
		int		size	= SUPER_SIZE;
		
		try {
			if( val != null )
				size = Integer.parseInt( val );
		}
		catch( Exception e ) {
		}
		
		return size;
	}
}
