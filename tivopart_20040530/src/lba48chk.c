#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <linux/hdreg.h>
#include <linux/fs.h>
#include "partition_map.h"
#include "dump.h"

#define SECTOR_SIZE 512

#define MIN_ROOTBLOCKS 262144

//
// Defines
//
#define ARGV_CHUNK 5
#define CFLAG_DEFAULT	0
#define DFLAG_DEFAULT	0
#define HFLAG_DEFAULT	0
#define INTERACT_DEFAULT	0
#define LFLAG_DEFAULT	0
#define RFLAG_DEFAULT	0
#define VFLAG_DEFAULT	0

//
// Global Variables
//
int lflag = LFLAG_DEFAULT;	/* list the device */
char *lfile;	/* list */
int vflag = VFLAG_DEFAULT;	/* show version */
int hflag = HFLAG_DEFAULT;	/* show help */
int dflag = DFLAG_DEFAULT;	/* turn on debugging commands and printout */
int rflag = RFLAG_DEFAULT;	/* open device read Only */
int interactive = INTERACT_DEFAULT;
int cflag = CFLAG_DEFAULT;	/* compute device size */

int force = 0;
int verbose = 0;

partition_map *
get_entry(partition_map_header *map, int i)
{
	partition_map *m;

	m = find_entry_by_disk_address(i, map);
	if(! m)
	{
		printf("fatal: cannot locate partition %d\n", i);
		exit(1);
	}
	return(m);
}

static inline u32
getpartsize(partition_map_header *map, int i)
{
	return(get_entry(map, i)->data->dpme_pblocks);
}

static inline u32
getpartstart(partition_map_header *map, int i)
{
	return(get_entry(map, i)->data->dpme_pblock_start);
}

int
test_partition_lite(partition_map_header *map, char *dev, int part)
{
	off_t o;
	u32 b;
	int fdd, fdp;
	char pname[256];
	char buf1, buf2;
	int c;
	int mismatch;

	if(strlen(dev) > 253)
	{
		printf("bad device: %s\n", dev);
		return(1);
	}

	if(part > 16)
	{
		printf("bad partition: %s%d\n", dev, part);
		return(1);
	}

	sprintf(pname, "%s%d", dev, part);

	b = getpartstart(map, part);
	o = (off_t)b * SECTOR_SIZE;
	if(verbose)
	{
		printf("partition %d: block %lu offset %llu\n", part, b, o);
	}

	mismatch = 0;

	fdd = open(dev, O_RDWR);
	if(fdd == -1)
	{
		printf("can't open %s: %s\n", dev, strerror(errno));
		return(1);
	}
	
	fdp = open(pname, O_RDONLY);
	if(fdp == -1)
	{
		printf("can't open %s: %s\n", pname, strerror(errno));
		return(1);
	}
	lseek(fdd, o, SEEK_SET);
	lseek(fdp, 0, SEEK_SET);
	if(read(fdd, &buf1, 1) != 1) return(1);
	if(read(fdp, &buf2, 1) != 1) return(1);
	if(buf1 != buf2)
	{
		printf("PARTITION DATA MISMATCH: partition %d\n", part);
		return(1);
	}
	close(fdd);
	close(fdp);
	return(mismatch ? 2 : 0);
}

void
tpusage(char *n)
{
	printf("usage: %s [ drive ]\n", n);
	exit(1);
}


unsigned long
get_blocks(char *drv)
{
	int fd;
	unsigned long length;

	fd = open(drv, O_RDONLY);
	if(fd == -1) return(0);
        if (ioctl(fd, BLKGETSIZE, &length) < 0) return(0);
	close(fd);
	return(length);
}

int
main(int argc, char **argv)
{
	partition_map_header *map;
	partition_map *m;
	int valid_file = 0;
	int n;
	int done = 0;
	unsigned long swapsize = 128;
	char *endptr;
	char defdrv[] = "/dev/hda";
	char *drv = defdrv;
	int ret = 0;
	unsigned long length;

	while(! done)
	{
		n = getopt(argc, argv, "h");
		switch(n)
		{
			case -1:
				done = 1;
				break;
			case ':':
			case '?':
			case 'h':
			default:
				tpusage(argv[0]);
				break;
		}
	}

	if(argv[optind])
		drv = argv[optind];

	length = get_blocks(drv);
	if(length == 0)
	{
		printf("fatal: can't get size of %s\n", drv);
		return(1);
	}
	printf("%s: drive %s is %ld blocks\n", argv[0], drv, length);

	map = open_partition_map(drv, &valid_file, 0);
	if(! valid_file || ! map)
	{
		printf("fatal: invalid MAC partition table: %s\n", drv);
		return(1);
	}

	/* see if we can't access any partition (exit |= 1)
	 * see if any partition ends above the LBA28 limit (exit |= 2)
	 */

	for(n = 1 ; n < 17 ; ++n)
	{
		unsigned long e;
		partition_map *m;

		m = find_entry_by_disk_address(n, map);
		if(! m) continue;

		/* allow users who are underutilizing their >137GB drive
		 * to pass
		 */
		if(! strcmp(m->data->dpme_name, "Extra")) continue;

		e = getpartstart(map, n) + getpartsize(map, n) - 1;

		if(e >= (1 << 28))
		{
			printf("LBA48: partition %d ends at %ld\n", n, e);
			ret |= 2;
		} else {
			printf("NORMAL: partition %d ends at %ld\n", n, e);
		}

		if(e >= length)
		{
			printf("BAD: partition %d ends at %ld, "
				"drive ends at %d\n", n, e, length);
			ret |= 1;
		}
	}

	printf("%s: returning %d\n", argv[0], ret);

	return(ret);
}
