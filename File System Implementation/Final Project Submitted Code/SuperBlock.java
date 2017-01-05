/**
 *
 * @author: Jasenko Cordalija
 * @class: CSS 430
 * @date: 12/13/2016
 *
 * This Project is SuperBlock.java which is submitted alongside 
 * multiple other files for the File System project of CSS 430
 */

class SuperBlock
{
    // default size of inode is 64, block 1-4 each 16, superblock is block 0
    private final int defaultInodeBlocks = 64;	
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    // https://courses.washington.edu/css430/prog/CSS430FinalProject.pdf
    // Source given to use for constructor of superBlock.
    public SuperBlock(int diskSize)
    {
        // Responsible for reading the SuperBlock from the disk
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
		
        // locations of totalBlocks, totalInodes, freeList
        totalBlocks = SysLib.bytes2int(superBlock,0);
        totalInodes = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock,8); 
		
        if(totalBlocks == diskSize && totalInodes > 0 && freeList >=2)
        {
            return;	// the contents of the disk are valid
        }
        else
        {
        // the contents of the disk are not valid, the disk must be formated
        totalBlocks = diskSize;
        format(defaultInodeBlocks);
        }
    }
	
	
    // format() method of SuperBlock, will delete/ erease the disk's data with 
    // the specific number of iNodes presented in format()'s argument
    public void format (int iNodes)
    {
        totalInodes = iNodes;

        //inodes are made under tmp and then written to the Disk
        for(int i = 0; i < iNodes; i++)
        {
            Inode tmpINode = new Inode();
            tmpINode.toDisk((short)i);	// writes to Disk
        }

        // freeList head is first free block. 
        freeList = (iNodes / 16) + 2; // every block has only 16 (0-15, etc) 

        // Blocks of (999) written to disk, default is 1000.
        for(int i = freeList; i < 999; i++)
        {
            byte[]tmpSBlock = new byte[Disk.blockSize];

            // the tmpSBlock has all elements set to 0
            for(int j = 0; j < Disk.blockSize; j++)
            {
                tmpSBlock[j] = 0;
            }

            // i+1 indicates next free block to next free ptr, followed
            // by writing block to the disk
            if(i != 999)
            {
                SysLib.int2bytes(i+1, tmpSBlock, 0);
            }
            
            SysLib.rawwrite(i, tmpSBlock);
        }

        sync();	// sync to disk
    }
	
	
    // returnBlock() method of SuperBlock will return the new block head of
    // freeList true if available else it will return a false on error
    // resulting from block number being less then 0 or larger then total blocks
    public boolean returnBlock(int blockNumber)
    {
        // Checks blockNumber to verify it's not error
        if (blockNumber < 0 || blockNumber > totalBlocks) 
        {
            return false;
        }
		
        // Sets newFb for entire diskblock Size (512)
        // to 0 (deletes block), The newFB will be added to buffer, the 
        // blockNumber data written to new free block (newFb)
        byte[] newFb = new byte[Disk.blockSize];
        for (int i = 0; i < Disk.blockSize; i++) 
        {
            newFb[i] = 0;
        }
            
        SysLib.int2bytes(freeList, newFb, 0);
        SysLib.rawwrite(blockNumber, newFb);	// writes back to Disk
        freeList = blockNumber;
        return true;	// returns true
    }
	
	
    // getFreeBlock() method of SuperBlock dequeues the top block
    // from the freeList and returns it, if there is no free block
    // in the freeList then -1 is returned indicating so.
    public int getFreeBlock()
    {
        int returnBlock = freeList;
        // indicating there are no empty blocks in freeList
        if (freeList <= 0 )  
        {
            return -1;	// error on freeList
        }
		
        byte[] firstFb = new byte [Disk.blockSize];
        SysLib.rawread(freeList, firstFb);		// first free block is read
        int nextFreeBlock = SysLib.bytes2int(firstFb,0);
        freeList = nextFreeBlock;	// next free block to freeList
        sync();
        return returnBlock;		// free block returned
    }
	
	
    // synch() method of SuperBlock writes back the totalBlocks, the inodeBlocks
    // and also the freelist to the disk
    public void sync()
    {
        // syncSuperBlock will have written to it the totalBlocks,
        // totalInodes, and freelist indicated above and then is written
        // onto the disk.
        byte[] syncSuperBlock = new byte[Disk.blockSize];		
        SysLib.int2bytes(totalBlocks, syncSuperBlock, 0);
        SysLib.int2bytes(totalInodes, syncSuperBlock, 4);
        SysLib.int2bytes(freeList, syncSuperBlock, 8);
        SysLib.rawwrite(0,syncSuperBlock);
    }
}
