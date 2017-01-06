/**
 *
 * @author: Jasenko Cordalija
 * @class: CSS 430
 * @date: 11/20/2016
 *
 * This Project is Cache.java which is submitted alongside Test4.java
 */

import java.util.*;
import java.io.*;

public class Cache 
{ 
	private int newVictim;		// this is the victim choosen to be sacrificed								
	private int blockSize;		
	private Entry[] pageTable = null;	// My pageTable for Cache Entries
	private int foundP;			// variable that get assigned a located victim through enhanced second chance

//******************* Private Class Entry method ******************************
    private class Entry 	
	{ 
		int blockFrameNumber;
		boolean referenceBit;
		boolean dirtyBit;
		byte[] dataBlock;
	
		private Entry(int blockSize) 
		{
		dataBlock = new byte[blockSize];
		blockFrameNumber = -1;	// Disk block number of Cache data, default set to -1
        referenceBit = false;	// reference used to determine if block is accessed, defualt set to false
        dirtyBit = false;	// Identifies if write request has occured, set to true. default is set to false
		}
    }
//******************* End of Private Entry Class method ***********************

//******************* findBlock & findFreePage method *************************
	// findFreePage searches through the entire pageTable to
	// locate a free page that is available. A free page's frame 
	// is -1 if it is free.Upon finding a free page it returns the index
	// location within the pageTable
    private int findFreePage() 
	{
		for (int i = 0; i < pageTable.length; i++) 
		{	
            if (pageTable[i].blockFrameNumber == -1)	// found empty page
			{
			return i;	// empty page located and returned its location in pageTable
			}
        }
        return -1;	// return -1 indicating that there is no free page available
    }
	
	
	// findBlock searches through the entire pageTable to
	// locate a block frame number that matches the blockId passed
	// into it's parameter. Upon finding a match it returns the 
	// location of match in pageTable
	private int findBlock(int findingBlockId) 
	{
		for (int i = 0; i < pageTable.length; i++) 
		{	
            if (pageTable[i].blockFrameNumber == findingBlockId)	//found match
			{
			return i;	// blockId located and returned 
			}
        }
        return -1;	// returns -1 indicating that there is no match 
    }
//******************* End of findBlock & findFreePage method ******************

//******************* nextVictim & WriteBack method ***************************
	// My Enhanced Second Chance Algorithm
	
	private int nextVictim() 
	{
		// creates continous loop eventually allocating a victim with no refBit
		// acts much like a circular queue going indefinently until victim is 
		// located, this may take multiple attempts in going through page Table
        while(true)	
		{
			// A victim is choosen incrementally to be checked if
			// its reference Bit is 0 (false), if the victim had a reference
			// Bit, their bit is changed to 0 and left alone this time around
			// hence giving it a second chance. Going around if dirtyBit is set
			// writeback handles writting it to disk
			newVictim = ((newVictim++) % pageTable.length); 
			if(pageTable[newVictim].referenceBit != true) // victim with refBit 0
			{
				return newVictim;	// without a reference bit this victim is choosen
			}
			// all other potential victims traversed have their referenceBit set 
			// to 0 from 1 (false), thus removing their chance of survival next 
			// loop around pageTable
			pageTable[newVictim].referenceBit = false;	
		}
    }

