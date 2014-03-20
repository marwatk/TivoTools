#!/bin/bash

source=$1
source_pos=$2
dest=$3
dest_pos=$4
size=$5

OPTIMIZE=1

block_size=512

#runs every second forcing dd to output status
./ddmonitor.sh &
disown $!

echo "dd if=$source of=$dest seek=$dest_pos skip=$source_pos bs=512 count=$size conv=notrunc"

if [ $OPTIMIZE != 0 ]; then

    mult=2048
    all_divisible=`./util_all_divisible.sh $mult $source_pos $dest_pos $size`
    while [ $all_divisible == 0 -a $mult > 1 ]
    do
        mult=$(($mult / 2))
        all_divisible=`./util_all_divisible.sh $mult $source_pos $dest_pos $size`
    done
    
    if [ $mult > 1 ]; then
        block_size=$(($block_size * $mult))
        source_pos=$(($source_pos / $mult))
        dest_pos=$(($dest_pos / $mult))
        size=$(($size / $mult))
        echo "Optimized: ($mult)"
        echo "dd if=$source of=$dest seek=$dest_pos skip=$source_pos bs=512 count=$size conv=notrunc"
    fi
fi

dd if=$source of=$dest seek=$dest_pos skip=$source_pos bs=$block_size count=$size conv=notrunc 2>&1 | grep --line-buffered -v records | ./ddprint.sh
printf "\nComplete\n"
killall ddmonitor.sh > /dev/null 2>&1

#TODO: I can't figure out how to make ddrescue copy to different locations

