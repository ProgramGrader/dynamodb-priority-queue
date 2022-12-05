If you want an assignment you can get it based ID
If you want to delete an assignment you can delete it based off ID

You can also enqueue and dequeue however these do not remove the value from dynamodb only from the queue 

So you must first put item into table, using the sdk THEN queue it with enqueue function 

Databases of sdks acts as a record holder and queue persistence 
