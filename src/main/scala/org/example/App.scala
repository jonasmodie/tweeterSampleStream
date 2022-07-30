package org.example
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{countDistinct, explode, sum, to_timestamp}
import org.apache.spark.sql.types.{ArrayType, DataType, LongType, MapType, StructField}

import java.io.File
import scala.io.Source

/**
 * @author ${user.name}
 */
import org.example.TwitterApiConnecter
import org.json.JSONObject

object App {
  
  def foo(x : Array[String]) = x.foldLeft("")((a,b) => a + b)

  val spark = SparkSession.builder()
    .master("local[*]")
    .appName("Tweets aggregates")
    .getOrCreate()

  import org.apache.spark.sql.types.{StringType, StructType}

  def main(args : Array[String]) {

    // arguments
    val recordLimit = 50
    val schemaFilePath = "/home/jonas/Academics/twitterAnalyser/config/schema.json"

    val schemaSource = Source.fromFile(schemaFilePath).getLines.mkString
    val schemaFromJson = DataType.fromJson(schemaSource).asInstanceOf[StructType]
    println("schemaFromJson : " + schemaFromJson)

    val twitterInterface = new TwitterApiConnecter()
    print("getting tweets ")
    val jsonObjectsList = twitterInterface.getFilteredStream(recordLimit)
    import spark.implicits._

    val tweetsDf = spark.sparkContext.parallelize(jsonObjectsList).toDF()
//    println("tweetsDf : " + tweetsDf)


    import org.apache.spark.sql.functions.{col,from_json}
    val dfJSON = tweetsDf.withColumn("jsonData",from_json(col("value"),schemaFromJson))
      .select("jsonData.*")
    dfJSON.printSchema()
    dfJSON.show(false)

    val tweetsDataDf = dfJSON.select("data.*","includes.*")
    println("Tweets schema : " )
    println(tweetsDataDf.withColumn("author_id",col("created_at").cast(LongType)).schema.prettyJson)
//    tweetsDataDf.printSchema()
    tweetsDataDf.show(false)
    println("tweets data")


    val entitiesDf = tweetsDataDf
      .select("author_id","created_at","id","text","lang","public_metrics","organic_metrics","places","users","entities.*")
      .where(col("hashtags").isNotNull
      )
    entitiesDf.printSchema()
    entitiesDf.show(false)
    val hashTagsMetrics = entitiesDf
      .withColumn("tag",explode(col("hashtags.tag")))
      .withColumn("user",explode(col("users")))
      .groupBy("tag","lang")
      .agg(
        countDistinct("user.id").alias("users_count"),
        countDistinct("lang").alias("languages_count"),
        sum("public_metrics.retweet_count").alias("total_retweet_count"),
        sum("public_metrics.reply_count").alias("total_reply_count"),
        sum("public_metrics.like_count").alias("total_like_count")
      )

    hashTagsMetrics.show(false)

  }

}

