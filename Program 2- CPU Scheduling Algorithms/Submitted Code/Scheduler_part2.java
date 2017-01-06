/**
 *
 * @author: Jasenko Cordalija
 * @class: CSS 430
 * @date: 11/02/2016
 *
 * There's a separate folder with the ThreadOS .class files professor has
 * This Project is Scheduler_part2.java , my MLFQ implementation
 */


import java.util.*;

public class Scheduler extends Thread
{
    private Vector queue0;			// new Threads TCB enqueued into this queue
	private Vector queue1;			// queue 0's non-completed execution threads moved TCB here next
	private Vector queue2;			// thread in queue1 still not completed its execution moves the TCB here
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

    // New data added to p161
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to p161
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;
    private void initTid(int maxThreads) 
	{
        tids = new boolean[maxThreads];
        for (int i = 0; i < maxThreads; i++)
            tids[i] = false;
    }

    // A new feature added to p161
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid() 
	{
        for (int i = 0; i < tids.length; i++) 
		{
            int tentative = (nextId + i) % tids.length;
            if (tids[tentative] == false) 
			{
                tids[tentative] = true;
                nextId = (tentative + 1) % tids.length;
                return tentative;
            }
        }
        return -1;
    }

    // A new feature added to p161
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid(int tid) 
	{
        if (tid >= 0 && tid < tids.length && tids[tid] == true) 
		{
            tids[tid] = false;
            return true;
        }
        return false;
    }

    // A new feature added to p161
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb() 
	{
        Thread myThread = Thread.currentThread(); // Get my thread object
        
		// Always first check for TCB start at queue0
		// Checks tcb with Thread to see if right one
		synchronized(queue0) 
		{
            for (int i = 0; i < queue0.size(); i++) 
			{
                TCB tcb = (TCB)queue0.elementAt(i);
                Thread thread = tcb.getThread();
                if (thread == myThread) 
                    return tcb;		// returns right tcb
            }
        }
		
		// After queue0 check for TCB check at queue1
		// Checks tcb with Thread to see if right one
		synchronized(queue1) 
		{
            for (int i = 0; i < queue1.size(); i++) 
			{
                TCB tcb = ( TCB )queue1.elementAt( i );
                Thread thread = tcb.getThread( );
                if (thread == myThread) 
                    return tcb;		// returns right tcb
            }
        }
		
		// After queue0 and queue1 check for TCB check queue2
		// Checks tcb with Thread to see if right one
		synchronized(queue2) 
		{
            for (int i = 0; i < queue2.size( ); i++) 
			{
                TCB tcb = (TCB)queue2.elementAt( i );
                Thread thread = tcb.getThread();
                if (thread == myThread) 
                    return tcb;		// returns right tcb
            }
        }
        return null;
    }

    // A new feature added to p161
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads() 
	{
        return tids.length;
    }
	// A constructor with no parameters
    public Scheduler() 
	{
        timeSlice = DEFAULT_TIME_SLICE;
        queue0 = new Vector();
		queue1 = new Vector();
		queue2 = new Vector();
        initTid(DEFAULT_MAX_THREADS);
    }
	// A constructor to receive parameter quantum
    public Scheduler(int quantum) 
	{
        timeSlice = quantum;
        queue0 = new Vector();
		queue1 = new Vector();
		queue2 = new Vector();
        initTid(DEFAULT_MAX_THREADS);
    }

    // A new feature added to p161
    // A constructor to receive the max number of threads to be spawned
    public Scheduler(int quantum, int maxThreads) 
	{
        timeSlice = quantum;
        queue0 = new Vector();
		queue1 = new Vector();
		queue2 = new Vector();
        initTid(maxThreads);
    }

    private void schedulerSleep() 
	{
        try {
            Thread.sleep(timeSlice);
        } catch (InterruptedException e) {
        }
    }

    // A modified addThread of p161 example
    public TCB addThread(Thread t) 
	{
        TCB parentTcb = getMyTcb(); // get my TCB and find my TID
        int pid = (parentTcb != null) ? parentTcb.getTid() : -1;
        int tid = getNewTid(); 	// acquired new TID
        if (tid == -1)
            return null;
        TCB tcb = new TCB(t, tid, pid); 
        
		// new Threads's TCB always enqueued into queue0
		queue0.add(tcb);
        return tcb;
    }

    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread() 
	{
        TCB tcb = getMyTcb();
        if (tcb!= null)
            return tcb.setTerminated();
        else
            return false;
    }

    public void sleepThread(int milliseconds) 
	{
        try {
            sleep(milliseconds);
        } catch (InterruptedException e) { }
    }

