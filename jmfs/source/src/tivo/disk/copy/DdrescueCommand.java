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

public class DdrescueCommand extends SystemCopyCommand {
	public DdrescueCommand() throws Exception {
		super();
	}

	public DdrescueCommand(String in, long inOffset, String out,
			long outOffset, long length) throws Exception {
		super(in, inOffset, out, outOffset, length);
	}

	@Override
	protected String getCheckCommand() {
		return "ddrescue -V";
	}

	@Override
	protected String[] getCopyCommand(String in, long inOffset, String out,
			long outOffset, long length) {
		String[] command = {	"ddrescue",
								"-v",
								"--max-retries=3",
								"--block-size=" + AppleDisk.BLOCK_SIZE,
								"--input-position=" + inOffset + "b",
								"--output-position=" + outOffset + "b",
								"--max-size=" + length + "b",
								in,
								out };
		return command;
	}
}
