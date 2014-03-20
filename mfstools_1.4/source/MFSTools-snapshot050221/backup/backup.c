#ifdef HAVE_CONFIG_H
#include <config.h>
#endif
#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#if HAVE_MALLOC_H
#include <malloc.h>
#endif
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#if HAVE_SYS_MALLOC_H
#include <sys/malloc.h>
#endif
#include <sys/types.h>
#ifdef HAVE_ASM_TYPES_H
#include <asm/types.h>
#endif
#include <fcntl.h>
#include <zlib.h>
#include <string.h>
#ifdef HAVE_LINUX_FS_H
#include <linux/fs.h>
#endif
#include <ctype.h>
/* For htonl() */
#include <netinet/in.h>

#include "mfs.h"
#include "macpart.h"
#include "backup.h"

struct blocklist
{
	int backup;
	unsigned int sector;
	struct blocklist *next;
};

/***************************************************************************/
/* Allocate a block from the given block pool.  If the pool is dry, calloc */
/* it.  Either way, the returned block shall be zeroed out. */
static struct blocklist *
alloc_block (struct blocklist **pool)
{
	struct blocklist *newblock = *pool;

	if (newblock)
	{
		*pool = newblock->next;
		newblock->sector = 0;
		newblock->backup = 0;
		newblock->next = 0;
	}
	else
	{
		newblock = calloc (sizeof (*newblock), 1);
	}

	return newblock;
}

/*******************************/
/* Return a block to the pool. */
static void
free_block (struct blocklist **pool, struct blocklist *block)
{
	block->next = *pool;
	*pool = block;
}

/***********************************************************************/
/* Free an entire list of blocks.  This can be used to cleanup a pool. */
static void
free_block_list (struct blocklist **blocks)
{
	while (*blocks)
	{
		struct blocklist *tmp = *blocks;
		*blocks = tmp->next;
		free (tmp);
	}
}

/*********************************/
/* Free an array of block lists. */
static void
free_block_list_array (struct blocklist **blocks)
{
	while (*blocks)
	{
		free_block_list (blocks);
		blocks++;
	}
}

/***********************************************************************/
/* Concatenates an array of block lists into a single list.  This list */
/* should be read or free only.  Any write to it will likely confuse the */
/* add function. */
static struct blocklist *
block_list_array_concat (struct blocklist **blocks)
{
	struct blocklist *res = NULL;
	struct blocklist **last = &res;

	while (*blocks)
	{
		*last = *blocks;

		while (*last)
		{
			last = &(*last)->next;
		}
		*blocks = 0;
		blocks++;
	}

	return res;
}

