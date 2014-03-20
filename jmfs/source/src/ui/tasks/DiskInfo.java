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
package ui.tasks;

public class DiskInfo {
	private String	name;
	private String	model;
	private String	sizeHuman;
	private long	sizeBytes;
	
	
	public DiskInfo() {}
	public DiskInfo( String	name, String model, String sizeHuman, long sizeBytes ) {
		this.name		= name;
		this.model		= model;
		this.sizeHuman	= sizeHuman;
		this.sizeBytes	= sizeBytes;
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}

	public String getSizeHuman() {
		return sizeHuman;
	}
	public void setSizeHuman(String sizeHuman) {
		this.sizeHuman = sizeHuman;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}
	public void setSizeBytes(long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}




	public String toString() {
		return name + ", model='" + model + "', size=" + sizeHuman + " (" + sizeBytes + " bytes)";
	}
}