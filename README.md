
## Amazon Dynamodb Priority Queuing

This priority queue is currently configured for the assignment_grader dynamodb table
It provides a sdk that allows you to put items in dynamodb delete them and create a queue off those elements

The priority of the queue is ordered by oldest to the newest date in the schedule column 

view the test directory for examples on usage.  

## Improvements That Would Be Nice

This priority queue only works for our specific use case, the sdk should be reconfigured to accept generic objects and there configurations instead of specific ones 
adopters of this queue would then only need to define there configurations