/************************************************/
/* Add a block to the list of blocks to backup. */
static int
backup_add_block (struct blocklist **blocks, unsigned int *partstart, struct blocklist **pool, int sector, int count)
{
	struct blocklist **loop;
	struct blocklist *prev = 0;

	while (partstart[1] <= sector)
	{
		partstart++;
		blocks++;
	}

/* A little debug here and there never hurt anything. */
#if DEBUG
	fprintf (stderr, "Adding block %d of %d from volume at %d\n", sector, count, partstart[0]);
#endif

/* Find where in the list this block fits.  This will return with &loop */
/* pointing to the first block with a sector number greater than the new */
/* block. */
	for (loop = blocks; *loop && (*loop)->sector < sector; loop = &((*loop)->next))
	{
		prev = *loop;
	}

	if (!*loop)
	{
/* There are no blocks prior to this one, and it doesn't butt up against */
/* the end of the list. */
		struct blocklist *newblock;
		newblock = alloc_block (pool);

		if (!newblock)
		{
			return -1;
		}

/* And one more.  Since this is the end of the list, this tail block is */
/* to indicate the size of this block. */

		newblock->next = alloc_block (pool);

		if (!newblock->next)
		{
			free (newblock);
			return -1;
		}

		newblock->backup = 1;
		newblock->sector = sector;

		newblock->next->next = *loop;
		newblock->next->sector = sector + count;

/* Insert the block at the position found. */
		*loop = newblock;
	}
	else if ((*loop)->backup)
	{
/* This block fits in a space currently set not to backup. */
		if (sector + count >= (*loop)->sector)
		{
/* In fact, it butts right up against the next backup block.  Merely extend */
/* that block to include up to the new sector. */
			(*loop)->sector = sector;
		}
		else
		{
/* This block is in the middle of a block set to not backup.  That means this */
/* block will have to be split into 3 parts, 2 of them new. */
			struct blocklist *newblock;
			newblock = alloc_block (pool);

			if (!newblock)
			{
				return -1;
			}

/* Allocate the second new block, that will take care of the unbacked up */
/* space between this backed up space and the next. */
			newblock->next = alloc_block (pool);

			if (!newblock->next)
			{
				free (newblock);
				return -1;
			}

			newblock->backup = 1;
			newblock->sector = sector;

			newblock->next->next = *loop;
			newblock->next->sector = sector + count;

/* Insert it into the current location. */
			*loop = newblock;
		}
	}
	else
	{
/* The next block the new one is not after is not set to be backed up. */
/* The only way this could happen is either because no blocks exist yet, */
/* or the new block is at the beginning of an unbacked up section, or no */
/* blocks before it are backed up. */
		if (prev && (*loop)->sector < sector + count)
		{
/* This is not the first block, and it extends the previous backed up block. */
			(*loop)->sector = sector + count;
		}
		else if (!prev)
		{
/* This is the first block in the list.  Split the initial non-backup block. */
			struct blocklist *newblock;
			newblock = alloc_block (pool);

			if (!newblock)
			{
				return -1;
			}

			newblock->next = alloc_block (pool);

			if (!newblock->next)
			{
				free (newblock);
				return -1;
			}

			newblock->backup = 1;
			newblock->sector = sector;

			newblock->next->sector = sector + count;
			newblock->next->next = (*loop)->next;;

/* Insert this block at the beginning of the list.  Also trick out the loop */
/* variable, since it is expected to point to the newly adjusted block to be */
/* backed up. */
			*loop = newblock;
			loop = &newblock->next;
		}
		else
		{
/* These ifs don't cover all cases...  But no other cases should occur.  If */
/* they do, it is an error. */
			return -1;
		}
	}

/* At this point it is possible for a block to overlap the next block. */
	while ((*loop)->next && ((*loop)->next->sector < (*loop)->sector || (*loop)->next->backup == (*loop)->backup))
	{
/* If there is overlap, or if the next block has the same backup/don't backup */
/* status, just drop the next block. */
		struct blocklist *oldblock = (*loop)->next;
		(*loop)->next = oldblock->next;
		free_block (pool, oldblock);
	}

	return 0;
}

/***********************************************************/
/* Create the main data structure for holding backup info. */
static int
add_blocks_to_backup_info (struct backup_info *info, struct blocklist *blocks)
{
	struct blocklist *loop;
	int count = 0;

	if (!info)
	{
		return -1;
	}

/* Count how many blocks there are. */
	for (loop = blocks; loop; loop = loop->next)
	{
		if (loop->backup)
		{
			info->nblocks++;
		}
	}

/* Allocate space for the backup blocks in the portable format. */
	info->blocks = calloc (sizeof (struct backup_block), info->nblocks);

	if (!info->blocks)
	{
		info->err_msg = "Memory exhausted";
		return -1;
	}

/* Copy the temporary list to the final block list. */
	for (loop = blocks; loop; loop = loop->next)
	{
		if (loop->backup)
		{
			info->blocks[count].firstsector = loop->sector;
			info->blocks[count].sectors = loop->next->sector - loop->sector;
			info->nsectors += info->blocks[count].sectors;
			count++;
		}
	}
	return 0;
}

