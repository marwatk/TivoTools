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
package tivo.io;

public interface Checksummable {
	public static final byte[]	MAGIC			=	{ (byte)0xDE, (byte)0xAD, (byte)0xF0, (byte)0x0D }; // deadfood
	public static final long	MAGIC_NUMBER	=	(((long)MAGIC[0] & 0xFF) << 24)
												|	(((long)MAGIC[1] & 0xFF) << 16)
												|	(((long)MAGIC[2] & 0xFF) << 8)
												|	(((long)MAGIC[3] & 0xFF));
	public static final int		CHECKSUM_SIZE	=	Integer.SIZE / 8;

	public int		getChecksumOffset();
	public long		getChecksum();
	public void		setChecksum( long checksum );
	
	public boolean	isValidCrc();
	public void		setValidCrc(boolean validCrc);
}
