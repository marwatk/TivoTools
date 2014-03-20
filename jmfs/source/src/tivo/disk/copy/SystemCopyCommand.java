package tivo.disk.copy;

import tivo.io.process.ExternalProcess;

public abstract class SystemCopyCommand extends CopyCommand {
	protected abstract String	getCheckCommand();
	protected abstract String[]	getCopyCommand(String in, long inOffset, String out, long outOffset, long length);


	@Override
	protected int execute() throws Exception {
		int retval = p.execute();
		if( p.hasFailed() )
			throw p.getFailure();
		return retval;
	}
	
	
	
	
	
	private ExternalProcess p;

	protected SystemCopyCommand() throws Exception {
		Runtime.getRuntime().exec( getCheckCommand() );
	}
	
	protected SystemCopyCommand(String in, long inOffset, String out, long outOffset, long length) throws Exception {
		super(in, inOffset, out, outOffset, length);
		p = new ExternalProcess( getCopyCommand(in, inOffset, out, outOffset, length) );
	}
}