/*****************************************************************/
/* Scan the inode table and generate a list of blocks to backup. */
static struct blocklist *
scan_inodes (struct backup_info *info)
{
	unsigned int loop, loop2, loop3;
	int ninodes = mfs_inode_count (info->mfs);
	struct blocklist *blocks[32];
	unsigned int partstart[32];
	struct blocklist *pool = NULL;
	unsigned int highest = 0;

	bzero (blocks, sizeof (blocks));

	for (loop = 0, loop3 = 0; (loop2 = mfs_volume_size (info->mfs, loop)); loop += loop2, loop3++)
	{
		partstart[loop3] = loop;
		blocks[loop3] = calloc (sizeof (**blocks), 1);
		if (!blocks[loop3])
		{
			while (loop3 > 0)
			{
				loop3--;
				free (blocks[loop3]);
				info->err_msg = "Memory exhausted";
				return 0;
			}
		}
	}
	partstart[loop3] = ~0;
	blocks[loop3] = 0;

/* Add inodes. */
	for (loop = 0; loop < ninodes; loop++)
	{
		mfs_inode *inode = mfs_read_inode (info->mfs, loop);

		if (inode)
		{
/* If it a stream, treat it specially. */
			if (inode->type == tyStream)
			{
				unsigned int streamsize;

				if (info->back_flags & (BF_THRESHTOT | BF_STREAMTOT))
					streamsize = htonl (inode->blocksize) / 512 * htonl (inode->size);
				else
					streamsize = htonl (inode->blocksize) / 512 * htonl (inode->blockused);

/* Only backup streams that are smaller than the threshhold. */
				if (streamsize > 0 && (((info->back_flags & BF_THRESHSIZE) && streamsize < info->thresh) || (!(info->back_flags & BF_THRESHSIZE) && htonl (inode->fsid) <= info->thresh)))
				{
/* Add all blocks. */

					if ((info->back_flags & (BF_THRESHTOT | BF_STREAMTOT)) == BF_THRESHTOT)
						streamsize = htonl (inode->blocksize) / 512 * htonl (inode->blockused);

					for (loop2 = 0; loop2 < htonl (inode->numblocks); loop2++)
					{
						unsigned int thiscount = htonl (inode->datablocks[loop2].count);
						unsigned int thissector = htonl (inode->datablocks[loop2].sector);

						if (thiscount > streamsize)
							thiscount = streamsize;

#if DEBUG
						fprintf (stderr, "Inode %d: ", htonl (inode->fsid));
#endif

						if (backup_add_block (blocks, partstart, &pool, thissector, thiscount) != 0)
						{
							free_block_list_array (blocks);
							free_block_list (&pool);
							free (inode);
							info->err_msg = "Memory exhausted";
							return 0;
						}
						streamsize -= thiscount;

						if (streamsize == 0)
							break;

						if (highest < thiscount + thissector)
							highest = thissector + thiscount;
					}

				}
			}
			else
			{
				for (loop2 = 0; loop2 < htonl (inode->numblocks); loop2++)
				{
					unsigned int thiscount = htonl (inode->datablocks[loop2].count);
					unsigned int thissector = htonl (inode->datablocks[loop2].sector);

					if (highest < thiscount + thissector)
					{
						highest = thiscount + thissector;
					}
				}
			}
			free (inode);
		}
	}

// Make sure all needed data is present.
	if (info->back_flags & BF_TRUNCATED)
	{
		unsigned set_size = mfs_volume_set_size (info->mfs);
		if (highest > set_size)
		{
			info->err_msg = "Required data at %ld beyond end of the device (%ld)";
			info->err_arg1 = (void *)highest;
			info->err_arg2 = (void *)set_size;

			free_block_list_array (blocks);
			free_block_list (&pool);

			return 0;
		}
	}

	if (info->back_flags & BF_SHRINK)
	{
		zone_header *hdr = 0;

		while ((hdr = mfs_next_zone (info->mfs, hdr)) != 0)
		{
#if DEBUG
			fprintf (stderr, "Checking zone at %ld of type %d for region %ld-%ld\n", htonl (hdr->sector), htonl (hdr->type), htonl (hdr->first), htonl (hdr->last));
#endif
			if (htonl (hdr->type) != ztMedia)
			{
				if (htonl (hdr->sector) + htonl (hdr->length) > highest)
					highest = htonl (hdr->sector) + htonl (hdr->length);
				if (htonl (hdr->last) > highest)
					highest = htonl (hdr->last);
			}
		}
	}

/* Put in the whole volumes. */
	for (loop = 0, loop3 = 1; (loop2 = mfs_volume_size (info->mfs, loop)); loop += loop2, loop3 ^= 1)
	{
		if (loop3)
		{
			if ((info->back_flags & BF_SHRINK) && loop >= highest)
			{
#if DEBUG
				fprintf (stderr, "Truncating MFS at %d\n", loop);
#endif
				break;
			}
			if (backup_add_block (blocks, partstart, &pool, loop, loop2) != 0)
			{
				free_block_list_array (blocks);
				free_block_list (&pool);
				info->err_msg = "Memory exhausted";
				return 0;
			}
		}
		if (info->back_flags & BF_SHRINK)
			info->nmfs++;
	}

/* Free the data. */
	free_block_list (&pool);

	return block_list_array_concat (blocks);
}

