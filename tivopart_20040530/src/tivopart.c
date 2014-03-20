#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <linux/hdreg.h>
#include <linux/fs.h>
#include <linux/blkpg.h>
#include "partition_map.h"
#include "dump.h"

#define SECTOR_SIZE 512

#define MIN_ROOTBLOCKS 262144

#define NPART 16
#define MAIN_ON_HDA1

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

static inline void
setpart(partition_map_header *map, int i, u32 start, u32 len)
{
	partition_map *m = get_entry(map, i);

	m->data->dpme_pblock_start = start;
	m->data->dpme_pblocks = m->data->dpme_lblocks = len;
	return;
}

static inline void
setdpistring(char *dst, char *src)
{
	memset(dst, 0, sizeof(DPISTRLEN));
	strncpy(dst, src, DPISTRLEN - 1);
}

static inline int
checkpartlen(partition_map_header *map, int i, u32 len)
{
	partition_map *m = get_entry(map, i);

	if(m->data->dpme_pblocks != len)
	{
		printf("error: partition %d is not size %lu\n", i, len);
		if(force) return(0);
		return(1);
	}
	return(0);
}

int
get_lastpart(partition_map_header *map)
{
	int c;
	partition_map *m;

	for(c = 1 ; c <= NPART ; ++c)
	{
		m = find_entry_by_disk_address(c, map);
		if(! m) return(0);
		if(! strcmp(m->data->dpme_name, "Extra"))
			return(c);
		if(! strcmp(m->data->dpme_name, "Main root"))
			return(c);
	}
	return(0);
}

/*
 * blkpg_* functions
 *
 * these manipulate the in-memory (only) partition structures in the kernel
 * we use them to "revalidate" (reread) the partition table without rebooting
 *
 * partitions are numbered starting at 1
 */

int
blkpg_part_del(int fd, int partition)
{
	struct blkpg_ioctl_arg a;
	struct blkpg_partition pt;

	pt.pno = partition;
	pt.start = 0;
	pt.length = 0;
	pt.devname[0] = '\0';
	pt.volname[0] = '\0';
	a.op = BLKPG_DEL_PARTITION;
	a.flags = 0;
	a.datalen = sizeof(pt);
	a.data = &pt;

	return(ioctl(fd, BLKPG, &a));
}

int
blkpg_part_add(int fd, int partition, u32 start, u32 len)
{
	struct blkpg_ioctl_arg a;
	struct blkpg_partition pt;

	pt.pno = partition;
	pt.start = 0x200 * (long long)start;
	pt.length = 0x200 * (long long)len;
	pt.devname[0] = '\0';
	pt.volname[0] = '\0';
	a.op = BLKPG_ADD_PARTITION;
	a.flags = 0;
	a.datalen = sizeof(pt);
	a.data = &pt;

	return(ioctl(fd, BLKPG, &a));
}

int
revalidate_drive(char *dev, partition_map_header *map)
{
	int c;
	int fd;
	int ret = 0;
	partition_map *m;

	fd = open(dev, O_RDONLY);
	if(fd == -1)
	{
		printf("can't open %s: %s\n", dev, strerror(errno));
		return(-1);
	}

	for(c = 1 ; c <= NPART ; ++c)
		blkpg_part_del(fd, c);

	for(c = 1 ; c <= NPART ; ++c)
	{
		m = find_entry_by_disk_address(c, map);
		if(m)
		{
			if(blkpg_part_add(fd, c, getpartstart(map, c),
				getpartsize(map, c)) == -1)
			{
				printf("error revalidating %s%d: %s\n",
					dev, c, strerror(errno));
				ret = -1;
			} else {
				if(verbose)
					printf("revalidating %s%d: success\n",
						dev, c);
			}
		}
	}

	close(fd);

	return(ret);
}

/* consolidate(): rearranges the partition layout
 *
 * original layout:
 *
 * hda1:     63 ( 32K) Apple (partition map)
 * hda2:   4096 (  2M) Bootstrap 1
 * hda3:   4096 (  2M) Kernel 1
 * hda4: 262144 (128M) Root 1
 * hda5:      1 ( 512) Bootstrap 2
 * hda6:   8192 (  4M) Kernel 2
 * hda7: 262144 (128M) Root 2
 * hda8: ?????? (????) Linux swap  (varies)
 * ...
 * hda?: ?????? (????) Extra
 *
 * new layout:
 *
 * hda1: ?????? (????) Main root    (the root fs that actually boots)
 * hda2:   8192 (  4M) Decoy kernel (signed but compromised kernel)
 * hda3:   8192 (  4M) Kernel 1
 * hda4: 262144 (256M) Root 1
 * hda5:   8192 (  4M) Decoy root   (decoy root to satisfy initrd checks)
 * hda6:   8192 (  4M) Kernel 2
 * hda7: 262144 (256M) Root 2
 * hda8: ?????? (????) Linux swap   (custom size)
 *
 * by varying the size of the -s parameter to mfsrestore, the amount of
 * free space in the user's custom root partition may be increased
 */

