/**
 *
 * @author: Jasenko Cordalija & Thomas Dye
 * @class: CSS 430
 * @date: 12/13/2016
 *
 * This Project is FileSystem.java which is submitted alongside 
 * multiple other files for the File System project of CSS 430
 *
 * Jasenko is responsible for following methods: sync, open, 
 * format, close, fsize, deallocAllBlocks, seek, delete
 *
 * Thomas is responsible for methods: read and write.
 */

public class FileSystem
{
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
    private final int blockSize = 512;

    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    public FileSystem(int diskBlocks)
    {
        // create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock(diskBlocks);

        // create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.totalInodes);

        // file table is created, and store directory in the file table
        filetable = new FileTable(directory);

        // directory reconstruction
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);

        if (dirSize > 0)
        {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }

        close(dirEnt);
    }
	
   // sync(), allows filesystem directory to sync back and save to disk.
    void sync()
    {
        superblock.sync();
        byte[] tmpByteDir = directory.directory2bytes();	
        FileTableEntry ftEnt = open("/", "w");		// "w" - write
        write(ftEnt, tmpByteDir);		// directory is written to file table entry, then FTEntry closed
        close(ftEnt);
		
    }
	
	
    // open(), opens the file specified by string filename in
    // its parameter, which also has another string mode. This mode will
    // be given to the filename string. falloc() method creates a new ftEnt- 
    // which is file table entry. Should the mode be "w", deallocAllBlocks will
    // delete all blocks. Upon success of open() the new file table entry is returned.
    // code source: https://courses.washington.edu/css430/prog/CSS430FinalProject.pdf
    FileTableEntry open(String filename, String mode)	
    {
        FileTableEntry ftEnt = filetable.falloc(filename, mode);
        if(mode.equals("w"))
        {
            if(deallocAllBlocks(ftEnt) == false)
            {
                return null;	// returns null, on deallocate blocks
            }
        }
        return ftEnt;	// returns file entry
    }

	
    // format(), Formats the disk (Disk.java) data contents. format() has 1
    // parameter, files. files specifies the maximum number of files 
    // to be created-> number of inodes to be allocated in your file system.
    // Returns true on success, and false if error/fail. Before returning true,
    // dletes disk content, disk formating, after builds content on the Disk.
    boolean format(int files)
    {
        if(files <= 0)	// file 0 or -1 returns false
        {
            return false; 
        }
        else 
        {
            // These below three provided above under FileSystem
            // superblock, format disk with files in argument format()
            superblock.format(files);           
            // create directory, and register "/" in directory entry 0
            directory = new Directory(superblock.totalInodes);
            // file table is created, and store directory in the file table
            filetable = new FileTable(directory);
        }
        return true;   
    }

	
    // close(), closes the file table entry corresponding to ftEnt in parameter
    // returns false if ftEnt is null, and true if ftEnt is > 0. 
    // If ftEnt is 0, indiciating by the count that the ftEnt is not in use then
    // filetable.ffree, writes to disk and free ftEnt
    boolean close(FileTableEntry ftEnt)	// closes file corresponding to ftentry
    {
        synchronized (ftEnt)
        {	
            if(ftEnt == null || ftEnt.count == -1)
            {
                return false;	// returns false on error
            }
            int newFTECount = ftEnt.count -1; // decrease count by one for closing
            if (newFTECount == 0)
            {
                return filetable.ffree(ftEnt);	// unregisters ftentry from fdt
            }
	
            return true; 	// returns true if > 0
        }
    }
	

    // fSize(), returns the size of the ftEnt FileTableEntry in its parameter in bytes
    int fsize(FileTableEntry ftEnt)
    {
        synchronized(ftEnt)
        {
            return ftEnt.inode.length;
        }
    }
	
	
    // deallocAllBlocks(), handles the deallocation of all blocks in a file 
    // specified by argument ftEnt. If ftEnt or inode is null false is returned.
    // Loops through 11 (the direct pointer block) and if any of the 11 are not -1, 
    // then each of the 11 valid direct pointers then superblock.returnBlock(i) to return
    // after setting them to -1 indicating invalid. After indirect pointers are handled next.
    private boolean deallocAllBlocks(FileTableEntry ftEnt)
    {
        byte[] tmpIndirectPointer;
        if(ftEnt == null || ftEnt.inode == null) // inode is null so return false on deallocation
        {
            return false;
        }
		
        //  Sets 11 direct pointer (final static inode) to -1
        for(int i = 0; i < 11; i++ )	
        {
            if(ftEnt.inode.direct[i] != -1)
            {
                 superblock.returnBlock(ftEnt.inode.direct[i]);	// if direct at i block isn't null, superBloc returnBlock method on 
            }
		
            ftEnt.inode.direct[i] = -1;		// all direct set to -1 always
        }
		
        if((tmpIndirectPointer = ftEnt.inode.unregisterIndexBlock()) != null)
        {	// any indirect pointer attained. if direct pointer from a block is not null
            int i = SysLib.bytes2short(tmpIndirectPointer,0);
            while(i != -1)
            {
                superblock.returnBlock(i);
            }
        }
		
        // As always, write ftEnt iNodes back onto the disk then return true
        // all blocks have been deallocated from file ftEnt
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

	
    // seek(), handles a filetableEntry's seekpointer. Returns seekPtr if operation
    // is not error else it returns -1 if error. If whence SEEK_SET (= 0), the 
    // file's seek pointer then is set to offset bytes from the beginning of file
    // If SEEK_CUR (= 1), the file's seek pointer set to its current value plus 
    // the offset. The offset can be positive but if negative is set to 0
    // If SEEK_END (= 2), the file's seek pointer is set to the size of the file 
    // plus the offset. The offset can be positive but if negative, it is set to 0.
    // Any user attempts in setting seek pointer to negative is set to 0 and returns
    // on success seekPtr. Should user attempts to set pointer beyond file size, 
    // the pointer is set to the file and returns on success seekPtr.
    int seek(FileTableEntry ftEnt, int offset, int whence)
    {
        synchronized (ftEnt)
        {
            switch(whence)
            {
                // Offset is relative to the beginning of the file
                case SEEK_SET:	ftEnt.seekPtr = offset; break;
				
                // Offset is relative to the current seek pointer
                case SEEK_CUR:	ftEnt.seekPtr += offset;
                if (ftEnt.seekPtr < 0) 
                {
                    ftEnt.seekPtr = 0;	// negative seekpointer set to 0, cant be negative
                }

                if (ftEnt.seekPtr > ftEnt.inode.length) 
                {
                    ftEnt.seekPtr = ftEnt.inode.length;	// seekpointer can be larger then file, if it is, sets Ptr to max file size
                }
                break;
				
                // Offset is relative to the end of the file
                case SEEK_END:	ftEnt.seekPtr = ftEnt.inode.length + offset;
                if (ftEnt.seekPtr < 0) 
                {
                    ftEnt.seekPtr = 0;
                }

                if (ftEnt.seekPtr > ftEnt.inode.length) // seekPtr is larger then inode lenght
                {
                    ftEnt.seekPtr = ftEnt.inode.length;	// set seekPtr to inode lenght (end of file)
                }
                break;
				
                // Error on whence, it is neither of the 3 cases
                default: return -1;
            }	
            return ftEnt.seekPtr;	// ptr returned upon success
        }
    }
	
	
    // delete(), deletes/ destroyes file indicated by argument filename of delete().
    boolean delete(String filename)
    {
        // temporary file entry will be used, returns true if file entry was freed and closed
        FileTableEntry tmpFTE = open(filename, "w"); // currently open file placed in tmp
        short iNumberFTE = tmpFTE.iNumber;
        if (directory.ifree(iNumberFTE))	// returns true if corresponding file number dellocated
        {
            if(close(tmpFTE)) // if close function returns true on tmpFTE
            {
                return true;	// delete was successfull
            }
                return false;	// unsuccefull last open is not closed
        }
        else
        {
            return false;
        }
    }
	
    // read() has two parameters, FileTableEntry ftEnt and byte[] buffer.
    // This method has data read from the disk until the end of the block 
    // or until the end of the file is reached, whichever is first.  
    // In all cases, data read is limited to the buffer size.  
    // Continue to read data until read data sequentially from the disk 
    // to fill the given buffer, or until the end of the file is reached, 
    // as indicated by the file descriptor, starting at the position currently 
    // indicated by the seek pointer.
    int read(FileTableEntry ftEnt, byte[] buffer)
    {
        if (ftEnt == null)
        {
            return -1;
        }
        
        // Block on modes where read is not available
        if (ftEnt.mode == "w" || ftEnt.mode == "a")
        {
            return -1;
        }
        
        int bytesRead = 0;
        
        synchronized(ftEnt)
        {
            int bytesRemaining = buffer.length;
            int block;
            int offset;
            int readLength;
            
            // read while there are bytes remaining and not EOF
            while (bytesRemaining > 0 && ftEnt.seekPtr < fsize(ftEnt))
            {
                // associate block that contains the file
                if ((block = ftEnt.inode.findTargetBlock(ftEnt.seekPtr)) < 0)
                {
                    // can't find block
                    return -1;
                }
                
                offset = ftEnt.seekPtr % blockSize;
                
                if (blockSize - offset < fsize(ftEnt) - ftEnt.seekPtr)
                {
                    // only read until the end of the block
                    readLength = blockSize - offset;
                }
                else
                {
                    // only read until the end of the file
                    readLength = fsize(ftEnt) - ftEnt.seekPtr;
                }
                
                // restrict the read length to the size of the buffer
                // pick up any remaining bytes in subsequent loops
                if (bytesRemaining < readLength)
                {
                    readLength = bytesRemaining;
                }
                
                // copy data from disk to buffer
                byte[] data = new byte[blockSize];
                SysLib.rawread(block, data);
                System.arraycopy(data, offset, buffer, bytesRead, readLength);
                
                // update pointers
                bytesRead += readLength;
                bytesRemaining -= readLength;
                ftEnt.seekPtr += readLength;
            }
        }
        
        return bytesRead;
    }

    // write() has two parameters, FileTableEntry ftEnt and byte[] buffer.
    // This method writes the contents of the given buffer to the disk.  
    // If the file descriptor is not associated with a current file containing 
    // data, a new block must be located. The special case is handled where 
    // an index block also needs to be created before the block data can be 
    // allocated, using error condition -3. Data is written until reaching 
    // the end of the block, or until the buffer is depleted, whichever occurs 
    // first. All buffered data is then written to disk and the associated 
    // inode is updated. 
    int write(FileTableEntry ftEnt, byte[] buffer)
    {
        if (ftEnt == null)
        {
            return -1;
        }
        
        // Block on modes where files are read only
        if (ftEnt.mode == "r")
        {
            return -1;
        }
        
        int bytesWritten = 0;
        
        synchronized(ftEnt)
        {
            int bytesRemaining = buffer.length;
            int block;
            int offset;
            int writeLength;
            
            while (bytesRemaining > 0)
            {
                // associate block that contains the file
                if ((block = ftEnt.inode.findTargetBlock(ftEnt.seekPtr)) < 0)
                {
                    // no block found, so get a free block
                    if ((block = superblock.getFreeBlock()) < 0)
                    {
                        // there were no free blocks
                        return -1;
                    }
                    
                    // Get the return code when registering the new block
                    int rc = ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, (short)block);
                    
                    if (rc == -1 || rc == -2)
                    {
                        // error conditions exit, so bail.
                        return -1;
                    }
                    else if (rc == -3)
                    {                    
                        short indexBlock;
                        
                        // the index block must be registered first on an indirect pointer.
                        if ((indexBlock = (short)superblock.getFreeBlock()) < 0)
                        {
                            // there were no free blocks
                            return -1;
                        }
                        
                        if (!ftEnt.inode.registerIndexBlock(indexBlock))
                        {
                            // could not create index block
                            return -1;
                        }
                        
                        // try to register the target block again
                        if (ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, (short)block) < 0)
                        {
                            // give up (disk is full?)
                            return -1;
                        }
                    }
                }
                
                offset = ftEnt.seekPtr % blockSize;
                
                if (blockSize - offset < bytesRemaining)
                {
                    // only write until the end of the block
                    writeLength = blockSize - offset;
                }
                else
                {
                    // only write what's left in the buffer
                    writeLength = bytesRemaining;
                }
                
                // get existing data and overwrite/append with new data from buffer
                byte[] data = new byte[blockSize];
                SysLib.rawread(block, data);
                System.arraycopy(buffer, bytesWritten, data, offset, writeLength);
                SysLib.rawwrite(block, data);
                
                // update pointers
                bytesWritten += writeLength;
                bytesRemaining -= writeLength;
                ftEnt.seekPtr += writeLength;
                
                // data was appended, so update inode
                if (ftEnt.seekPtr > ftEnt.inode.length)
                {
                    ftEnt.inode.length = ftEnt.seekPtr;
                }
            }
        }
        
        // save changes to disk
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return bytesWritten;
    }
}