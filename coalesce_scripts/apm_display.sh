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

#Usage: apm_display.sh <device>


device=$1

numpartitions=`./apm_get_partition_count.sh $device`

echo "Part      First      Actual     Optimal Disk                                 "
echo "Num       Block        Size        Size Order              Type          Name"


for (( i=1; i<=$numpartitions; i++ ))
do
    first_block=`./apm_get_first_block_number.sh $device $i`
    name=`./apm_get_partition_name.sh $device $i`
    type=`./apm_get_partition_type.sh $device $i`
    size=`./apm_get_partition_size.sh $device $i`
    optimal=`./apm_get_optimal_size.sh $device $i`
    position=`./apm_get_ondisk_order.sh $device $i`
    printf "%02i  % 11i % 11i % 11i %02i % 20s %s\n" $i $first_block $size $optimal $position $type "$name" 
done