#define BLK_HDA2 8192
#define BLK_HDA3 8192
#define BLK_HDA4 524288
#define BLK_HDA5 8192
#define BLK_HDA6 8192
#define BLK_HDA7 524288

#define BLK_FIX_APPLEFREE 8192

int
consolidate(partition_map_header *map, u32 swapblocks)
{
	u32 totalblocks;
	u32 curblock;
	u32 mainrootblocks;
	int c;
	struct dpme *mainroot = NULL;
	int mainroot_num;
	int last;
	int last_is_in_pool = 0;

	last = get_lastpart(map);
#ifdef MAIN_ON_HDA1
	mainroot_num = 1;
#else /* MAIN_ON_HDA1 */
	mainroot_num = last;
#endif /* MAIN_ON_HDA1 */

	if(last == 0)
	{
		printf("fatal: can't find Extra/Main root partition\n");
		return(1);
	}
	mainroot = get_entry(map, mainroot_num)->data;

	totalblocks = 0;

	/* check that partitions 2-8 are contiguous, and sum the lengths
	 * to compute the pool size */
	curblock = getpartstart(map, 2);
	for(c = 2 ; c <= 8 ; ++c)
	{
		u32 s = getpartsize(map, c);
		if(curblock != getpartstart(map, c))
		{
			printf("error: partitions %d and %d are not "
				"contiguous\n", c - 1, c);
			if(! force) return(1);
		}
		curblock += s;
		totalblocks += s;
	}

	/* see if the "last" partition is part of the pool */
	if(getpartstart(map, last) == curblock)
	{
		u32 s = getpartsize(map, last);
		curblock += s;
		totalblocks += s;
		last_is_in_pool = 1;
	}

	/* sanity checks */

	mainrootblocks = totalblocks - (BLK_HDA2 + BLK_HDA3 + BLK_HDA4 +
		BLK_HDA5 + BLK_HDA6 + BLK_HDA7 + swapblocks);

	if(last_is_in_pool && (last != 1))
	{
		/* 8192 blocks are allocated to reconstruct the
		 * Apple_Free partition we stole in a previous invocation
		 */
		mainrootblocks -= BLK_FIX_APPLEFREE;
	}
	
	if((mainrootblocks < MIN_ROOTBLOCKS) || (mainrootblocks > totalblocks))
	{
		printf("error: not enough space amongst hda2-hda8 "
			"(total %ld)\n", totalblocks);
		if(! force) return(1);
	}

	/* do the allocation */

	curblock = getpartstart(map, 2);

	/* reconstruct the Apple_Free partition if it was previously used */
	if(last_is_in_pool && (last != 1))
	{
		setdpistring(get_entry(map, last)->data->dpme_name, "Extra");
		setdpistring(get_entry(map, last)->data->dpme_type,
			"Apple_Free");
		setpart(map, last, curblock, BLK_FIX_APPLEFREE);
		curblock += BLK_FIX_APPLEFREE;
		totalblocks -= BLK_FIX_APPLEFREE;
	}

	setpart(map, 2, curblock, BLK_HDA2);
	curblock += BLK_HDA2; totalblocks -= BLK_HDA2;

	setpart(map, 3, curblock, BLK_HDA3);
	curblock += BLK_HDA3; totalblocks -= BLK_HDA3;

	setpart(map, 4, curblock, BLK_HDA4);
	curblock += BLK_HDA4; totalblocks -= BLK_HDA4;

	setpart(map, 5, curblock, BLK_HDA5);
	curblock += BLK_HDA5; totalblocks -= BLK_HDA5;

	setpart(map, 6, curblock, BLK_HDA6);
	curblock += BLK_HDA6; totalblocks -= BLK_HDA6;

	setpart(map, 7, curblock, BLK_HDA7);
	curblock += BLK_HDA7; totalblocks -= BLK_HDA7;

	setpart(map, 8, curblock, swapblocks);
	curblock += swapblocks; totalblocks -= swapblocks;

	setpart(map, mainroot_num, curblock, mainrootblocks);
	curblock += mainrootblocks; totalblocks -= mainrootblocks;

#ifndef MAIN_ON_HDA1
	setdpistring(get_entry(map, 1)->data->dpme_name, "ROMFS");
	setdpistring(get_entry(map, 1)->data->dpme_type, "Image");
#endif /* MAIN_ON_HDA1 */
	setdpistring(get_entry(map, 2)->data->dpme_name, "Decoy kernel");
	setdpistring(get_entry(map, 2)->data->dpme_type, "Image");
	setdpistring(get_entry(map, 5)->data->dpme_name, "Decoy root");
	setdpistring(get_entry(map, 5)->data->dpme_type, "Ext2");
	setdpistring(mainroot->dpme_name, "Main root");
	setdpistring(mainroot->dpme_type, "Ext2");

	write_partition_map(map);

	return(0);
}

