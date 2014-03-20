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

#Usage: ddfast.sh <source_device> <source_position> <dest_device> <dest_position> <num_blocks_to_copy>

source=$1
source_pos=$2
dest=$3
dest_pos=$4
size=$5

#Assumes 512 block size

source_mult=2048
all_divisible=`./util_all_divisible.sh $source_mult $source_pos $size`
while [ $all_divisible == 0 -a $source_mult > 1 ]
do
    source_mult=$(($source_mult / 2))
    all_divisible=`./util_all_divisible.sh $source_mult $source_pos $size`
done

dest_mult=2048
all_divisible=`./util_all_divisible.sh $dest_mult $dest_pos $size`
while [ $all_divisible == 0 -a $dest_mult > 1 ]
do
    dest_mult=$(($dest_mult / 2))
    all_divisible=`./util_all_divisible.sh $dest_mult $dest_pos $size`
done

source_block_size=$((512 * $source_mult))
source_pos=$(($source_pos / $source_mult))
source_size=$(($size / $source_mult))

dest_block_size=$((512 * $dest_mult))
dest_pos=$(($dest_pos / $dest_mult))
dest_size=$(($size / $dest_mult))

echo "  Optimized ($source_mult:$dest_mult)"
cmd="  dd if=$source skip=$source_pos ibs=$source_block_size count=$source_size of=$dest seek=$dest_pos obs=$dest_block_size conv=notrunc"
echo $cmd
$cmd 2>&1 | ./ddprint.sh
printf "\nComplete\n"


#echo "(dd if=$source skip=$source_pos bs=$source_block_size count=$source_size iflags=fullblock  2>1) | dd iflags=fullblock of=$dest seek=$dest_pos bs=$dest_block_size count=$dest_size 2>1 | grep --line-buffered -v records | ./ddprint.sh"
#(dd if=$source skip=$source_pos bs=$source_block_size count=$source_size iflag=fullblock ) | dd iflag=fullblock of=$dest seek=$dest_pos bs=$dest_block_size count=$dest_size 2>1 | 



