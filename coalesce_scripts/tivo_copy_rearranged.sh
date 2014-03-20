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

#Usage: tivo_copy_rearranged.sh <source device> <dest device> <new partition 1> <new partition 2>...
#Example: tivo_copy_rearranged.sh /dev/sda /dev/sdb 1 11 12 13 15 2 3 4 5 6 7 8 9 14 10

source=$1
dest=$2
shift
shift

echo "************************************************************************"
echo "WARNING: This script is for advanced users only. It makes no attempts to"
echo "determine if you entered correct values, devices or other data."
echo "Use at your own risk! You could easily delete all data on your original"
echo "Tivo disk if you're not careful."
echo "************************************************************************"
read -p "Press [enter] to continue or press Ctrl+C to quit"

dest_partition_1_name=`./apm_get_partition_type.sh $dest 1`

if [ "$dest_partition_1_name" == "Apple_partition_map" ]; then
    echo ""
    echo "SUPER ULTRA WARNING!!!!!!!"
    echo "Destination $dest already has a tivo partition table!"
    echo "Are you sure you know what you're doing?"
    read -p "Press [enter] to continue or press Ctrl+C to quit"
fi
    
src_partition_1_name=`./apm_get_partition_type.sh $source 1`

if [ "$src_partition_1_name" != "Apple_partition_map" ]; then
    echo ""
    echo "Source $source doesn't appear to be a tivo drive."
    echo "Are you sure you know what you're doing? Remember, bad things can happen!"
    exit 1
fi

temp_apm=/tmp/apm.tmp

echo "$source => $dest"

#todo confirm source and dest disks

copy_cmd="./ddrescue.sh"

./apm_copy_apm.sh $source $temp_apm
first_block=1

cmd_count=0

while [ "$1" != "" ]; do
    partnum=$1
    shift
    optimal_size=`./apm_get_optimal_size.sh $source $partnum`
    ./apm_set_first_block_number.sh $temp_apm $partnum $first_block
    ./apm_set_partition_size.sh $temp_apm $partnum $optimal_size
    source_start=`./apm_get_first_block_number.sh $source $partnum`
    if [ $partnum != 1 ]; then #We already copied the apm
        if [ $optimal_size == 8 ]; then #Bootstrap partitions are all zeros
            dd_cmds[$cmd_count]="dd if=/dev/zero of=$dest seek=$first_block bs=512 count=$optimal_size"
        else        
            dd_cmds[$cmd_count]="$copy_cmd $source $source_start $dest $first_block $optimal_size"
        fi
        human_size=`./util_blocks_to_size.sh $optimal_size`
        dd_txt[$cmd_count]="Copying partition $partnum with size $human_size"
        cmd_count=$(($cmd_count + 1))
    fi
    first_block=$(($first_block + $optimal_size))
done
echo "====================="
echo "Source APM Structure:"
echo "====================="
./apm_display.sh $source

echo ""
echo ""
echo "=========================="
echo "Destination APM Structure:"
echo "=========================="
./apm_display.sh $temp_apm


read -p "Press [enter] if everything looks correct...."

printf "Copying new partition table to target drive...\n"
./apm_copy_apm.sh $temp_apm $dest


printf "About to run:\n"

for (( i=0; i<$cmd_count; i++ ))
do
    echo ${dd_cmds[$i]}
done

read -p "Press [enter] to continue..."
for (( i=0; i<$cmd_count; i++ ))
do
    echo ${dd_txt[$i]}
    ${dd_cmds[$i]}
done


