if ! cd /root; then
	echo "CAN NOT FIND /root DIRECTORY! PLEASE REBOOT!"
	exit 1
fi

printf "\n\n\n\n\n\n\n\n\n\n"
printf "\n\n\n\n\n\n\n\n\n\n"
printf "\n\n\n\n\n\n\n\n"
printf "\e[26A\e[1m"

./jmfs.sh ui.Guide "main.script"

if [ $? -eq 255 ]; then
	echo
	poweroff -p >/dev/null 2>/dev/null
fi
