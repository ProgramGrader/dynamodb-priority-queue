package com.awsblog.queueing

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.awsblog.queueing.appdata.AssignmentItem
import com.awsblog.queueing.appdata.DatabaseItem
import com.awsblog.queueing.appdata.DatabaseItemData
import com.awsblog.queueing.sdk.Database
import com.awsblog.queueing.sdk.Dynamodb
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.quarkus.test.junit.QuarkusTest
import java.time.LocalDate

@QuarkusTest

class DynamoDBTests : AnnotationSpec() {

    val endpoint = EndpointConfiguration("http://localhost:4566/", "local")
    val client: Database? = Dynamodb.Builder()
        .withRegion("us-east-2")
        .withTableName("assignment_schedule")
        .build(endpoint)

    // Data to Insert
    final val id1 = "A-101"
    val date1: LocalDate = LocalDate.now().plusDays(10)
    val assignment1 = DatabaseItem(id1, date1.toString())

    final val id2 = "A-202"
    final val date2: LocalDate = LocalDate.now().plusDays(2)
    val assignment2 = DatabaseItem(id2, date2.toString())

    final val id3 = "A-303"
    val date3: LocalDate = LocalDate.now().plusDays(3)
    val assignment3 = DatabaseItem(id3, date3.toString())



//    @Before
//    fun insertItems(){
//        client!!.put(assignment1)
//        client!!.put(assignment2)
//        client!!.put(assignment3)
//    }

//    @After
//    fun deleteItems(){
//        client!!.delete(id1)
//        client!!.delete(id2)
//        client!!.delete(id3)
//    }

    @Test
    fun `putting, getting and deleting from DynamoDB`(){

        //get
        ( client!![id1])?.id.shouldBe(assignment1.id)
        ( client!![id2])?.id.shouldBe(assignment2.id)
        ( client!![id3])?.id.shouldBe(assignment3.id)

        //delete
        client!!.delete(id1)
        client!!.delete(id2)
        client!!.delete(id3)

        val a = (client!![id1])

        a?.id.shouldBe(null)
        (client!![id2])?.id.shouldBe(null)
        (client!![id3])?.id.shouldBe(null)

    }


    @Test
    fun `enqueue items to Dynamodb priority queue`(){
// currently attribute needs to be defined in database item for it to be enqueued not what we want :(
        client!!.enqueue(assignment1)
        client!!.enqueue(assignment2)
        client!!.enqueue(assignment3)

        client!!.getQueueStats().totalRecordsInQueue.shouldBe(3)

    }


    @Test
    fun `dequeue items from DynamoDB priority queue and assert correct order`() {

        client!!.enqueue(assignment1)
        client!!.enqueue(assignment2)
        client!!.enqueue(assignment3)

        val dequeuedDatabaseItem = client!!.dequeue(3)

        val earliestDate = LocalDate.parse((dequeuedDatabaseItem[0]).schedule)//.to(Assignment))
        val midDate = LocalDate.parse((dequeuedDatabaseItem[1]).schedule)
        val latestDate = LocalDate.parse((dequeuedDatabaseItem[2]).schedule)

        (earliestDate < midDate).shouldBeTrue()
        (earliestDate < latestDate).shouldBeTrue()
        (midDate < latestDate).shouldBeTrue()
        client!!.getQueueStats().totalRecordsInQueue.shouldBe(0)
    }

    @Test
    fun `peek top of DynamoDB priority queue and assert top value is earliest date`() {
        client!!.enqueue(assignment1)
        client!!.enqueue(assignment2)
        client!!.enqueue(assignment3)

        (client!!.peek(1)[0]).id.shouldBe(id2)    }

    @Test
    fun `remove items from queue and restore them back to DynamoDB priority queue`(){
        client!!.enqueue(assignment1)
        client!!.enqueue(assignment2)
        client!!.enqueue(assignment3)
        client!!.getQueueStats().totalRecordsInQueue.shouldBe(3)
        client!!.remove(id1)
        client!!.remove(id2)
        client!!.remove(id3)
        client!!.getQueueStats().totalRecordsInQueue.shouldBe(0)

        (client!![id1])?.id.shouldNotBeBlank()
        (client!![id2])?.id.shouldNotBeBlank()
        (client!![id3])?.id.shouldNotBeBlank()

        client!!.restore(id1)
        client!!.restore(id2)
        client!!.restore(id3)
        client!!.getQueueStats().totalRecordsInQueue.shouldBe(3)

    }

    @Test
    fun `peek n items from queue and dequeuing n items `(){
        client!!.enqueue(assignment1)
        client!!.enqueue(assignment2)
        client!!.enqueue(assignment3)
        val top3PeekedItems = client!!.peek(3)
        val top3DequeuedItems = client!!.dequeue(3)

        ((top3PeekedItems.get(0)).id == assignment2.id).shouldBeTrue()
        ((top3PeekedItems.get(1)).id == assignment3.id).shouldBeTrue()
        ((top3PeekedItems.get(2)).id == assignment1.id).shouldBeTrue()

        (top3DequeuedItems.get(0).id == assignment2.id).shouldBeTrue()
        (top3DequeuedItems.get(1).id == assignment3.id).shouldBeTrue()
        (top3DequeuedItems.get(2).id == assignment1.id).shouldBeTrue()
    }

    @Test
    fun `retrieve items from queue`(){
        client!!.enqueue(assignment1)
        client!!.enqueue(assignment2)
        client!!.enqueue(assignment3)

        (client!!.retrieve(id1).resultObject)?.id.shouldBe(id1)
        (client!!.retrieve(id2).resultObject)?.id.shouldBe(id2)
        (client!!.retrieve(id3).resultObject)?.id.shouldBe(id3)
    }


    /**
     *
     * All that is left is to clean this up,
     * Grab the values from the map and put it into its respected item.
     *
     */


    @Test
    fun `putting and getting items from data field`() {

        val data = DatabaseItemData()
        data.item = (AssignmentItem("Hello_World", "2017-01-22"))
        assignment1.setData(data.item as AssignmentItem) // Assignment item is not accepted here even though it implements DynamoDbTypeConverter
        client?.put(assignment1)

        client!![assignment1.id]?.id.shouldBe(assignment1.id)
        val returnedData = client!![assignment1.id]?.getData() as AssignmentItem
        returnedData.id.shouldBe("Hello_World")

    }
}