/**
 *
 * @author: Jasenko Cordalija
 * @class: CSS 430
 * @date: 12/13/2016
 *
 * This Project is FileTable.java which is submitted alongside 
 * multiple other files for the File System project of CSS 430
 */

import java.util.Vector;

public class FileTable
{
    private Vector table;       // the actual entity of this file table
    private Directory dir;      // the root Directory

    // Constructor
    public FileTable(Directory directory)
    {
        table = new Vector();   // instantiate a file (structure) table
        dir = directory;        // receive a reference to the Directory
                                // from the file system
    }

    	
    // falloc() method allocates a new file (structure) table entry for this filename 
    // allocates & retrieve register for the corresponding inode using dir
    // increment this inode's count, followed by immediate write back this inode to the disk
    // then returns a reference to this file (structure) table entry
    // 0 = unused, 1 = used, 2 = read, 3 = write 
    // some ideas came from slides provided- source:
    // https://courses.washington.edu/css430/prog/CSS430FinalProject.pdf
    public synchronized FileTableEntry falloc(String filename, String mode)
    {
        short iNumber;
        Inode inode;
        while (true)
        {	
            //inode iNumber for filename
            iNumber = (filename.equals("/") ? 0 : dir.namei(filename)); 
            if(iNumber >= 0)
            {	
                // iNumber 0 or greater = file exists, if iNumber -1 file dosen't exist
                if(mode.equals("r"))	// if mode is in read
                {
                    inode = new Inode(iNumber);
                    if(inode.flag == 2 || inode.flag == 1 || inode.flag == 0)
                    {
                        inode.flag = 2;	// flag is read mode, no need to wait 
                        break;
                    }
                    else if(inode.flag == 3)	// flag is write
                    {	
                        // waiting for a write to exit
                        try {
                            wait();		
                        } catch (InterruptedException e){}
                    }
                }

                // mode of file w, w+, a instead
                if(mode.equals("w") || mode.equals("w+") || mode.equals("a"))
                {	
                    // file w, w+, or a mode 
                    inode = new Inode(iNumber);
                    if (inode.flag == 1 || inode.flag == 0)	// inode flag is 1 or 0, used/unused
                    {
                        inode.flag = 3;		// flag is 3, mode write
                        break;
                    }
                    else if (inode.flag == 3 || inode.flag == 2)
                    {	
                        try {
                            wait();	// wait until flag is done 
                        } catch (InterruptedException e){}
                    }
                }
            }
		
            if(!(mode.equals("r")))	// mode dosent equal to read, iNumber < 0 (negative)
            {	
                // new file inode made
                iNumber = dir.ialloc(filename);
                inode = new Inode(iNumber);
                break;
            }
            else 
            {	// flag is not either read/ write. to be deleted
                iNumber = -1;
                return null;
            }
        }
		
        inode.count++;  // increment the inode count
        inode.toDisk(iNumber);	// written to Disk
        FileTableEntry e = new FileTableEntry(inode, iNumber, mode);
        table.addElement(e);	// create and store a table entry
        return e;
    }
    
	
    // ffree() method receive a file table entry reference, 
    // writes inode to the disk, free inodes FTE,
    // return false if this file table entry cant be found in my table
    // 0 = unused, 1 = used, 2 = read, 3 = write 
    public synchronized boolean ffree(FileTableEntry e)
    {
        if(table.remove(e))
        {
            e.inode.count--;	// inode count decreased by 1, 
            if(e.inode.flag == 2)	// flag is read if equal to 2
            {
                e.inode.flag = 1;	// flag set to 1 indicating used
                notify();
            }

            else if (e.inode.flag == 3)	// flag is write if equal to 3
            {
                e.inode.flag = 1;	// flag set to 1 indicating used
                notify();
            }

            e.inode.toDisk(e.iNumber);
            return true;	// FTE was removed from fileTable, returned true
        }
	return false;	// FTE is not in fileTable
    }

	
    // fempty() method checks if table is empty	
    public synchronized boolean fempty()
    {
       	return table.isEmpty(); // return if table is empty
                            	// should be called before starting a format
    }
}