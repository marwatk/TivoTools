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
package tivo.view;

import java.util.ArrayList;
import java.util.List;

import tivo.disk.PartitionEntry;
import tivo.disk.Storage;
import tivo.io.Utils;

public class MfsView extends ContinuousLogicalView {
	public static final long	VOLUME_SIZE_ROUNDING	= 1024L;
	
	
	public void addMfsPartition( Storage disk, PartitionEntry pe ) {
		// account for MFS rounding
		long size = Utils.roundDown( pe.getLogicalSizeBlocks(), VOLUME_SIZE_ROUNDING );

		PartitionExtent x = new PartitionExtent( pe.getStartBlock() + pe.getLogicalStartBlock(), size, disk, pe.getNumber() );
		super.addStorage( x );
		partitions.add( pe );
	}
	
	public Storage getDiskForLogicalBlock(long logicalBlock) {
		Info i = getLogical( logicalBlock );
		return ( (i != null) && (i.getPhysical() != null) ) ? i.getPhysical().getStorage() : null;
	}
	
	public int getPartitionForLogicalBlock(long logicalBlock) {
		Info i = getLogical( logicalBlock );
		return ( (i != null) && (i.getPhysical() != null) ) ? ((PartitionExtent)i.getPhysical()).partitionNumber : -1;
	}
	
	public int getPartitionForPhysicalBlock(String diskName, long physicalBlock) {
		Info i = getPhysical( diskName, physicalBlock );
		return ( (i != null) && (i.getPhysical() != null) ) ? ((PartitionExtent)i.getPhysical()).partitionNumber : -1;
	}
	
	public List<PartitionEntry> getPartitions() {
		return partitions;
	}
	
	
	
	
	
	private List<PartitionEntry> partitions = new ArrayList<PartitionEntry>();
	
	private static class PartitionExtent extends PhysicalExtent {
		private int	partitionNumber;
		
		public PartitionExtent(long startBlock, long length, Storage disk, int partitionNumber ) {
			super(disk, startBlock, length);
			this.partitionNumber = partitionNumber;
		}
	}
}
