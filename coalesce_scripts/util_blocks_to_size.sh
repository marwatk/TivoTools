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

#Usage: util_blocks_to_size.sh <block count>


blocks=$1

bytes=$(($blocks * 512))

#dd uses base 10 conversions

if [ $bytes -gt 1000000000000 ]; then
    printf "%.1fTB" `echo "$bytes/1000000000000" | bc`
    exit 0
fi

if [ $bytes -gt 1000000000 ]; then
    printf "%.1fGB" `echo "$bytes/1000000000" | bc`
    exit 0
fi

if [ $bytes -gt 1000000 ]; then
    printf "%.1fMB" `echo "$bytes/1000000" | bc`
    exit 0
fi

if [ $bytes -gt 1000 ]; then
    printf "%.1fKB" `echo "$bytes/1000" | bc`
    exit 0
fi

printf "%iB" $bytes

