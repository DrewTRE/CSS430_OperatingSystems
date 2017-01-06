/**
 *
 * @author: Jasenko Cordalija
 * @class: CSS 430
 * @date: 11/20/2016
 *
 * This Project is Test4.java which is submitted alongside Cache.java
 */

 
import java.util.*;
import java.io.*;
import java.util.Date;
import java.util.Random;

public class Test4 extends Thread 
{
	//Cache has array of 10 elements, one for each block. each block has frame/ref/dirty
	private int cachedBlocks = 10;	
	private int arrayTest = 200;	// 200 reads and 200 writes for testing 
	private int diskBlockSize = 512; // each elements/block has  size of 512	
	private int testing;
	private String currStatus = "disabled";	// displays in results if Cache was enabled or disabled
	private boolean cacheEnabled = false;	// identifies if cache was enabled
	private byte[] readBuffer;	// byte buffer for reads
	private byte[] writeBuffer;	// byte buffer for writes
	private Random r;
	
	// These 4 are aid to calculate AVG write and read times of each Access
	private long startWriteTime;                           
    private long stopWriteTime;                            
    private long startReadTime;                            
    private long stopReadTime;  
	
///****************** Test4 *****************************************************************************
	public Test4(String[] args) 
	{
		// Assigns both byte arrays disk block size 512
		writeBuffer = new byte[diskBlockSize];               
        readBuffer = new byte[diskBlockSize]; 
		r = new Random();
		//r.nextBytes(writeBuffer); // randomly filles writeBuffer with data for use in testing
				
		
		// Checks user 1st argument to know if Cache is enabled or disabled
		if(args[0].equals("enabled"))	
		{
			cacheEnabled  = true;	// Cache is enabled by 1st argument 
			currStatus = "enabled";	// current status of Cache will display enabled
		}
		else
		{
			cacheEnabled  = false;	// Cache is disabled by 1st argument 
			currStatus = "disabled"; // current status of Cache will display disabled
		}
		
		// user Second argument determine which Access test will be run 
        testing = Integer.parseInt(args[1]);
        
    }
//*******************************************************************************************************

//****************** RUN ********************************************************************************
	// run method will execute test by cases, each case 1-4
	// represents a number 1-4 in user's second argument indicating
	// which access test will be used.
	public void run( ) 
	{
    	SysLib.flush();	// empties/flushes out previous data in Cache block								
    	switch(testing)
		{                               
    		case 1:	
			{
				randomAccess(); 	
				break;        
			}
			
    		case 2:
			{
				localizedAccess();	
				break;        
			}
			
    		case 3:  
			{
				mixedAccess(); 	
				break;        
			}
			
    		case 4:  
			{
				adversaryAccess(); 	
				break;        
			}	      			  			  			  			  			
    	}
		
		//sync back together with disk, depending on wether the user
		//enabled or disabled Cache will determine if we sync using
		// csync (cache enabled), or sync (cache disabled)
		if(cacheEnabled)
		{
			SysLib.csync();		
		}
		else
		{
			SysLib.sync();
		}
		SysLib.exit( );  // thread exit at end of run, run completed now                              
    }
//*******************************************************************************************************


//****************** reader & writer Methods ************************************************************
	// reader method aids in determing wether to call 'cread' or 'rawread'
	// depending on wether the Cache is enabled or disabled in testing
	private void reader(int blockId, byte[] buffer)
	{
		if (cacheEnabled)
		{
			SysLib.cread(blockId, buffer);
		}
		else
		{
			SysLib.rawread(blockId, buffer);
		}
	}
	
	
	// writer method aids in determing wether to call 'cwrite' or 'rawwrite'
	// depending on wether the Cache is enabled or disabled in testing
	private void writer(int blockId, byte[] buffer)
	{
		if (cacheEnabled)
		{
			SysLib.cwrite(blockId, buffer);
		}
		else
		{
			SysLib.rawwrite(blockId, buffer);
		}
	}
	

//****************** RandomAccess ***********************************************************************
	// randomAccess, reads and writes blocks across the disk randomly for
	// 200 times (arrayTest). Then will check disk validity, if both 
	// writer and reader are equal to each other. Followed by
	// outputting performance results, AVG time for writing and reading 
	private void randomAccess()
	{
		//Array used for randomAccess, random numbers upto 512 will be 
		//assigned at each index in array totaling 200 (0-199)
		r.nextBytes(writeBuffer); // randomly filles writeBuffer with data for use in testing
		int[] randomAccessArr = new int[arrayTest];
		for(int i = 0; i < arrayTest; i++)
		{
			randomAccessArr[i] = Math.abs(r.nextInt() % diskBlockSize);
		}
		
		startWriteTime = new Date().getTime();	// timer start for writer
		for (int i = 0; i < arrayTest; i++)
		{
			// writes to random locations in disk 200 times
			writer(randomAccessArr[i], writeBuffer); 
		}
		stopWriteTime = new Date().getTime(); // timer stops for writer
		
		
		startReadTime = new Date().getTime();	// timer starts for reader
		for(int i = 0; i < arrayTest; i++)
		{	
			// reads from random locations in disk 200 times
			reader(randomAccessArr[i], readBuffer);
		}
		stopReadTime = new Date().getTime();	// timer stops for reader
		
		// DISK VALIDITY, verifies correctness of disk cache
		// Here we check both blocks read & write Bytes are equal to another
		// if they are not equal, print out Validity error
		if(!(Arrays.equals(writeBuffer, readBuffer)))	
		{
			 SysLib.cout("DISK VALIDITY ERROR: writerBytes and readBuffer equal\n");
		}
		
		// This section is responsible for the output Access avg time for read 
		// and write to disk, as well as if Cache was enabled or disabled and 
		// which access was run in test indicated by user arguments 
		SysLib.cout("Testing with randomAccess(), using Test Case:" + testing + "\n");
		SysLib.cout("Cache: [" + currStatus + "] \n");
		SysLib.cout("Average time for Write is: " + ((stopWriteTime - startWriteTime) / 200) + " milliseconds \n");
		SysLib.cout("Average time for Read is: " + ((stopReadTime - startReadTime) / 200)+ " milliseconds \n");
	}
//*******************************************************************************************************

//****************** localizedAccess ********************************************************************