/***********************************************************/
/* Queries the mfs code for the list of partitions in use. */
static int
add_mfs_partitions_to_backup_info (struct backup_info *info)
{
	char *mfs_partitions;
	int loop;
	unsigned int cursector = 0;

	mfs_partitions = mfs_partition_list (info->mfs);

	if (info->nmfs == 0)
	{
/* First count the number of partitions. */
		loop = 0;
		while (mfs_partitions[loop])
		{
			info->nmfs++;
			while (mfs_partitions[loop] && !isspace (mfs_partitions[loop]))
			{
				loop++;
			}
			while (mfs_partitions[loop] && isspace (mfs_partitions[loop]))
			{
				loop++;
			}
		}
	}

	info->mfsparts = calloc (sizeof (struct backup_partition), info->nmfs);

	if (!info->mfsparts)
	{
		info->nmfs = 0;
		info->err_msg = "Memory exhausted";
		return -1;
	}

/* This loop looks almost the same as the last one...  Except this time, */
/* it actually fills out the structures. */
	for (loop = 0; loop < info->nmfs; loop++)
	{
		if (strncmp (mfs_partitions, "/dev/hd", 7))
		{
			free (info->mfsparts);
			info->mfsparts = 0;
			info->nmfs = 0;
			info->err_msg = "Bad partition name (%.*s) in partition list";
			info->err_arg1 = (void *)strcspn (mfs_partitions, " ");
			info->err_arg2 = mfs_partitions;
			return -1;
		}

/* Since the TiVo only supports 2 IDE devices, assume hda=dev 0, hdb=dev 1. */
		switch (mfs_partitions[7])
		{
		case 'a':
			info->mfsparts[loop].devno = 0;
			break;
		case 'b':
			info->mfsparts[loop].devno = 1;
			break;
		default:
			info->err_msg = "Bad partition name (%.*s) in partition list";
			info->err_arg1 = (void *)strcspn (mfs_partitions, " ");
			info->err_arg2 = mfs_partitions;
			free (info->mfsparts);
			info->mfsparts = 0;
			info->nmfs = 0;
			return -1;
		}

/* Find the partition number from the device name. */
		info->mfsparts[loop].partno = strtoul (mfs_partitions + 8, &mfs_partitions, 10);

/* If there are other non-space characters after the number, thats a problem. */
		if (*mfs_partitions && !isspace (*mfs_partitions))
		{
			info->err_msg = "Bad partition name (%.*s) in partition list";
			info->err_arg1 = (void *)strcspn (mfs_partitions, " ");
			info->err_arg2 = mfs_partitions;
			free (info->mfsparts);
			info->mfsparts = 0;
			info->nmfs = 0;
			return -1;
		}

/* Get the size of this partition from MFS.  This may vary slightly from the */
/* real partition size.  But thats okay, this means a little space can be */
/* saved during the restore, if the partition table is re-created from */
/* scratch. */
		info->mfsparts[loop].sectors = mfs_volume_size (info->mfs, cursector);
		if (info->mfsparts[loop].sectors == 0)
		{
			info->err_msg = "Empty MFS partition %.*s";
			info->err_arg1 = (void *)strcspn (mfs_partitions, " ");
			info->err_arg2 = mfs_partitions;
			free (info->mfsparts);
			info->mfsparts = 0;
			info->nmfs = 0;
			return -1;
		}

/* Add this partition into the current running total. */
		cursector += info->mfsparts[loop].sectors;

/* Find the beginning of the next partition. */
		while (*mfs_partitions && isspace (*mfs_partitions))
		{
			mfs_partitions++;
		}
	}

	return 0;
}

