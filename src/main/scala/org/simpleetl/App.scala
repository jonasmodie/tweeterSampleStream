package org.simpleetl
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{countDistinct, explode, sum, to_date, to_timestamp}
import org.apache.spark.sql.types.{ArrayType, DataType, LongType, MapType, StructField, StructType}

import java.io.File
import scala.io.Source
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import scopt.OptionParser

/**
 * @author ${user.name}
 */
import org.simpleetl.TwitterApiConnecter
import org.json.JSONObject

object App {

  case class Paramaters(
                       configFilePath: String = "",
                       publishFolderPath: String = ""
                       )
  val parser : OptionParser[Paramaters] = new OptionParser[Paramaters]("tweeterSampleStream"){
    head("tweeterSampleStream","1.x")
    opt[String]("configFilePath")
      .action((x,c)=>c.copy(configFilePath = x))
      .text("path to config file")
    opt[String]("publishFolderPath")
      .action((x,c)=>c.copy(publishFolderPath = x))
      .text("path to publish folder")
  }

  val spark = SparkSession
    .builder()
//    .master("yarn")    // for cluster deployment
    .master("local[*]")
    .appName("tweeterSampleStream")
    .getOrCreate()
  spark.sparkContext.setLogLevel("INFO")
  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args : Array[String]) {
    val optionsParser = parser.parse(args,Paramaters()).get
    val configFilePath = optionsParser.configFilePath
    val publishFolderPath = optionsParser.publishFolderPath

    val sampleStreamsFolder = publishFolderPath + "sampleoftweets/"
    val sampleStreamsHashTageReportFolder = publishFolderPath + "hashtaganalysis/"

    val config = ConfigFactory.parseFile(new File(configFilePath))
    val twitterInterface = new TwitterApiConnecter(config)
    val schemaFilePath = config.getString("schemaFilePath")
    val recordLimit = config.getString("recordLimit").toLong
    import scala.collection.JavaConverters._
    val sampleStreamPartition = config.getStringList("sampleStreamPartition").asScala.toList
    val hashTagReportSampleStreamReportPartition = config.getStringList("hashTagReportPartition").asScala.toList


    val schemaSource = Source.fromFile(schemaFilePath).getLines.mkString
    val schemaFromJson = DataType.fromJson(schemaSource).asInstanceOf[StructType]

    logger.info(s"Reading streams from tweeter api , recordLimit : $recordLimit , schemaFilePath: $schemaSource")
    val jsonObjectsList = twitterInterface.getFilteredStream(recordLimit)
    println("jsonObjectsList : " + jsonObjectsList)
    import spark.implicits._

    val tweetsDf = spark.sparkContext.parallelize(jsonObjectsList).toDF()
    tweetsDf.show(false)
    logger.info("tweets df show")
    import org.apache.spark.sql.functions.{col,from_json}
    val dfJSON = tweetsDf.withColumn("jsonData",from_json(col("value"),schemaFromJson))
      .select("jsonData.*")
    dfJSON.printSchema()
    dfJSON.show(false)

    val tweetsDataDf = dfJSON.select("data.*","includes.*")
//    tweetsDataDf.show(false)
    println("tweets data")


    val entitiesDf = tweetsDataDf
      .select("author_id","created_at","id","text","lang","public_metrics","organic_metrics","places","users","entities.*")
      .withColumn("created_at_date",to_date(col("created_at")))
      .where(col("hashtags").isNotNull
      )
//    entitiesDf.printSchema()
    entitiesDf.show(false)
    logger.info("sample of tweets top 20")
    entitiesDf.write.format("parquet").mode("append").partitionBy(sampleStreamPartition:_*).save(sampleStreamsFolder)

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
    logger.info("showing 20 result of hashtag metrics dataframe ")
    hashTagsMetrics.write.format("parquet").partitionBy(hashTagReportSampleStreamReportPartition:_*).mode("append").save(sampleStreamsHashTageReportFolder)
  }

}

