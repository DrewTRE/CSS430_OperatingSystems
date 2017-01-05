/**
 *
 * @author: Thomas Dye
 * @class: CSS 430
 * @date: 12/13/2016
 *
 * This Project is Directory.java which is submitted alongside 
 * multiple other files for the File System project of CSS 430
 */

public class Directory
{
    private static int maxChars = 30; // max characters of each file name
    private static int sizeOfInt = 4; // because sizeof isn't in java...?
    private static int sizeOfChar = 2; // because it's 2 bytes in java since UTF-16

    // Directory entries
    private int fsizes[];       // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    // Directory Constructor
    public Directory(int maxInumber)
    {
        fsizes = new int[maxInumber];               // maxInumber = max files
        for (int i = 0; i < maxInumber; i++)
        {
            fsizes[i] = 0;                           // all file size initialized to 0
        }

        fnames = new char[maxInumber][maxChars];
        String root = "/";                          // entry(inode) 0 is "/"
        fsizes[0] = root.length();                  // fsize[0] is the size of "/".
        root.getChars(0, fsizes[0], fnames[0], 0);  // fnames[0] includes "/"
    }

    // Bytes 2 Directory
    // Methods bytes2directory() and directory2bytes are used to initialize the
    // Directory instance with a byte array read from the disk, and convert the
    // Directory instance into a byte array, which will later be written back
    // to the disk.
    public int bytes2directory(byte data[])
    {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
        for (int i = 0; i < fsizes.length; i++)
        {
            fsizes[i] = SysLib.bytes2int(data, i * sizeOfInt);
        }
        
        // The "/" root directory maintains each file in a different directory
        // entry that contains its file name (maximum 30 characters; 60 bytes
        // in Java) and the corresponding inode number.
        int entries = maxChars * 2;
        int offset = (fsizes.length - 1) * sizeOfInt;
        String name;
        
        for (int i = 0; i < fnames.length; i++)
        {
            name = new String(data, offset, entries);
            offset += sizeOfChar;
            
            name.getChars(0, fsizes[i], fnames[i], 0);
        }
        
        return 0;
    }

    // Directory 2 Bytes
    // Methods bytes2directory() and directory2bytes are used to initialize the
    // Directory instance with a byte array read from the disk, and convert the
    // Directory instance into a byte array, which will later be written back
    // to the disk.
    public byte[] directory2bytes()
    {
        int entries = maxChars * 2;
        int fileAlloc = fsizes.length * sizeOfInt;
        
        byte[] data = new byte[entries * fnames.length + fileAlloc];
        
        for (int i = 0; i < fsizes.length; i++)
        {
            SysLib.int2bytes(fsizes[i], data, i * sizeOfInt);
        }
        
        int offset = (fsizes.length - 1) * sizeOfInt;
        String name;
        
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        for (int i = 0; i < fnames.length; i++)
        {
            name = new String(fnames[i], 0, fsizes[i]);
            byte[] buffer = name.getBytes();
            System.arraycopy(buffer, 0, data, offset, buffer.length);
            offset += entries;
        }
        
        return data;
    }

    // IAlloc
    // Find the location of an empty file to use for a new allocation, 
    // and returns the new associated inode number.  If a candidate is 
    // located, the filename is saved.
    public short ialloc(String filename)
    {
        // we are limited to 30 characters, so enforce that.
        int length;
        if (filename.length() > maxChars)
        {
            length = maxChars; // length set to maxChars if its larger
        }
        else
        {
            length = filename.length();
        }
        
        // find and use the first available empty file
        for (short i = 0; i < fsizes.length; i++)
        {
            if (fsizes[i] == 0)
            {
                // use this one, empty file
                fsizes[i] = length;
                filename.getChars(0, length, fnames[i], 0);
                return i;
            }
        }
        // none available
        return -1;
    }

    // IFree
    // The given inumber is disassociated with the directory where the entry 
    // is reinitialized to zero.  The data previously contained is not 
    // destroyed, but is no longer valid.
    public boolean ifree(short iNumber)
    {
        if (fsizes[iNumber] > 0)
        {
            // get rid of the evidence
            fsizes[iNumber] = 0;
            return true;
        }
        
        return false;
    }

    // NameI
    // Locate an inode in the directory for a given filename.  
    // If the filename exists in the directory, the inumber is returned.  
    // Otherwise, an error code is generated to report that there was no match.
    public short namei(String filename)
    {
        String current;
        
        for (short i = 0; i < fsizes.length; i++) 
        {
            if (fsizes[i] == filename.length()) // search directory 
            {
                current = new String(fnames[i], 0, fsizes[i]);
                if (filename.equals(current))
                {
                    // found iNumber, filename located
                    return i;
                }
            }
        }
        // no match to filename found return -1
        return -1;
    }
}