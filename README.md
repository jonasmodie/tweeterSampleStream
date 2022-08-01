# tweeter Sample Stream application

The application streams sample of tweets from twitter v2 api

To run the application you will need to create config(.conf) file and pass it as paramater 

# below are the key value pair requeird in application.conf
     \
     recordLimit = 10 ## this is to limit the number of stream record being pulled from tweeter api
     schemaFilePath = "/home/jonas/Academics/twitterAnalyser/schema/schema.json"
     urlString = "https://api.twitter.com/2/tweets/sample/stream?tweet.fields=created_at,entities,geo,id,in_reply_to_user_id,lang,public_metrics,organic_metrics&expansions=author_id,geo.place_id&user.fields=created_at&place.fields=contained_within,country,country_code,full_name,geo,id,name,place_type"
     APP_ACCESS_TOKEN = "xxxxxxxx"
     hashTagReportPartition=["lang"]
     sampleStreamPartition=["lang","created_at_date"] \ 

# Development
To run the application locally you will need to setup your environent with the following versions

scala 2.12.15
spark 3.3.0

# Deployment
 Change Object App SparkSession builder() code from master(Local[*]) to master("yarn")
 run mvn clean compile package

# Spark submit example
    #pass the path to the confug file you created above
    # spark-submit --class org.simpleetl.App twitterAnalyser-1.0-SNAPSHOT.jar --configFilePath /path/to/config/application.conf --publishFolderPath /path/to/storage/

# local environment examaple
    # spark-submit --class org.simpleetl.App twitterAnalyser-1.0-SNAPSHOT.jar --configFilePath /home/jonas/Academics/twitterAnalyser/config/application.conf --publishFolderPath /home/jonas/Academics/twitterAnalyser/storage/
