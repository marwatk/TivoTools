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

#Usage: tivo_coalesce.sh <device> <first partition number> <second partition number>


device=$1
first_part=$2
second_part=$3

if [ $first_part -gt $second_part ]; then
    echo "First partition is greater than second partition"
    exit 1
fi

first_type=`./apm_get_partition_type.sh $device $first_part`
second_type=`./apm_get_partition_type.sh $device $second_part`
if [ "$first_type" != "MFS" ]; then
    echo "First partition is not MFS ($first_type)"
    exit 1
fi
if [ "$second_type" != "MFS" ]; then
    echo "Second partition is not MFS"
    exit 1
fi

first_size=`./apm_get_partition_size.sh $device $first_part`
first_optimal_size=`./apm_get_optimal_size.sh $device $first_part`
second_size=`./apm_get_partition_size.sh $device $second_part`

if [ "$first_size" != "$first_optimal_size" ]; then
    echo "First size is not optimal, can't coalesce without moving"
    exit 1
fi

first_loc=`./apm_get_first_block_number.sh $device $first_part`
second_loc=`./apm_get_first_block_number.sh $device $second_part`

correct_second_loc=$(($first_loc + $first_optimal_size))
if [ "$correct_second_loc" != "$second_loc" ]; then
    echo "Partitions are not adjacent"
    exit 1
fi

num_partitions=`./apm_get_partition_count.sh $device`
if [ "$num_partitions" -lt 15 ]; then
    echo "Cannot have fewer than 14 partitions (partition 14 cannot be moved)"
    exit 1
fi

backup_file="/tmp/coalesce_${first_part}_${second_part}.img"
dd if=$device of=$backup_file.img bs=512 count=64 2>/dev/null

echo "Backup APM saved to $backup_file"

new_size=$(($first_optimal_size + $second_size))
echo "Setting parition $first_part size to $new_size"
./apm_set_partition_size.sh $device $first_part $new_size
for (( i=$second_part; i<14; i++ ))
do
    if [ $i -eq 13 ]; then
        j=$(($i + 2))
    else
        j=$(($i + 1))
    fi
    echo "Moving APM entry $j to $i"
    ./apm_copy_entry.sh $device $j $i
done
echo "Zeroing APM entry $num_partitions"
./apm_clear_entry.sh $device $num_partitions

num_partitions=$(($num_partitions - 1))
echo "Setting partition count to $num_partitions"
./apm_set_partition_count.sh $device $num_partitions


./apm_display.sh $device
