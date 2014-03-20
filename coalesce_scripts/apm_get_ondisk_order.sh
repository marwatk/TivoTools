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

#Usage: apm_get_ondisk_order.sh <device> <partnum>


device=$1
partnum=$2

numpartitions=`./apm_get_partition_count.sh $device`
firstblock=`./apm_get_first_block_number.sh $device $partnum`

position=1

for (( i=1; i<=$numpartitions; i++ ))
do
    if [ $i -ne $partnum ]
    then
        tfirstblock=`./apm_get_first_block_number.sh $device $i`
        if [ $tfirstblock -lt $firstblock ]
        then
            position=$((position+1))
        fi
    fi
done
printf "%i" $position


