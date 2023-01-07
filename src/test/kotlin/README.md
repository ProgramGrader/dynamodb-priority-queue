# Testing using local stacks
## Tools Needed: 
- terraform and tflocal - ``` pip install tflocal```  then place directory containing tflocal executable in your PATH. 
- NoSQLWorkbench: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/workbench.settingup.html


## Getting Started
1. ```docker-compose up``` to update and start the local stacks container with our local Dynamodb instance
which default to http://localhost:4566/ therefore to connect to this instance with NoSQLWorkbench set the port to 4566

2. cd into the terraform directory and run commands: ```tflocal init; tflocal apply -auto-approve``` this creates the table 
assignment_schedule in the local Dynamodb instance. 
3. When creating a Client to connect to dynamodb pass endpoint as a parameter to the build function see examples in 
already completed tests. You can now test this sdk locally. 


## Notes
Whenever you start a new instance of your localstacks container you need to rebuild the table in NoSql Workbench. 