/**********************************************************************/
/* Add the regular partitions to the backup info.  This only backs up */
/* partitions 1 (Partition table) and one of 2/3/4 or 5/6/7 (One of the */
/* bootstrap/kernel/root sets) and 9 (/var) - 8 is skipped because it can */
/* easily be re-created.  It is, after all, just swap space.  Nothing else */
/* is supported. */
static int
add_partitions_to_backup_info (struct backup_info *info, char *device)
{
	int loop;
	char bootsector[512];
	int rootdev;
	char *tmpc;

/* Four.  Always Four.  Or three. */
	if (info->back_flags & BF_BACKUPVAR)
	{
		info->nparts = 4;
	}
	else
	{
		info->nparts = 3;
	}
/* One.  No more, no less. */
	info->ndevs = 1;

	info->devs = calloc (sizeof (struct device_info), info->ndevs);
	if (!info->devs)
	{
		info->ndevs = 0;
		info->nparts = 0;
		info->err_msg = "Memory exhausted";
		return -1;
	}

	info->parts = calloc (sizeof (struct backup_partition), info->nparts);
	if (!info->parts)
	{
		info->nparts = 0;
		info->ndevs = 0;
		free (info->devs);
		info->err_msg = "Memory exhausted";
		return -1;
	}

	info->devs[0].devname = device;
	info->devs[0].nparts = tivo_partition_count (device);

/* 9 is the minimum number of devices needed. */
	if (info->devs[0].nparts < 9)
	{
		free (info->devs);
		free (info->parts);
		info->ndevs = 0;
		info->nparts = 0;
		info->err_msg = "Not enough partitions on source drive";
		return -1;
	}

	info->devs[0].files = calloc (sizeof (tpFILE *), info->devs[0].nparts);

	if (info->devs[0].files == NULL)
	{
		free (info->devs);
		free (info->parts);
		info->ndevs = 0;
		info->nparts = 0;
		info->err_msg = "Memory exhausted";
		return -1;
	}

	if (tivo_partition_read_bootsector (device, &bootsector) <= 0)
	{
		free (info->devs[0].files);
		free (info->devs);
		free (info->parts);
		info->ndevs = 0;
		info->nparts = 0;
		info->err_msg = "Error reading boot sector of source drive";
		return -1;
	}

	if (bootsector[2] != 3 && bootsector[2] != 6)
	{
		free (info->devs[0].files);
		free (info->devs);
		free (info->parts);
		info->ndevs = 0;
		info->nparts = 0;
		info->err_msg = "Can not determine primary boot partition from boot sector";
		return -1;
	}

	rootdev = bootsector[2] + 1;

/* Scan boot sector for root device.  2.5 seems to need this. */
	tmpc = &bootsector[4];
	while (tmpc && *tmpc && strncmp (tmpc, "root=/dev/hda", 13))
	{
		tmpc = strchr (tmpc, ' ');
		if (tmpc)
			tmpc++;
	}

	if (*tmpc)
	{
		if (((tmpc[13] == '4' || tmpc[13] == '7') && tmpc[14] == 0) || isspace (tmpc[14]))
		{
			rootdev = tmpc[13] - '0';
#if DEBUG
			fprintf (stderr, "Using root partition %d from boot sector.\n", rootdev);
#endif
		}
	}

	info->parts[0].partno = bootsector[2] - 1;
	info->parts[0].devno = 0;
	info->parts[1].partno = bootsector[2];
	info->parts[1].devno = 0;
	info->parts[2].partno = rootdev;
	info->parts[2].devno = 0;
	if (info->nparts > 3)
	{
		info->parts[3].partno = 9;
		info->parts[3].devno = 0;
	}

	for (loop = 0; loop < info->nparts; loop++)
	{
		tpFILE *file;

		file = tivo_partition_open_direct (device, info->parts[loop].partno, O_RDONLY);

		if (!file) {
			while (loop-- > 0)
			{
				tivo_partition_close (info->devs[0].files[(int)info->parts[loop].partno]);
			}
			free (info->devs[0].files);
			free (info->devs);
			free (info->parts);
			info->ndevs = 0;
			info->nparts = 0;
			info->err_msg = "Error opening partition %s%d";
			info->err_arg1 = device;
			info->err_arg2 = (void *)(unsigned)info->parts[loop].partno;
			return -1;
		}

		info->devs[0].files[(int)info->parts[loop].partno] = file;
		info->parts[loop].sectors = tivo_partition_size (file);
		info->nsectors += info->parts[loop].sectors;
	}

	return 0;
}

/*************************************/
/* Initializes the backup structure. */
struct backup_info *
init_backup (char *device, char *device2, int flags)
{
 	struct backup_info *info;

	flags &= BF_FLAGS;

 	if (!device)
 		return 0;
 
 	info = calloc (sizeof (*info), 1);
 
 	if (!info)
 	{
 		return 0;
 	}

	info->mfs = mfs_init (device, device2, O_RDONLY);
 
 	info->back_flags = flags;

	info->thresh = 2000;

	if (!tivo_partition_swabbed (device))
		info->back_flags |= BF_NOBSWAP;

	info->hda = strdup (device);
	if (!info->hda)
	{
		info->err_msg = "Memory exhausted";
	}

	if (!mfs_has_error (info->mfs))
	{
		info->back_flags &= ~BF_TRUNCATED;
	}
 
	return info;
}

void
backup_set_thresh (struct backup_info *info, unsigned int thresh)
{
	info->thresh = thresh;
}

/*************************************************************/
/* Check that the non stream zone maps are within the volume */
int
backup_verify_zone_maps (struct backup_info *info)
{
	unsigned volume_size = mfs_volume_set_size (info->mfs);
	zone_header *zone;

#if DEBUG
	fprintf (stderr, "Volume set size %ld\n", volume_size);
#endif

	for (zone = mfs_next_zone (info->mfs, NULL); zone; zone = mfs_next_zone (info->mfs, zone))
	{
		// Media zones will be accounted for later.
		if (zone->type == ztMedia)
			continue;

#if DEBUG
		fprintf (stderr, "Zone type %d at %ld\n", zone->type, zone->first);
#endif

		if (htonl (zone->first) >= volume_size)
		{
			info->err_msg = "%s zone outside available volume";
			switch (zone->type)
			{
			case ztInode:
				info->err_arg1 = "Inode";
				break;
			case ztApplication:
				info->err_arg1 = "Application";
				break;
			default:
				info->err_arg1 = "Unknown";
				break;
			}

			return -1;
		}
	}

// All loaded non-media zones within volume.
	return 0;
}

