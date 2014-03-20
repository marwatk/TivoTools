#!/bin/bash

./ddmonitor.sh < /dev/null > /dev/null 2>&1 &
disown $!

while read LINE; do
    last_line="$LINE"
    if [[ "$LINE" == *records\ in* ]]; then
        last_in="$LINE"
    elif [[ "$LINE" == *records\ out* ]]; then
        last_out="$LINE"
    else
        if [[ "$LINE" == *copied* && "$LINE" == *bytes* ]]; then
            printf "  %s\r" "$LINE"
        else
            printf " **%s\n" "$LINE"
        fi
    fi
done

killall ddmonitor.sh > /dev/null 2>&1

printf "\n";
printf "  IN: %s\n" "$last_in"
printf "  OUT: %s\n" "$last_out"
printf "  SUM: %s\n" "$last_line"
