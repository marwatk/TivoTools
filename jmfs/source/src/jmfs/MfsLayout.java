package jmfs;
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
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import tivo.Mfs;
import tivo.disk.PartitionEntry;
import tivo.disk.TivoDisk;
import tivo.io.Utils;
import tivo.view.Extent;
import tivo.view.MfsView;
import tivo.zone.Zone;

@SuppressWarnings("unused")
public class MfsLayout {
	
	public static void main(String[] args) {
		try  {
//			Mfs.VALIDATE_ZONES = false;
			Mfs m = new Mfs( args );
			run( m );
		}                                                             
		catch( Exception e ) {
			System.err.println();
			System.err.flush();
			e.printStackTrace();
		}
		System.out.println("\nMfsLayout: done");
	}
	
	
	public static class SizeInfo {
		private long	total;
		private long	used;
		private long	free;
	};

	public static SizeInfo calculateSize( List<Zone> zones ) {
		SizeInfo si = new SizeInfo();

		if( zones != null ) {
			for( Zone z : zones ) {
				si.total	+= z.getDataSize();
				si.free		+= z.getFreeDataBlocks();
				si.used		+= z.getDataSize() - z.getFreeDataBlocks();
			}
		}

		return si;
	}

	public static void outputSize( SizeInfo si, Mfs mfs ) {
		System.out.println(getFormattedSize(mfs));
		
	}

	public static String getFormattedSize( Mfs mfs ) {
		Utils.setFormatFull();
		
		SizeInfo	si		= calculateSize( mfs.getZones() );
		String		result	=
			Utils.printf(	"Size of zones:\0"
						+	"Used:\t%s\0"
						+	"Free:\t%s\0"
						+	"Total:\t%s"
							, formatSize( si.used )
							, formatSize( si.free )
							, formatSize( si.total )
						);
		
		Long size = mfs.getBootDisk().getMfsHeader().getTotalSectors();
		if( mfs.getMfs().getSize() != size )
			result += Utils.printf( "Size of MFS volume set: %s", formatSize( mfs.getMfs().getSize() ) );
		result += Utils.printf( "Recordable space reported by Tivo: %s, approximately %d HD hours",
			formatSize( size ),
			getHdHours( size ) );
		
		return result;
	}
	
	static void layout( Mfs m ) {
		Map<String,TivoDisk> disks = m.getDisks();
		for( TivoDisk disk : disks.values() )
			outputDisk( disk );
	}
	