	// localizedAccess access 10 blocks repeatedly reading and writing
	// to this small selection of blocks many times
	private void localizedAccess()
	{    r.nextBytes(writeBuffer); // randomly filles writeBuffer with data for use in testing
		startWriteTime = new Date().getTime();		// timer starts for writer
        for(int i = 0; i < arrayTest; i++)	//runs 200
		{                
          for(int j = 0; j < cachedBlocks; j++)	//runs 10x , only 10 blocks accessed
			{                  
			writer(j, writeBuffer); // only 10 blocks accessable ~0-9,  
			}
		}
        stopWriteTime = new Date().getTime(); 		// timer stops for writer

		startReadTime = new Date().getTime();		// timer starts for reader
        for(int i = 0; i < arrayTest; i++)
		{                
         for(int j = 0; j < cachedBlocks; j++) //10, a block of 10, 20 times
		{  
			reader(j, readBuffer);
		}
        }
		stopReadTime = new Date().getTime();		// timer stops for reader
		
		
		// DISK VALIDITY, verifies correctness of disk cache
		// Here we check both blocks read & write Bytes are equal to another
		// if they are not equal, print out Validity error
		if(!(Arrays.equals(writeBuffer, readBuffer)))	
		{
			 SysLib.cout("DISK VALIDITY ERROR: writerBytes and readBuffer equal\n");
		}
		
		
		// Performance output, explained in RandomAccess() section
		SysLib.cout("Testing with localizedAccess(), using Test Case:" + testing + "\n");
		SysLib.cout("Cache: [" + currStatus + "] \n");
		SysLib.cout("Average time for Write is: " + ((stopWriteTime - startWriteTime) / 200) + " milliseconds \n");
		SysLib.cout("Average time for Read is: " + ((stopReadTime - startReadTime) / 200)+ " milliseconds \n");
    }
//*******************************************************************************************************

//****************** mixedAccess ************************************************************************
 