/*********************************************/
/* Attempt to recover from a failed mfs_init */
void
backup_check_truncated_volume (struct backup_info *info)
{
	if (!(info->back_flags & BF_TRUNCATED))
	{
		info->err_msg = "Backup cannot proceed on failed init";
		return;
	}

	// Shrinking a truncated volume implied.
	info->back_flags |= BF_SHRINK;;

	// Clear any errors.
	backup_clearerror (info);

	mfs_load_zone_maps (info->mfs);

	// More likely more errors generated.  Clear them too.
	backup_clearerror (info);

	// Make sure all loaded app zone maps are within volume;
	backup_verify_zone_maps (info);

	// The rest of the checks occur later.
	//   Check that inodes referencing application data fall below volume end.
}

/*********************/
/* Start the backup. */
int
backup_start (struct backup_info *info)
{
	struct blocklist *blocks;

	if ((add_partitions_to_backup_info (info, info->hda)) != 0) {
		return -1;
	}

	blocks = scan_inodes (info);
	if (add_blocks_to_backup_info (info, blocks) != 0) {
		free_block_list (&blocks);
		free (info->parts);
		return -1;
	}
	free_block_list (&blocks);

	if (add_mfs_partitions_to_backup_info (info) != 0) {
		free (info->parts);
		free (info->blocks);
		return -1;
	}

	info->presector = (info->nblocks * sizeof (struct backup_block) + info->nparts * sizeof (struct backup_partition) + info->nmfs * sizeof (struct backup_partition) + 511) / 512 + 1;

	info->nsectors += info->presector + 1;

	return 0;
}

