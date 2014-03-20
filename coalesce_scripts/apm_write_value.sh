#!/bin/bash

#    Copyright Marcus Watkins marwatk@marcuswatkins.net
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 2 of the License, or
#    (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.

#Usage: apm_write_value.sh <device> <partition number> <value offset> <value length> <integer value>

blocksize=512

device=$1
partnum=$2
offset=$3
length=$4
value=$5

let hexlength=$length*2

hex=`printf "%0${hexlength}X" $value`
let blockstart=$blocksize*$partnum
let valuestart=$blockstart+$offset

echo $hex | xxd -r -p | dd of=$device bs=1 seek=$valuestart count=$length conv=notrunc 2>/dev/null

