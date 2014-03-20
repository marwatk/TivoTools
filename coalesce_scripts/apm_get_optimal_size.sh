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

#Usage: apm_get_optimal_size.sh <device> <partition number>


device=$1
partnum=$2

type=`./apm_get_partition_type.sh $device $partnum`
actual=`./apm_get_partition_size.sh $device $partnum`

if [ $type == "MFS" ]
then
    mod=$(($actual % 1024))
    optimal=$(($actual - $mod))
    printf "%i" $optimal
else
    if [ $type == "Image" -a $actual -eq 1 ]
    then
        printf "%i" 8
    else
        printf "%i" $actual
    fi
fi


