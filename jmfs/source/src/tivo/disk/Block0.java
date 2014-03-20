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
package tivo.disk;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tivo.io.Utils;
import tivo.io.Writable;

public class Block0 implements Writable, Cloneable
{	
	public static short APPLE_BLOCK0_SIGNATURE = (short)0x1492;

	private static final int	BOOT_PARAM_SIZE	= 128;
	private static final int	HOST_NAME_SIZE	= 32;
	private static final int	MAC_SIZE		= 6;
	private static final int	UNDEFINED_SIZE	= 338;

	private int				signature;
	private int				primaryKernelPartition;
	private int				alternateKernelPartition;
	private byte[]			bootParamBytes				= new byte[BOOT_PARAM_SIZE];
	private List<String>	bootParams;
	private byte[]			hostNameBytes				= new byte[HOST_NAME_SIZE];	
	private String			hostName;
	private int				ip;
	private byte[]			mac							= new byte[MAC_SIZE];
	private byte[]			undefined					= new byte[UNDEFINED_SIZE];

	public Block0( DataInput in ) throws Exception {
		/*
			Location      Size                 Value              Description
			=========     ===============      ================   ===================================
			0000-0001     16 bit integer       0x1492             Signature
			0002-0002     8 bit integer        0x03               Primary Kernel Partition
			0003-0003     8 bit integer        0x06               Alternate Kernel Partition
			0004-0083     128 byte string      'root=/dev/hdb4'   Boot Params. Null terminated
			0084-00A3     32 byte string       'unnamed'          Hostname. Null terminated
			00A4-00A7     4 bytes              10.x.x.x           IP address, x=random # 0x00 - 0xFE
			00A8-00AD     6 bytes              x:x:x:x:x:x        MAC address, x=random # 0x00 - 0xFF
			00AE-01FF     338 bytes            0                  Undefined
		*/

		signature					=	in.readUnsignedShort();
		if( !isValid() ) // do not continue if not a valid Block0
			return;

		primaryKernelPartition		=	in.readUnsignedByte();
		alternateKernelPartition	=	in.readUnsignedByte();
										in.readFully( bootParamBytes );
										in.readFully( hostNameBytes );
		ip							=	in.readInt();
										in.readFully( mac );
										in.readFully( undefined );
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeShort	( signature					);
		out.writeByte	( primaryKernelPartition	);
		out.writeByte	( alternateKernelPartition	);
		out.write		( bootParamBytes			);
		out.write		( hostNameBytes				);
		out.writeInt	( ip						);
		out.write		( mac						);
		out.write		( undefined					);
		
		return out;
	}

	public boolean isValid() {
		return (signature == APPLE_BLOCK0_SIGNATURE);
	}

	public int getSignature() {
		return signature;
	}

	public void setSignature(int signature) {
		this.signature = signature;
	}

	public int getPrimaryKernelPartition() {
		return primaryKernelPartition;
	}

	public void setPrimaryKernelPartition(int primaryKernelPartition) {
		this.primaryKernelPartition = primaryKernelPartition;
	}

	public int getAlternateKernelPartition() {
		return alternateKernelPartition;
	}

	public void setAlternateKernelPartition(int alternateKernelPartition) {
		this.alternateKernelPartition = alternateKernelPartition;
	}

	public byte[] getBootParamBytes() {
		return bootParamBytes;
	}

	public void setBootParamBytes(byte[] bootParamBytes) {
		this.bootParamBytes = Utils.alignToSize( bootParamBytes, BOOT_PARAM_SIZE );
		bootParams = null;
	}

	public List<String> getBootParams() {
		if( (bootParams == null) && (bootParamBytes != null) )
			bootParams = Arrays.asList( Utils.decodeStrings( bootParamBytes ) );
		return bootParams;
	}

	public void setBootParams(List<String> bootParams) {
		java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
		boolean first = true;
		for( String param : bootParams ) {
			try {
				if( !first )
					bytes.write( 0 );
				bytes.write( Utils.encodeString( param, null ) );
				first = false;
			}
			catch( Exception e ) {
			}
		}
		setBootParamBytes( bytes.toByteArray() );
	}

	public byte[] getHostNameBytes() {
		return hostNameBytes;
	}

	public void setHostNameBytes(byte[] hostNameBytes) {
		this.hostNameBytes = Utils.alignToSize( hostNameBytes, HOST_NAME_SIZE );
		hostName = null;
	}

	public String getHostName() {
		if( (hostName == null) && (hostNameBytes != null) )
			hostName = Utils.decodeString( hostNameBytes );
		return hostName;
	}

	public void setHostName(String hostName) {
		setHostNameBytes( Utils.encodeString( hostName, HOST_NAME_SIZE ) );
	}

	public int getIp() {
		return ip;
	}

	public void setIp(int ip) {
		this.ip = ip;
	}

	public byte[] getMac() {
		return mac;
	}

	public void setMac(byte[] mac) {
		this.mac = Utils.alignToSize( mac, MAC_SIZE );
	}

	public byte[] getUndefined() {
		return undefined;
	}

	public void setUndefined(byte[] undefined) {
		this.undefined = Utils.alignToSize( undefined, UNDEFINED_SIZE );
	}

	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
				"signature=0x%04X\0"
			+	"primaryKernelPartition=%d\0"
			+	"alternateKernelPartition=%d\0"
			+	"bootParams='%s'\0"
			+	"hostName='%s'\0"
			+	"ip=%d.%d.%d.%d\0"
			+	"mac=%02X:%02X:%02X:%02X:%02X:%02X"
				, getSignature()
				, getPrimaryKernelPartition()
				, getAlternateKernelPartition()
				, getBootParams()
				, getHostName()
				, (ip >>> 24), (ip >>> 16) & 0xFF, (ip >>> 8) & 0xFF, ip & 0xFF					
				, mac[0],mac[1],mac[2], mac[3],mac[4],mac[5]	
				) 
			)
			.append( " }" );
		return sb.toString();
	}

	@Override
	public Block0 clone() {
		Block0 that = new Block0();
		
		that.signature					= this.signature;
		that.primaryKernelPartition		= this.primaryKernelPartition;
		that.alternateKernelPartition	= this.alternateKernelPartition;
		that.bootParamBytes				= that.bootParamBytes.clone();
		that.bootParams					= new ArrayList<String>( this.bootParams );
		that.hostNameBytes				= this.hostNameBytes.clone();	
		that.hostName					= this.hostName;
		that.ip							= this.ip;
		that.mac						= that.mac.clone();
		that.undefined					= that.undefined.clone();
		
		return that;
	}
	
	private Block0() {}
}