	// writeBack is the second part of my Enhanced Second Chance Algorithm.
	// writeback checks if the victim in its parameter has a dirty (modify) bit
	// set to 1 (true), indicating it needs to be written to Disk before it is 
	// removed from pageTable and once that is done, the dirtyBit is set back
	// to its default of 0 (false)
    private void writeBack(int victimEntry) 
	{
		if(pageTable[victimEntry].dirtyBit) // checks if dirty bit is true from victim
		{
		SysLib.rawwrite(pageTable[victimEntry].blockFrameNumber, pageTable[victimEntry].dataBlock);
		pageTable[victimEntry].dirtyBit = false; // dirty bit set to 0 again after write to Disk
		}
    }
//******************* End of nextVictim & WriteBack method ********************

//******************* Cache Constructor ***************************************
    public Cache(int blockSize, int cacheBlocks) 
	{
		this.blockSize = blockSize;	// sets size of blocks 
		pageTable = new Entry[cacheBlocks];	// sets size of pageTable entry
		for (int i = 0; i < pageTable.length; i++) 
		{
			pageTable[i] = new Entry(this.blockSize); 
		}	
    }
//******************** End of Cache Constructor *******************************
	
//************************ READ method ****************************************
    public synchronized boolean read(int blockId, byte buffer[]) 
	{
		if(blockId > 1000)
		{
			return false;
		}
		// searches pageTable array of 10 elements for corresponding block	
		// matching blockId to frame number of element
		foundP = findBlock(blockId);				
		if(foundP != -1)
		{ 	
			pageTable[foundP].blockFrameNumber = blockId;	
			
			// found elements data copied onto buffer in 2nd argument of 'read'
			System.arraycopy(pageTable[foundP].dataBlock, 0, buffer, 0, blockSize);	
			pageTable[foundP].referenceBit = true; // referenceBit changed to true
			return true; 
		}
		
		// If corresponding block was not found, then search pageTable array 
		// of 10 elements for invalid element whose	frame number is -1
		foundP = findFreePage();					
		if(foundP != -1)
		{ 	
			// rawread, will read corresponding disk block from the disk into the
			// newly choosen invalid element. Followed by an update to its frame 
			// number and setting the reference bit to 1 (true)
			SysLib.rawread(blockId, pageTable[foundP].dataBlock); 
			pageTable[foundP].blockFrameNumber = blockId;				
			pageTable[foundP].referenceBit = true;	
			
			// content is copied to buffer identified in 2nd argument of 'read'
			System.arraycopy(pageTable[foundP].dataBlock, 0, buffer, 0, blockSize);	
			return true;
		}		
		
		// couldnt find corresponding block and any invalid elements then
		// victim is chosen using the Enhanced Second Choice Algorithm 
		// as sacrifice. Using writeBack. 
		// if new victim has a dirty bit of 1 (true), content written back to
		// Disk, following then by resetting dirty bit to 0 (false)
		writeBack(nextVictim());	

		// rawread, will read corresponding disk block from the disk into the
		// newly choosen victim/ element. Followed by an update to its frame 
		// number and setting the reference bit to 1 (true)
		SysLib.rawread(blockId, pageTable[newVictim].dataBlock);	     	
		pageTable[newVictim].blockFrameNumber = blockId;				
		pageTable[newVictim].referenceBit = true;	
		
		// content is copied to buffer identified in 2nd argument of 'read'
		System.arraycopy(pageTable[newVictim].dataBlock, 0, buffer, 0, blockSize);
		return true;
	}

	
//*********************** End of Read method **********************************	
	
//************************ WRITE method ***************************************	
    public synchronized boolean write(int blockId, byte buffer[]) 
	{
		if(blockId > 1000)
		{
			return false;
		}
		// Like in 'read' method, write starts by searching 
		// pageTable array of 10 elements for corresponding block	
		// matching blockId to frame number of element
		foundP = findBlock(blockId);					
		if(foundP != -1)
		{ 	
			// buffer identified in 2nd argument content copied to matched 
			// element/block in pageTable. Then dirtybit and referenceBit set
			// to 1 (true), and blockId updates elements frame number
			System.arraycopy(buffer, 0, pageTable[foundP].dataBlock, 0, blockSize);
			pageTable[foundP].dirtyBit = true;			
			pageTable[foundP].blockFrameNumber = blockId;				
			pageTable[foundP].referenceBit = true;	
			return true; 
		}	
		
		// If corresponding block was not found, then search pageTable array 
		// of 10 elements for invalid element whose	frame number is -1
		foundP = findFreePage();					
		if(foundP != -1)
		{ 	
			// buffer content copied to matched free page/ invalid element. 
			// Then dirtybit and referenceBit set to 1 (true), 
			// and blockId updates elements frame number 
			System.arraycopy(buffer, 0, pageTable[foundP].dataBlock, 0, blockSize);
			pageTable[foundP].dirtyBit = true;			
			pageTable[foundP].blockFrameNumber = blockId;				
			pageTable[foundP].referenceBit = true;	
			return true; 
		}
		
		// couldnt find corresponding block and any invalid elements then
		// victim is chosen using the Enhanced Second Choice Algorithm 
		// as sacrifice. Using writeBack. 
		// if new victim has a dirty bit of 1 (true), content written back to
		// Disk.
		writeBack(nextVictim());	

		// dirtyBit and referenceBit are set to 1 (true)
		// and blockId updates elements frame number
		pageTable[newVictim].dirtyBit = true;			
		pageTable[newVictim].blockFrameNumber = blockId;				
		pageTable[newVictim].referenceBit = true;

		// buffer content copied to matched free page/ invalid element.
		System.arraycopy(buffer, 0, pageTable[newVictim].dataBlock, 0, blockSize);
		return true;		
    }
//************************ End of WRITE method ********************************

//************************ SYNC method ****************************************
	// sync, writes back all dirty blocks to Disk without creating any
	// changes to pageTable's frame number or referenceBit
    public synchronized void sync() 
	{
		for(int i = 0; i < pageTable.length; i++)
		{
			writeBack(i);
		}
		SysLib.sync();
    }
//*****************************************************************************
	
//************************  FLUSH  ********************************************
	// flush, writes back all dirty blocks to Disk also wipes empty all 
	// cached blocks setting their block frame number to default -1 and 
	// reference bit to default 0 (false).
	public synchronized void flush() 
	{
		for(int i = 0; i < pageTable.length; i++)
		{ 	
			writeBack(i);
			pageTable[i].blockFrameNumber = -1;			
			pageTable[i].referenceBit = false;		
		}
		SysLib.sync();
    }
//*********************************************************************************
}