/**
 *
 * @author: Thomas Dye
 * @class: CSS 430
 * @date: 12/13/2016
 *
 * This Project is Inode.java which is submitted alongside 
 * multiple other files for the File System project of CSS 430
 */


public class Inode
{
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    private final static int blockSize = 512;
    private final static int sizeOfInt = 4;        // size)f used for Offset
    private final static int sizeOfShort = 2;

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

	
    // Default Constructor, flag is set to USED, while count and 
    // length default to 0.
    Inode()
    {
        length = 0;
        count = 0;
        flag = 1;
        for (int i = 0; i < directSize; i++) // 0-10, 11 direct pointers
        {
            direct[i] = -1;	// all 11 direct pointer set to -1
        }

        indirect = -1;	
    }

	
    // Constructor
    // Inode's second constructor retrieves and read the iNumber block, then
    // locates inode info., and initializes the new inode with this info.  If a
    // new file is created, it is given a new inode. In this case, just
    // instantiate it from its default constructor where all direct pointers
    // and the indirect pointer are null. The contents will be later updated as
    // the file is written. An existing file has already had an inode. It is in
    // the disk. When such an existing file is opened, find the corresponding
    // inode from the disk. First, refer to the directory in order to find the
    // inode number. From this inode number, you can calculate which disk block
    // contains the inode. Read this disk block and get this inode information.
    // Instantiate an inode object first, and then reinitialize it with the
    // inode information retrieved from the disk. 
    Inode(short iNumber)
    {
        // assign inodes to block
        int block = iNumber / (blockSize / iNodeSize) + 1;
        int offset = (iNumber % (blockSize / iNodeSize)) * iNodeSize;
        
        byte[] buffer = new byte[blockSize];
        SysLib.rawread(block, buffer);
        
        // initialize and increment offset as appropriate
        // length offset by 4, count and flag offset by 2
        length = SysLib.bytes2int(buffer, offset);
        offset += sizeOfInt;
        
        count = SysLib.bytes2short(buffer, offset);
        offset += sizeOfShort;
        
        flag = SysLib.bytes2short(buffer, offset);
        offset += sizeOfShort;
        
        // handles allocation of direct pointers from buffer
        for (int i = 0; i < directSize; i++)
        {
            direct[i] = SysLib.bytes2short(buffer, offset);
            offset += sizeOfShort;	// offset increments by 2
        }
        
        // handles indirect pointer allocation with offset by 2
        indirect = SysLib.bytes2short(buffer, offset);
        offset += sizeOfShort;
    }

	
    // toDisk, similar to Inode constructor above but also
    // Write back a particular inode’s information to disk on request. 
    // All flag states are converted to byte values and packed before saved 
    // to the appropriate disk block (dependent on the iNumber specified).
    void toDisk(short iNumber)
    {
        // assign inodes to block
        int block = iNumber / (blockSize / iNodeSize) + 1;
        int offset = (iNumber % (blockSize / iNodeSize)) * iNodeSize;
        
        byte[] buffer = new byte[blockSize];
        SysLib.rawread(block, buffer);
        
        // prepare save to disk as the i-th inode
        SysLib.int2bytes(length, buffer, offset);
        offset += sizeOfInt;
        
        SysLib.short2bytes(count, buffer, offset);
        offset += sizeOfShort;
        
        SysLib.short2bytes(flag, buffer, offset);
        offset += sizeOfShort;
        
        for (int i = 0; i < directSize; i++)
        {
            SysLib.short2bytes(direct[i], buffer, offset);
            offset += sizeOfShort;
        }
        
        SysLib.short2bytes(indirect, buffer, offset);
        offset += sizeOfShort;
        
        // Save to disk
        SysLib.rawwrite(block, buffer);
    }
	
    
    // Find Target Block
    // Returns block data for a given offset on request.  
    // If the offset points to data stored in the range of 
    // direct pointers, the pointer’s data is returned.  
    // If it is in an indirect pointer, the entire block referenced 
    // by the indirect pointer is read and the specific byte data 
    // is unpacked and returned.
    int findTargetBlock(int offset)
    {
        int iNumber = offset / blockSize;
        
        // get block data from direct pointer (iNumber < 11)
        if (iNumber < directSize)
        {
            return direct[iNumber];
        }
        
        // get the block data from indirect, returning -1
        // if value of indirect pointer is less then 0
        if (indirect < 0)
        {
            return -1;
        }
        
        byte[] buffer = new byte[blockSize];
        SysLib.rawread(indirect, buffer);
        
        return SysLib.bytes2short(buffer, (iNumber - directSize) * sizeOfShort);
    }
    
	
    // Register Target Block
    // Save the value of the provided target block to an inode pointer.  
    // If the offset points to data stored in the range of direct pointer, 
    // the block address is saved to that pointer.  Otherwise, byte pack 
    // the value to the appropriate block referenced by the indirect pointer.  
    // Returns success or failure codes.
    int registerTargetBlock(int offset, short targetBlock)
    {
        int iNumber = offset / blockSize;
        
        // use direct blocks
        if (iNumber < directSize)
        {
            // this or the previous blocks are empty, so fail.
            if ((iNumber > 0 && direct[iNumber - 1] < 0) || direct[iNumber] >= 0)
            {
                return -1;	// not successfull on used block
            }
            
            // use this block
            direct[iNumber] = targetBlock;
            return 0; // success = unused block
        }
        
        // otherwise, use indirect blocks
        if (indirect < 0)
        {
            // FileSystem.java needs this specific error clearly identified
            return -3;
        }
        
        byte[] buffer = new byte[blockSize];
        SysLib.rawread(indirect, buffer);
        
        SysLib.short2bytes(targetBlock, buffer, (iNumber - directSize) * sizeOfShort);
        SysLib.rawwrite(indirect, buffer);
        return 0; // success = unused block
    }
    
	
    // Register Index Block
    // Initialize the index block and the entire range of 256 indexes 
    // referenced by the indirect block.  After initializing values, 
    // data is written to disk.  Returns true if successful, else false 
    // if the indirect pointer is already registered.
    boolean registerIndexBlock(short indexBlock)
    {
        // ensure indirect pointer references a free block
        if (indirect != -1)
        {
            return false;
        }
        
        indirect = indexBlock;
        
        byte[] buffer = new byte[blockSize];
        
        // work the range of the single indirect pointer
        for (int i = 0; i < blockSize / 2; i++)
        {
            SysLib.short2bytes((short)-1, buffer, i * sizeOfShort);
        }
        
        SysLib.rawwrite(indexBlock, buffer); // indexblock data written to buffer
        return true;
    }
    
    // Unregister Index Block
    // Get the contents of the index block stored and return it to the user. 
    //The index block is freed for this inode.
    byte[] unregisterIndexBlock()
    {
        if (indirect < 0)
        {
            // nothing to do, pointer value is negative
            return null;
        }
        
        // get the previous contents to return
        byte[] buffer = new byte[blockSize];
        SysLib.rawread(indirect, buffer);
        
        // unregister indirect, indirect pointer value set to -1
        indirect = -1;
        
        return buffer;	// returns buffer
    }
}
