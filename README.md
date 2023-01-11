## Date Priority Queuing

Provides a Dynamodb table and sdk that allows you to prioritize items based off of date
in descending order. 

## Quick start

Install hashicorp terraform https://www.terraform.io/ then 
Generate DynamoDB table in test/terraform with terraform init then terraform apply. 

If you are testing referer to the How to test? section below 

## Overview

The Dynamodb implementation for this priority queue uses a global secondary index (key: queued, range: schedule) 
called scheduled-index, a sparse index (queued) and DynamoDB's built in ability to order range keys

currently this implementation allows items to be inserted into the table without being inserted into the queue
this is possible because the gsi includes the sparse index "queued", only items with the queued attribute will appear in the 
gsi, and items are only recognized as being in the queue if queued=1

## How to Test

### Tools Needed:
- terraform and tflocal - ``` pip install tflocal```  then place directory containing tflocal executable in your PATH.
- NoSQLWorkbench: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/workbench.settingup.html


### Getting Started
1. ```docker-compose up``` to update and start the local stacks container with our local Dynamodb instance
   which default to http://localhost:4566/ therefore to connect to this instance with NoSQLWorkbench set the port to 4566

2. cd into the terraform directory and run commands: ```tflocal init; tflocal apply -auto-approve``` this creates the table
   priority_queue_table in the local Dynamodb instance.
3. When creating a Client to connect to dynamodb pass endpoint as a parameter to the build function see examples in
   already completed tests.


## Notes
- Whenever you start a new instance of your localstack container you need to rebuild the table in NoSql Workbench.
- If you run into a region issue when trying to ```tflocal apply``` set the AWS_DEFAULT_REGION=us-east-2 or any acceptable
  aws region in your terminal then run ```tflocal apply```

## Credit 
The general idea for this projects architecture 
was pulled from https://aws.amazon.com/blogs/database/implementing-priority-queueing-with-amazon-dynamodb/