	static void run( Mfs m ) {
		Utils.setFormatFull();
		layout( m );
		outputZonesBrief( m );
//		outputZonesFull( m );
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static class ZoneInfo {
		private String	disk;
		private int		partition;
		private	int		zoneNumber;
		private Extent	extent;
		private String	description;
	};
	
	private static String formatExtent( Extent x ) {
		return Utils.format( "start=%10d, size=%10d (%7s), end=%10d"
							,	x.getStartBlock()
							,	x.getLength()
							,	Utils.formatSizeHuman( Utils.blocksToBytes( x.getLength() ) )
							,	x.getEndBlock()
		);
	}

	private static String formatSize( long blocks ) {
		long bytes = Utils.blocksToBytes( blocks );
		return Utils.format( "%d (%s)"
					, blocks
					, Utils.formatSizeHuman( bytes )
		);
	}

	private static ZoneInfo createZoneInfo( MfsView vv, boolean physical,
			int number, long startBlock, long size, String description ) {
		ZoneInfo	zi		= new ZoneInfo();
		Extent		extent	= new Extent( startBlock, size );

		zi.disk			= vv.getDiskForLogicalBlock( extent.getStartBlock() ).getName();
		zi.partition	= vv.getPartitionForLogicalBlock( extent.getStartBlock() );
		zi.zoneNumber	= number;
		zi.extent		= extent;
		zi.description	= description;

		if( physical )
			extent.setStartBlock( vv.toPhysical( startBlock ).getAddress() );

		return zi;
	}

	private static List<ZoneInfo> createZoneInfos( Mfs mfs, boolean physical ) {
		MfsView		vv		= mfs.getMfs();
		List<Zone>		zones	= mfs.getZones();
		List<ZoneInfo>	infos	= new ArrayList<ZoneInfo>();

		if( zones != null ) {
			int zoneNumber = 0;
			for( Zone z : zones ) {
				infos.add( createZoneInfo(	vv, physical, zoneNumber, 
											z.getDescriptorStartBlock(), z.getDescriptorSize(),
											z.getType().toString() + " descriptor" ) );
				infos.add( createZoneInfo(	vv, physical, zoneNumber, 
											z.getBackupStartBlock(), z.getDescriptorSize(),
											z.getType().toString() + " descriptor backup" ) );
				infos.add( createZoneInfo(	vv, physical, zoneNumber, 
											z.getDataStartBlocks(), z.getDataSize(),
											z.getType().toString() + " data" ) );
				zoneNumber++;
			}
		}

		return infos;
	}

	private static void outputZoneLayout( List<ZoneInfo> infos ) {
		Collections.sort( infos, new Comparator<ZoneInfo>() {
			public int compare(ZoneInfo o1, ZoneInfo o2) {
				if( o1 == null ) {
					if( o2 == null )
						return 0;
					else
						return -1;
				}
				else {
					if( o2 == null )
						return 1;
				}
				return (int)Math.signum(o1.extent.getStartBlock() - o2.extent.getStartBlock());
			}
		});

		for( ZoneInfo zi : infos ) {
			Utils.printf( System.out, "  [%d] %-12s%s  %s"
				, zi.zoneNumber
				, zi.disk + zi.partition
				, formatExtent( zi.extent )
				, zi.description
			);
		}
	}

	private static void outputZonesBrief( Mfs mfs ) {
		List<ZoneInfo>	logical		= createZoneInfos( mfs, false );
		List<ZoneInfo>	physical	= createZoneInfos( mfs, true );
		SizeInfo		si			= calculateSize( mfs.getZones() );

		Utils.printf( System.out, "Zones Logical\n------------------" );
		outputZoneLayout( logical );
		Utils.printf( System.out, "------------------\n" );
		Utils.printf( System.out, "Zones Physical\n------------------" );
		outputZoneLayout( physical );
		Utils.printf( System.out, "------------------\n" );
		outputSize( si, mfs );
	}

	private static void outputZonesFull( Mfs mfs ) {
		List<ZoneInfo>	logical	= createZoneInfos( mfs, false );
		List<Zone>		zones	= mfs.getZones();
		SizeInfo		si		= calculateSize( mfs.getZones() );

		int zoneNumber = 0;
		Utils.printf( System.out, "Zones\n------------------" );
		for( Zone z : zones ) {
			ZoneInfo zi = logical.get( zoneNumber << 1 ); // two Infos per sone
			Utils.printf( System.out, "  [%d] %d:%d\0%s"
				, zi.zoneNumber
				, zi.disk, zi.partition
				, z
			);
			zoneNumber++;
		}
		Utils.printf( System.out, "------------------\n" );

		outputSize( si, mfs );
	}

	private static List<PartitionEntry> sortByPosition( List<PartitionEntry> partitions ) {
		List<PartitionEntry> sorted = new ArrayList<PartitionEntry>( partitions );
		Collections.sort( sorted, new Comparator<PartitionEntry>() {
			public int compare(PartitionEntry o1, PartitionEntry o2) {
				if( o1 == null ) {
					if( o2 == null )
						return 0;
					else
						return -1;
				}
				else {
					if( o2 == null )
						return 1;
				}
				return (int)Math.signum(o1.getStartBlock() - o2.getStartBlock());
			}
		});
		return sorted;
	}

	private static void outputPartitions( List<PartitionEntry> partitions ) {
		partitions = sortByPosition( partitions );
		for( PartitionEntry pe : partitions ) {
			long size = Utils.blocksToBytes( pe.getSizeBlocks() ); 
			Utils.printf( System.out, "%-2d:  start=%10d, size=%10d (%7s), type=%-21s, name='%s'"
									, pe.getNumber()
									, pe.getStartBlock()
									, pe.getSizeBlocks()
									, Utils.formatSizeHuman( size )
									, "'" + pe.getType().toString() + "'"
									, pe.getName()
						);
			System.out.flush();
		}
	}

	private static void outputDisk( TivoDisk disk ) {
		Utils.printf( System.out, "Disk '%s'\n------------------", disk.getName() );
		outputPartitions( disk.getPartitions() );
		long freeBytes = disk.getFreeBytes();
		Utils.printf( System.out, "------------------\nUnallocated space: %,d (%s)\n"
							, freeBytes
							, Utils.formatSizeHuman( freeBytes )
					);
	}
	
	private static int getHdHours( long sizeBlocks ) {
		final double A = 7.3738d;
		final double B = -0.456322d;
		
		return (int)(A * (double)sizeBlocks / 100000000d + B + 0.5d);
	}
}