    // A modified run of p161
	// This is my main run Program
	public void run() 
	{
        Thread current = null;

        while (true) 
		{	
            try {
		   if(queue0.size() == 0 && queue1.size() == 0 && queue2.size() == 0) {
		   }
			TCB currentTCB = null;	
				// ***************** Start of Queue0***************************
				// This sections handles Queue0's thread execution
				// if threads do not complete execution for Q0 timeslice
				// corresponding TCB move to Queue1
				if (queue0.size() > 0)
				{
					currentTCB = (TCB)queue0.firstElement(); 
					if (currentTCB.getTerminated() == true) 
					{    
							queue0.remove(currentTCB);    // removes from Q0 TCB             
							returnTid(currentTCB.getTid());                            
					} 
					else {
						current = currentTCB.getThread();                
						if (current != null) 
						{                        
							if (current.isAlive()) 
							{                 
							current.resume();                       
							} 
							else { 
							// Spawn controlled by the Scheduler
							current.start();  // Scheduler starts new thread                     
							}
						}
				sleepThread(timeSlice/2);  	// timeSlice half of RR's                     
        
				synchronized (queue0)	// synchronizes queue0
				{		
					if (current != null && current.isAlive()) 
					{
						current.suspend();                     
						queue0.remove(currentTCB);  // TCB removed from Q0      
						queue1.add(currentTCB);    // then added to Q1         
					}		
				  }
				 } 
				}
			//****************** End of Queue0*********************************
				
			// ***************** Start of Queue1*******************************
			// This sections handles Queue1's thread execution
			// if threads do not complete execution for Q1 timeslice
			// corresponding TCB move to Queue2
			if(queue0.size() == 0 && queue1.size() > 0)	// check for size in Qs
			{
				currentTCB = (TCB)queue1.firstElement();   
				if (currentTCB.getTerminated() == true) 
				{    
					queue1.remove(currentTCB);   	// removes from Q1               
					returnTid(currentTCB.getTid());                                
				} 
				else {  
					current = currentTCB.getThread();               
					if (current != null) 
					{                        
						if (current.isAlive()) 
						{                 
							current.resume();                       
						} 
						else {                                    
							current.start();     // as before, scheduler starts                 
						}
					}
        
			sleepThread(timeSlice/2);  // react to new threads in q0
			// This is repeated for queue 0 since it is going back to Queue0
			// See above code for comment on this section
				if(queue0.size() > 0)  
				{ 
					if (current != null && current.isAlive())
					{
						current.suspend();                                         

					currentTCB = (TCB)queue0.firstElement(); 
					if (currentTCB.getTerminated() == true) 
					{    
						queue0.remove(currentTCB);                 
						returnTid(currentTCB.getTid());          
					} 
					else {
						current = currentTCB.getThread();              
						if (current != null) 
						{                        
						 if (current.isAlive()) 
						 {                 
							current.resume();                       
							} 
							else {                                    
							current.start();                       
							}
						}
				sleepThread(timeSlice/2);                       
        
				synchronized (queue0)
				{
					if (current != null && current.isAlive())
						{
						 current.suspend();                     
						 queue0.remove(currentTCB);           
						 queue1.add(currentTCB);              
						}
					   }
					  } 
				    }
				// This is Continuation of Queue1, which leads into Queue2	
				current.resume();                           
				}
		
				sleepThread(timeSlice/2);   // after resume execution slice/2                    
		
				// Synchronization of Queue1 and moving onto Queue 2
				synchronized (queue1) 
				{
				if (current != null && current.isAlive()) 
				{
					current.suspend();                     
					queue1.remove(currentTCB);     // removes TCB from Q1      
					queue2.add(currentTCB);     	// adds TCB from Q1 to Q2         
				}
			   }
		      }
			}
			//****************** End of Queue1*********************************
				
			// *****************Start of Queue2********************************
			// This sections handles Queue2's thread execution
			// if threads do not complete execution for Q2 timeslice
			// corresponding TCB move to tail end of Q2 
			// starts by checking both Q1 and Q0 are empty and Q2 size is not 0
            if(queue0.size() == 0 && queue1.size() == 0 && queue2.size() > 0)
			{
				currentTCB = (TCB)queue2.firstElement();   
				if (currentTCB.getTerminated() == true)
				{    
					queue2.remove(currentTCB);       	// removes from Q2 TCB          
					returnTid(currentTCB.getTid());                               
				} 
				else {
        
				current = currentTCB.getThread();              
				if (current != null)
				{                        
					if (current.isAlive()) 
					{                 
						current.resume();                       
					} 
					else {                                    
					current.start();       // starts Scheduler                
					}
				}
			// react to thread with higher priority in Q0 and Q1 timeslice/2
			sleepThread(timeSlice/2);    	        
			// This section check Queue0 and Queue1 to see if it can 
			// Enter Queue2 or go back to either of the other two.
			// Code below similar to above Q0, look for comments above
			if(queue0.size() > 0 || queue1.size() > 0)  
			{ 
				if (current != null && current.isAlive())
				{
					current.suspend();                                            
					currentTCB = (TCB)queue0.firstElement(); 
					if (currentTCB.getTerminated() == true) 
					{    
						queue0.remove(currentTCB);                 
						returnTid(currentTCB.getTid());           
					} 
					else {
					
					current = currentTCB.getThread();              
					if (current != null) 
					{                        
						if (current.isAlive()) 
						{                 
							current.resume();                       
							} 
							else {                                    
							current.start();                       
						}
					}
					sleepThread(timeSlice/2);                       
					synchronized (queue0)
					{
					if (current != null && current.isAlive()) 
					{
						current.suspend();                     
						queue0.remove(currentTCB);           
						queue1.add(currentTCB);              
					  }	
					}
				  } 
				}
				
			// This Section continues of Queue 2, and resumes suspension	
            current.resume();     // resuming execution                       
           }
		   sleepThread(timeSlice/2);     // appropriating timeslice for Q2              
		   sleepThread(timeSlice);  

			//This section is responsbile for Synchronizing Queue2
			//However this one does not remove and add to Queue3 rather
			//Thread is put into the back of Queue2
			synchronized (queue2) 
			{
            if (current != null && current.isAlive()) 
			{
                current.suspend();                     
                queue2.remove(currentTCB);       // Removes TCB from    
                queue2.add(currentTCB);            // Adds TCB to tail end Q2
            }
          }
	    }					
      } //****************** End of Queue2*************************************
	} catch (NullPointerException e3) { };
   }
  }						
}