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
package tivo.zone;

import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;

public abstract class ZoneHeader implements Readable<ZoneHeader>, Writable {
    private long sector;   /* Sector of next table */
    private long sbackup;  /* Sector of backup of next table */
    private long length;   /* Length of next table in sectors */
    private long size;     /* Size of partition of next table */
    private long min;      /* Minimum allocation of next table */

	public int getReadAheadSize() {
		return 0;
	}

    public long getSector() {
        return sector;
    }
    public void setSector(long sector) {
        this.sector = sector;
    }
    public long getSbackup() {
        return sbackup;
    }
    public void setSbackup(long sbackup) {
        this.sbackup = sbackup;
    }
    public long getLength() {
        return length;
    }
    public void setLength(long length) {
        this.length = length;
    }
    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }
    public long getMin() {
        return min;
    }
    public void setMin(long min) {
        this.min = min;
    }
    public boolean isValid() {
    	return (getSector() != 0) && (getSbackup() != 0xDEADBEEFL);
    }

    public boolean is64() {
    	return false;
    }
    
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
						"startBlockPrimary=%d\0"
					+	"startBlockBackup=%d\0"
					+	"length=%d\0"
					+	"size=%d\0"
					+	"min=%d"
					    , sector
					    , sbackup
					    , length
					    , size
					    , min
				)
			) 
			.append( '}' );
		return sb.toString();
	}
}
