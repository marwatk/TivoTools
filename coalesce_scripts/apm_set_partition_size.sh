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

#Usage: apm_set_partition_size.sh <device> <partition number> <new partition size (blocks)>


device=$1
partnum=$2
size=$3

#partition block count
./apm_write_value.sh $device $partnum 12 4 $size

#data block count
./apm_write_value.sh $device $partnum 84 4 $size