/*****************************************************************************/
/* Return the next sectors in the backup.  This is where all the data in the */
/* backup originates.  If it's backed up, it came from here.  This only */
/* reads the data from the info structure.  Compression is handled */
/* elsewhere. */
static unsigned int
backup_next_sectors (struct backup_info *info, char *buf, int sectors)
{
	unsigned int retval = 0;

/* If there is nothing to do, do nothing.  How zen. */
	if (sectors <= 0)
	{
		return 0;
	}

/* If there is something to do, keep doing it until there is nothing.  How */
/* materialistic. */
	while (sectors > 0)
	{
/* If the secotr number is 0 or greater, the it is the actual data.  If it */
/* is below zero, it is the header. */
		if (info->cursector - info->presector >= 0)
		{
			int cursector = info->cursector - info->presector;
			int loop = 0;

			if (cursector == 0)
			{
/* Backup the boot sector. */
				tivo_partition_read_bootsector (info->devs[0].devname, buf);
				sectors -= 1;
				retval += 1;
				info->cursector += 1;
				buf += 512;
			}
			else
				cursector--;

/* Step through the partitions. */
			for (loop = 0; loop < info->nparts && sectors > 0; loop++)
			{
/* If the size of this partition doesn't account for the current sector, */
/* just track it and move on. */
				if (info->parts[loop].sectors <= cursector)
				{
					cursector -= info->parts[loop].sectors;
				}
				else
				{
					tpFILE *file;
					int tocopy = info->parts[loop].sectors - cursector;

/* The partition is larger than the buffer, read as much as possible. */
					if (tocopy > sectors)
					{
						tocopy = sectors;
					}

/* Get the file for this partition from the info structure. */
					file = info->devs[(int)info->parts[loop].devno].files[(int)info->parts[loop].partno];

/* If the file isn't opened, open it. */
					if (!file)
					{
						file = tivo_partition_open_direct (info->devs[(int)info->parts[loop].devno].devname, info->parts[loop].partno, O_RDONLY);
/* The sick part, is most of this line is an lvalue. */
						info->devs[(int)info->parts[loop].devno].files[(int)info->parts[loop].partno] = file;

/* If the file still isn't open, there is an error. */
						if (!file)
						{
							info->err_msg = "Internal error opening partition";
							return -1;
						}
					}

/* Read the data. */
					if (tivo_partition_read (file, buf, cursector, tocopy) < 0)
					{
						if (errno)
							info->err_msg = "%s backing up partitions";
						else
							info->err_msg = "Unknown error backing up partitions";
						info->err_arg1 = strerror (errno);
						return -1;
					}

					buf += tocopy * 512;
/* At this point, it is either the end of the buffer, at which point */
/* cursector doesn't matter, or it is the end of this partition, at which */
/* point, cursector should be 0. */
					cursector = 0;
					sectors -= tocopy;
					retval += tocopy;
					info->cursector += tocopy;
				}
			}
/* There are no partitions left.  Check the block list. */
			for (loop = 0; loop < info->nblocks && sectors > 0; loop++)
			{
/* If the current sector being backed up is beyond this block, just chip */
/* away at it and keep going. */
				if (info->blocks[loop].sectors <= cursector)
				{
					cursector -= info->blocks[loop].sectors;
				}
				else
				{
					int tocopy = info->blocks[loop].sectors - cursector;

/* Back up as much as possible.  If that exceeds the buffer size, get to the */
/* end of the buffer. */
					if (tocopy > sectors)
					{
						tocopy = sectors;
					}

/* Read the data. */
					if (mfs_read_data (info->mfs, buf, info->blocks[loop].firstsector + cursector, tocopy) < 0)
					{
						if (errno)
							info->err_msg = "%s reading MFS volume";
						else
							info->err_msg = "Unknown error reading MFS volume";

						info->err_arg1 = strerror (errno);
						return -1;
					}

					buf += tocopy * 512;
/* At this point, it is either the end of the buffer, at which point */
/* cursector doesn't matter, or it is the end of this block, at which */
/* point, cursector should be 0. */
					cursector = 0;
					sectors -= tocopy;
					retval += tocopy;
					info->cursector += tocopy;
				}
			}
			return retval;
/* In theory at this point the data is all written. */
		}
		else
		{
/* The cursector starts 1 further away from 0 than there are presectors. */
/* This is for the header.  Not too interesting here, but other places, the */
/* header is not compressed, while the rest of the backup is. */
			if (info->cursector > 0)
			{
/* This is part of the block list in the header. */
				int presector = info->cursector - 1;
				int curoff = presector * 512;
				int headerused = 0;
				int cursize;

/* First, the list of partitions.  The array in memory will be copied */
/* directly to the array on disk. */
				cursize = info->nparts * sizeof (struct backup_partition);
				if (curoff < cursize + headerused)
				{
					int needed_space = headerused + cursize - curoff;
					int have_space = sectors * 512 - curoff;

					if (needed_space > have_space)
					{
						needed_space = have_space;
					}

					memcpy (buf, (char *)info->parts + curoff - headerused, needed_space);

					buf += needed_space;

					curoff += needed_space;
					needed_space = curoff;

					while (presector < curoff / 512)
					{
						sectors--;
						retval++;
						presector++;
						needed_space -= 512;
						info->cursector++;
					}
				}
				headerused += cursize;

				cursize = info->nblocks * sizeof (struct backup_block);
				if (sectors > 0 && curoff < cursize * headerused)
				{
					int needed_space = headerused + cursize - curoff;
					int have_space = sectors * 512 - curoff;

					if (needed_space > have_space)
					{
						needed_space = have_space;
					}

					memcpy (buf, (char *)info->blocks + curoff - headerused, needed_space);

					buf += needed_space;

					curoff += needed_space;
					needed_space = curoff;

					while (presector < curoff / 512)
					{
						sectors--;
						retval++;
						presector++;
						needed_space -= 512;
						info->cursector++;
					}
				}
				headerused += cursize;

				cursize = info->nmfs * sizeof (struct backup_partition);
				if (sectors > 0 && curoff < cursize * headerused)
				{
					int needed_space = headerused + cursize - curoff;
					int have_space = sectors * 512 - curoff;

					if (needed_space > have_space)
					{
						needed_space = have_space;
					}

					memcpy (buf, (char *)info->mfsparts + curoff - headerused, needed_space);

					buf += needed_space;

					curoff += needed_space;
					needed_space = curoff;

					while (presector < curoff / 512)
					{
						sectors--;
						retval++;
						presector++;
						needed_space -= 512;
						info->cursector++;
					}
				}


				while (sectors > 0 && presector < info->presector - 1)
				{
					int start = curoff & 511;
					int end = (info->presector - presector - 1) * 512;

					if (end / 512 > sectors) {
						end = sectors * 512;
					}

					bzero (buf, end - start);

					buf += end - start;
					sectors -= end / 512;
					retval += end / 512;
					presector += end / 512;
					info->cursector += end / 512;
				}
			}
			else
			{
				struct backup_head *head = (struct backup_head *)buf;
	
				bzero (head, sizeof (*head));
				head->magic = TB_MAGIC;
				head->flags = info->back_flags;
				head->nsectors = info->nsectors;
				head->nparts = info->nparts;
				head->nblocks = info->nblocks;
				head->mfspairs = info->nmfs;
				retval += 1;
				sectors -= 1;
				info->cursector += 1;
				buf += 512;
			}
		}
	}

	return retval;
}

