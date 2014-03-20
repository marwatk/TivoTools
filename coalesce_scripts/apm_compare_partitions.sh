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

#Usage: apm_compare_partitions.sh <device A> <device B>


devicea=$1
deviceb=$2

numparts=`./apm_get_partition_count.sh $devicea`


for (( partnum=2; $partnum<$numparts; partnum++ ))
do
    
    
    
    first_block_a=`./apm_get_first_block_number.sh $devicea $partnum`
    first_block_b=`./apm_get_first_block_number.sh $deviceb $partnum`
    
    size_a=`./apm_get_partition_size.sh $devicea $partnum`
    size_b=`./apm_get_partition_size.sh $deviceb $partnum`
    
    
    size_to_use=$size_a
    
    if [ $size_a -gt $size_b ]; then
        echo "$partnum: $devicea is larger"
        size_to_use=$size_b
    fi
    if [ $size_b -gt $size_a ]; then
        echo "$partnum: $deviceb is larger"
    fi
    
    last_block_a=$(($first_block_a + $size_to_use - 1))
    last_block_b=$(($first_block_b + $size_to_use - 1))
    
    idx=0
    for (( i=0; i<5; i++ ))
    do
        ablocks[$idx]=$(($first_block_a+$i))
        bblocks[$idx]=$(($first_block_b+$i))
        offsets[$idx]=$i
        idx=$(($idx + 1))
    done
    for (( i=4; i>=0; i-- ))
    do
        ablocks[$idx]=$(($last_block_a-$i))
        bblocks[$idx]=$(($last_block_b-$i))
        offsets[$idx]="-$i"
        idx=$(($idx + 1))
    done
    
    allgood=1
    
    for (( i=0; $i<$idx; i++ ))
    do
        
        offset=${offsets[$i]}
        ablock=${ablocks[$i]}
        bblock=${bblocks[$i]}
        
        md5a=`./util_md5_block.sh $devicea $ablock`
        md5b=`./util_md5_block.sh $deviceb $bblock`
        if [ $md5a != $md5b ]; then
            allgood=0
            printf "%i: [% 3s] %010i %s != %s %010i\n" $partnum $offset $ablock $md5a $md5b $bblock
        fi
    
    done
    if [ $allgood == 1 ]; then
        echo "$partnum: All offsets matched"
    fi
done
