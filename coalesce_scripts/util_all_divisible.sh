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

#Usage: util_all_divisible.sh <divisor> <numerator 1> <numerator 2> ...

divisor=$1

while [ "$1" != "" ]
do
    numerator=$1
    mod=$(($numerator % $divisor))
    shift
    if [ $mod != 0 ]; then
        printf "0"
        exit 0
    fi
done
printf "1"
exit 1