/*************************************************************************/
/* Pass the data to the front-end program.  This handles compression and */
/* all that fun stuff. */
unsigned int
backup_read (struct backup_info *info, char *buf, unsigned int size)
{
	unsigned int retval = 0;

	if (size < 512)
	{
		info->err_msg = "Internal error 2 - Backup buffer too small";
		return -1;
	}

	if (info->back_flags & BF_COMPRESSED)
	{
		if (info->cursector == 0)
		{
			retval = backup_next_sectors (info, buf, 1);
			if (retval != 1)
			{
				info->err_msg = "Error starting backup";
				return -1;
			}

			info->comp_buf = calloc (2048, 512);
			if (!info->comp_buf)
			{
				info->err_msg = "Memory exhausted";
				return -1;
			}

			info->comp = calloc (sizeof (*info->comp), 1);
			if (!info->comp)
			{
				free (info->comp_buf);
				info->err_msg = "Memory exhausted";
				return -1;
			}

			info->comp->zalloc = Z_NULL;
			info->comp->zfree = Z_NULL;
			info->comp->opaque = Z_NULL;
			info->comp->next_in = Z_NULL;
			info->comp->avail_in = 0;
			info->comp->avail_out = 0;
			if (deflateInit (info->comp, BF_COMPLVL (info->back_flags)) != Z_OK)
			{
				free (info->comp_buf);
				free (info->comp);
				info->err_msg = "Compression init error";
				return -1;
			}

			buf += 512;
			retval = 512;
			size -= 512;
		}

		if (!info->comp)
		{
			return retval;
		}

		info->comp->avail_out = size;
		info->comp->next_out = buf;
		while (info->comp && info->comp->avail_out > 0)
		{
			if (info->comp->avail_in)
			{
				if (deflate (info->comp, Z_NO_FLUSH) != Z_OK)
				{
					info->err_msg = "Compression error";
					return -1;
				}
			}
			else if (info->comp_buf)
			{
				int nread = backup_next_sectors (info, info->comp_buf, 2048);
				if (nread < 0)
				{
					return -1;
				}
				if (nread == 0)
				{
					free (info->comp_buf);
					info->comp_buf = 0;
					continue;
				}

				info->comp->avail_in = 512 * nread;
				info->comp->next_in = info->comp_buf;
			}
			else
			{
				int zres = deflate (info->comp, Z_FINISH);

				if (zres == Z_STREAM_END)
				{
					retval += size - info->comp->avail_out;
					zres = deflateEnd (info->comp);
					free (info->comp);
					info->comp = 0;
				}

				if (zres != Z_OK)
				{
					break;
				}
			}
		}
		if (info->comp)
		{
			retval += size - info->comp->avail_out;
		}
	}
	else
	{
		return backup_next_sectors (info, buf, size / 512) * 512;
	}

	return retval;
}

int
backup_finish(struct backup_info *info)
{
	if (info->cursector != info->nsectors)
	{
		info->err_msg = "Backup ended prematurely";
		return -1;
	}

	return 0;
}

/****************************/
/* Display the backup error */
void
backup_perror (struct backup_info *info, char *str)
{
	int err = 0;

	if (info->err_msg)
	{
		fprintf (stderr, "%s: ", str);
		fprintf (stderr, info->err_msg, info->err_arg1, info->err_arg2, info->err_arg3);
		fprintf (stderr, ".\n");
		err = 1;
	}

	if (info->mfs->err_msg || info->mfs->vols->err_msg)
	{
		mfs_perror (info->mfs, str);
		err = 2;
	}

	if (err == 0)
	{
		fprintf (stderr, "%s: No error.\n", str);
	}
}

/***********************/
/* Backup has an error */
int
backup_has_error (struct backup_info *info)
{
	if (info->err_msg)
		return 1;

	if (info->mfs)
		return mfs_has_error (info->mfs);

	return 0;
}

/*************************************/
/* Return the MFS error in a string. */
int
backup_strerror (struct backup_info *info, char *str)
{
	if (info->err_msg)
		sprintf (str, info->err_msg, info->err_arg1, info->err_arg2, info->err_arg3);
	else if (info->mfs)
		return (mfs_strerror (info->mfs, str));
	else
	{
		sprintf (str, "No error");
		return 0;
	}

	return 1;
}

/********************/
/* Clear any errors */
void
backup_clearerror (struct backup_info *info)
{
	info->err_msg = 0;
	info->err_arg1 = 0;
	info->err_arg2 = 0;
	info->err_arg3 = 0;

	if (info->mfs)
		mfs_clearerror (info->mfs);
}
