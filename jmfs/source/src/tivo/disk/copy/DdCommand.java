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
package tivo.disk.copy;

import tivo.disk.AppleDisk;

public class DdCommand extends SystemCopyCommand {

	public DdCommand() throws Exception {
		super();
	}
	public DdCommand(String in, long inOffset, String out, long outOffset,
			long length) throws Exception {
		super(in, inOffset, out, outOffset, length);
	}

	@Override
	protected String getCheckCommand() {
		return "dd -h";
	}

	@Override
	protected String[] getCopyCommand(String in, long inOffset, String out, long outOffset, long length) {
		String[] command = {	"dd",
								"bs=" + AppleDisk.BLOCK_SIZE,
								"skip=" + inOffset,
								"seek=" + outOffset,
								"count=" + length,
								"if=" + in,
								"of=" + out };
		return command;
	}
}
