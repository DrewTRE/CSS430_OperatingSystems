#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/wait.h>
#include <iostream>

using namespace std;

int main(int argc, char* argv[]) {

	enum {RD, WR};			// for pipe fd1 & fd2, ReadEnd- index RD=0, WrtieEnd- WR=1
	int filedes1[2];		// Creates pipe 
	int filedes2[2];		// Creates pipe #2, 
	
	pid_t pid;
		
	// File descriptors, pipes checked for error
	if(pipe(filedes1) < 0)
	{
		perror ("pipe error");
	}
	
	if(pipe(filedes2) < 0) 
	{
		perror ("pipe error");
	}
	
	if ((pid = fork()) < 0) 
	{ 
		perror ("pipe error");
	}
	
	else if (pid == 0) 			// Child is spawned
	{			
		pid = fork();
		
		if (pid == 0) 			// GrandChild is spawned
		{				
			pid = fork();
			
			if (pid == 0) 		// GreatGrantChild is spawned
			{			
				
				// Read-end/write-end closed of filedes1 & 2
				close(filedes1[RD]);
				close(filedes1[WR]);
				close(filedes2[WR]);
				dup2(filedes2[RD], 0);
				
				execlp("/usr/bin/wc", "wc", "-l", NULL);
			
			}else {
				// Read-end/write-end closed of filedes1 & 2
				close(filedes1[WR]);
				dup2(filedes1[RD], 0);
				close(filedes2[RD]);
				dup2(filedes2[WR], 1);
				//close(filedes1[WR]);
				//close(filedes2[WR]);
				execlp("/bin/grep", "grep", argv[1], NULL);
			}
				
		} else {
				
				//close(filedes2[RD]); // closes fileDS2 read "[0]"
				dup2(filedes1[WR],1); // stdout, GGC pipe writes
				close(filedes1[RD]); // closes fileDS2 read "[0]"
				execlp("/bin/ps", "ps", "-A", NULL); // replace curr process image with new from loaded file
				
			   }						
	}
		else {	
			// Parent's only command to wait
			wait(NULL); 
		}
		// returned 0 for success
		return 0;
		exit(EXIT_SUCCESS); // maybe use needed
}
		
	
	
	
	