	private void mixedAccess()
	{ 
		r.nextBytes(writeBuffer); // randomly filles writeBuffer with data for use in testing
		// Array mixedAccessArr holds localized accesses and randomized
		// accesses. Array ensures 90% are localized accesses and 10% are 
		// randomized accesses of the total Disk Operations. 
		int[] mixedAccessArr = new int[arrayTest];            
        for(int i = 0; i < arrayTest; i++)
		{   
			// randomly pick a number 0-9, 8 and less indicated
			// localized access, and a 9 indicates a randomized access
			// for disk operation, this is done 200 times and stored in array
            if(Math.abs(r.nextInt() % cachedBlocks) <= 8) 
			{
				// if localized access, a random localized blockId stored
                mixedAccessArr[i] = Math.abs(r.nextInt() % cachedBlocks);  // %10           
            } 
			else 
			{   // if randomized access, a randomized blockId upto 512 blockId stored       
                mixedAccessArr[i] = Math.abs(r.nextInt() % diskBlockSize);  // %512         
            }            
        }
		
		startWriteTime = new Date().getTime();			// timer starts for writer
        for(int i = 0; i < arrayTest; i++)
		{    
			 // mixedAccess array writes buffer blocks, writeBuffer 
             writer(mixedAccessArr[i], writeBuffer);              
        }
        stopWriteTime = new Date().getTime();			// timer stops for writer	

		startReadTime = new Date().getTime();			// timer starts for reader
        for(int i = 0; i < arrayTest; i++)
		{            
			// mixedAccess array reads buffer blocks, readBuffer 
            reader(mixedAccessArr[i], readBuffer);                         
        }
        stopReadTime = new Date().getTime();			// timer stops for reader
		
		
		// DISK VALIDITY, verifies correctness of disk cache
		// Here we check both blocks read & write Bytes are equal to another
		// if they are not equal, print out Validity error
		if(!(Arrays.equals(writeBuffer, readBuffer)))	
		{
			 SysLib.cout("DISK VALIDITY ERROR: writerBytes and readBuffer equal\n");
		}
		
		// Performance output, explained in RandomAccess() section
		SysLib.cout("Testing with mixedAccess(), using Test Case:" + testing + "\n");
		SysLib.cout("Cache: [" + currStatus + "] \n");
		SysLib.cout("Average time for Write is: " + ((stopWriteTime - startWriteTime) / 200) + " milliseconds \n");
		SysLib.cout("Average time for Read is: " + ((stopReadTime - startReadTime) / 200)+ " milliseconds \n");
    }
//*******************************************************************************************************
	
//****************** adversaryAccess ********************************************************************
	// adversaryAccess() purposely access blocks in cache to generate miss
	private void adversaryAccess() 
	{	r.nextBytes(writeBuffer); // randomly filles writeBuffer with data for use in testing
        startWriteTime = new Date().getTime();   		// timer starts for writer
        for (int i = cachedBlocks; i < diskBlockSize; i++) // starting at 10, avoiding the local accessed ones
		{                                 
            writer(i, writeBuffer);        // writes into buffer non-loclized blockIds                  
        }        
        stopWriteTime = new Date().getTime();  			// timer stops for writer
		
        startReadTime = new Date().getTime();           // timer starts for reader            
        for (int i = cachedBlocks; i < diskBlockSize; i++) 
		{              
            reader(i, readBuffer);                             
        }        
        stopReadTime = new Date().getTime();    		// timer stops for reader
		
        
		// DISK VALIDITY, verifies correctness of disk cache
		// Here we check both blocks read & write Bytes are equal to another
		// if they are not equal, print out Validity error
		if(!(Arrays.equals(writeBuffer, readBuffer)))	
		{
			 SysLib.cout("DISK VALIDITY ERROR: writerBytes and readBuffer equal\n");
		}
		
		// Performance output, explained in RandomAccess() section
		SysLib.cout("Testing with adversaryAccess(), using Test Case:" + testing + "\n");
		SysLib.cout("Cache: [" + currStatus + "] \n");
		SysLib.cout("Average time for Write is: " + ((stopWriteTime - startWriteTime) / 200) + " milliseconds \n");
		SysLib.cout("Average time for Read is: " + ((stopReadTime - startReadTime) / 200)+ " milliseconds \n");

    }
//*******************************************************************************************************
}