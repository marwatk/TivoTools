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
package tivo.io.process;

import java.io.InputStream;
import java.io.OutputStream;

public class Pipe implements Runnable {

	private InputStream		in;
	private OutputStream	out;
	private boolean			done = false;
	
	public Pipe( InputStream in, OutputStream out ) {
		this.in = in;
		this.out = out;
	}
	
	public void run() {
		byte[] buf = new byte[1024];
		try {
			int n;
			while( !done && ((n = in.read(buf)) >= 0) ) {
				out.write( buf, 0, n );
				out.flush();
			}
		}
		catch( Exception e ) {
		}
	}
	
	public void done() {
		done = true;
	}
	
	public Pipe start() {
		(new Thread(this)).start();
		return this;
	}
}
