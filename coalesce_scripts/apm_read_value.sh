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

#Usage: apm_read_value.sh <device> <partition number> <value offset> <value length>


blocksize=512

device=$1
partnum=$2
offset=$3
length=$4

let blockstart=$blocksize*$partnum
let valuestart=$blockstart+$offset

hex=`(dd status=noxfer if=$device bs=1 skip=$valuestart count=$length 2>/dev/null) | xxd -p -u`
dec=$((16#$hex))
echo $dec

