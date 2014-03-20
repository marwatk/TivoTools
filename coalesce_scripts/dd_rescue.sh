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

#Usage: dd_rescue.sh <source_device> <source_position> <dest_device> <dest_position> <num_blocks_to_copy>

source=$1
source_pos=$2
dest=$3
dest_pos=$4
size=$5

block_size=512

source_pos=$(($source_pos * 512))
dest_pos=$(($dest_pos * 512))
$size=$(($size * 512))

cmd="dd_rescue -s $source_pos -S $dest_pos -m $size $source $dest"

echo $cmd
$cmd


