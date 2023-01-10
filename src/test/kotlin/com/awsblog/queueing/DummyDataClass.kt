package com.awsblog.queueing

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter
import kotlinx.serialization.Serializable
import kotlin.properties.Delegates

@Serializable
data class DummyDataClass(

    val testString: String,

    val testInt: Int
)