int
test_partition(partition_map_header *map, char *dev, int part)
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

	if(part > NPART)
	{
		printf("bad partition: %s%d\n", dev, part);
		return(1);
	}

	sprintf(pname, "%s%d", dev, part);

	b = getpartstart(map, part);
	o = (off_t)b * SECTOR_SIZE;
	if(verbose)
	{
		printf("partition %d: block %lu offset %lu\n", part, b, o);
	}


	/* write to the raw disk and make sure the first byte of the partition
	 * reflects the change
	 */

	mismatch = 0;

	for(c = 0 ; c < 2 ; ++c)
	{
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
			printf("PARTITION DATA MISMATCH: please reboot to "
				"refresh the kernel's partition data\n");
			if(c == 0) return(2);
			mismatch = 1;
		} else {
			if(verbose)
				printf("partition data matches: pass %d\n",
					c);
		}
		buf1 ^= 0xff;
		lseek(fdd, o, SEEK_SET);
		if(write(fdd, &buf1, 1) != 1)
		{
			printf("error writing to disk");
			if(c) printf(" - data may be corrupted!\n");
				else printf("\n");
			return(1);
		}
		fsync(fdd);
		close(fdd);
		close(fdp);
	}
	return(mismatch ? 2 : 0);
}

void
tpusage(char *n)
{
	printf("usage: %s [ options ] <command> <arguments>\n", n);
	printf("\noptions:\n");
	printf("\t-h                       print this help message\n");
	printf("\t-f                       ignore errors and FORCE the operation\n");
	printf("\t-s <nnn>                 use nnn megs of swap (default 128)\n");
	printf("\t-v                       verbose output\n");
	printf("\ncommands:\n");
	printf("\td[ump] <drive>           dump the partition table to the screen\n");
	printf("\tc[onsolidate] <drive>    rearrange hdX4..hdX8 to produce a bigger hdX14/hdX16\n");
	printf("\tl[astpart] <drive>       return the number of the last used partition\n");
	printf("\tr[evalidate] <drive>     update the kernel's in-memory partition map\n");
	printf("\tt[test] <drive> <part#>  test to see if the kernel's cache of\n");
	printf("\t                         partition part#'s offset is consistent with\n");
	printf("\t                         the disklabel\n");
	printf("\n");
	printf("WARNING! This program rewrites your partition table! If "
		"you do not know what\nyou are doing you will probably lose "
		"data! This program comes with NO WARRANTY\nso if you screw "
		"up your drive it is your OWN DAMN FAULT!!\n");
	printf("\nThis program should NOT be used while the drive is in use "
		"or while any\npartition on the drive is mounted.\n");
	exit(1);
}


int
main(int argc, char **argv)
{
	partition_map_header *map;
	int valid_file = 0;
	char *dev = NULL;
	int n;
	int done = 0;
	unsigned long swapsize = 128;
	char *endptr;

	while(! done)
	{
		n = getopt(argc, argv, "fvhs:");
		switch(n)
		{
			case ':':
			case '?':
			case 'h':
				tpusage(argv[0]);
				break;
			case 'f':
				force = 1;
				break;
			case 'v':
				verbose = 1;
				break;
			case 's':
				swapsize = strtoul(optarg, &endptr, 0);
				if(*endptr != 0)
				{
					printf("fatal: bad swap param %s\n",
						optarg);
					return(1);
				}
				break;
			case -1:
				done = 1;
				break;
		}
	}

	if(! argv[optind])
	{
		tpusage(argv[0]);
	}

	/* look at commands which do not take the drive as an arg */

	switch(argv[optind][0])
	{
		case 'h':
			tpusage(argv[0]);
			break;
	}

	if(! argv[optind + 1])
	{
		tpusage(argv[0]);
	}

	dev = argv[optind + 1];

	map = open_partition_map(dev, &valid_file, 0);
	if(! valid_file || ! map)
	{
		printf("fatal: invalid drive: %s\n", dev);
		return(1);
	}

	/* look at commands which take the drive as the first arg */

	switch(argv[optind][0])
	{
		case 'd':
			dump_partition_map(map, 1);
			return(0);
			break;
		case 'c':
			if(verbose)
			{
				printf("*** BEFORE ***\n");
				dump_partition_map(map, 1);
			}
			if(consolidate(map, swapsize * 1048576 / SECTOR_SIZE))
			{
				printf("consolidation FAILED\n");
				return(1);
			}
			if(verbose)
			{
				printf("*** AFTER ***\n");
				dump_partition_map(map, 1);
			}
			printf("consolidation was a success!\n");

			if(revalidate_drive(dev, map) == -1)
			{
				printf("revalidation failed: reboot to "
					"use the new map\n");
				return(1);
			}
			return(0);
			break;
		case 'l':
			n = get_lastpart(map);
			printf("%d\n", n);
			return(n);
			break;
		case 'r':
			if(revalidate_drive(dev, map) == -1)
			{
				printf("revalidation failed\n");
				return(1);
			} else {
				return(0);
			}
			break;
		case 't':
			if(! argv[optind + 2])
				tpusage(argv[0]);
			if(test_partition(map, dev,
				atoi(argv[optind + 2])))
			{
				printf("partition test failed\n");
				return(1);
			} else {
				printf("partition test succeeded!\n");
				return(0);
			}
			break;
	}

	tpusage(argv[0]);
	return(1);
}
