#include <unistd.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <sys/select.h>
#include <stdio.h>
#include <errno.h>
#include <stdarg.h>
#include <termios.h>
#include <string.h>

void
fatal(char *fmt, ...)
{
	va_list ap;

	va_start(ap, fmt);

	vprintf(fmt, ap);
	printf("\n");
	exit(1);

	va_end(ap);
}

signed char
get_char(int fd)
{
	fd_set fds;
	struct timeval tv;
	char t;

	tv.tv_sec = 0;
	tv.tv_usec = 250000;

	FD_ZERO(&fds);
	FD_SET(fd, &fds);
	switch(select(fd + 1, &fds, NULL, NULL, &tv))
	{
		case 0:
			return(0);
			break;
		case 1:
			if(read(0, &t, 1) == -1) return(-1);
			return(t);
		case -1:
		default:
			return(-1);
			break;
	}
}

int
main(int argc, char **argv)
{
	char *allowed = NULL;
	time_t timeout = 0, lasttime;
	int c;
	struct termios told, tnew;
	int ret = 1, ignorecase = 1, quiet = 0;

	while((c = getopt(argc, argv, "a:t:h")) != -1)
	{
		switch(c)
		{
			case 'a':
				allowed = optarg;
				break;
			case 't':
				timeout = strtol(optarg, NULL, 0);
				break;
			case 'c':
				ignorecase = 0;
				break;
			case 'q':
				quiet = 1;
				break;
			case 'h':
			default:
				printf("usage: %s [ -a allowed ] "
					"[ -t timeout ] [ -i ] [ -q ]\n",
					argv[0]);
				printf("returns: 1-based index into allowed "
					"string, or ASCII value\n");
				exit(1);
				break;
		}

	}

	if(tcgetattr(0, &told) == -1)
		fatal("tcgetattr failed: %s\n", strerror(errno));
	memcpy(&tnew, &told, sizeof(struct termios));

	tnew.c_lflag &= ~(ICANON | ECHO | ISIG);

	tcsetattr(0, TCSAFLUSH, &tnew);

	if(timeout)
	{
		lasttime = time(NULL);
		timeout += lasttime;
	}

	while(!timeout || (timeout > lasttime))
	{
		signed char t;
		char *ir;

		if(timeout && ! quiet && (lasttime != time(NULL)))
		{
			printf(".");
			fflush(stdout);
			lasttime = time(NULL);
		}

		t = get_char(0);
		if(t == -1) goto out;
		if(t == 0) continue;

		if(! allowed)
		{
			if(ignorecase && (t >= 'a') && (t <= 'z'))
				t = t & 0xdf;
			ret = t;
			goto out;
		}

		if(ignorecase)
		{
			ir = index(allowed, toupper(t));
			if(! ir)
				ir = index(allowed, tolower(t));
		} else {
			ir = index(allowed, t);
		}

		if(ir)
		{
			ret = ir - allowed + 1;
			goto out;
		}
	}

	ret = 0;

out:
	tcsetattr(0, TCSAFLUSH, &told);
	return(ret);
}
