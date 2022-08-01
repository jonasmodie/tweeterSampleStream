package org.simpleetl

import java.net.{HttpURLConnection, InetSocketAddress, URL}
import com.sun.net.httpserver.{HttpExchange, HttpServer}
import com.typesafe.config.Config
import org.apache.commons.io.IOUtils
import org.json.JSONObject

import java.io.{BufferedReader, InputStreamReader}
import scala.io.Source
//import org.http4s.client.dsl.io._
//import org.http4s.headers._
//import org.http4s.{AuthScheme, Credentials, MediaType}

class TwitterApiConnecter(config:Config) {

  def getFilteredStream(recordLimit:Long): Array[String] ={

    val urlString = config.getString("urlString")
    val APP_ACCESS_TOKEN = config.getString("APP_ACCESS_TOKEN")
    val url = new URL(urlString)

//    val stream = requests.get.stream(urlString,Authorization(Credentials.Token(AuthScheme.Bearer, APP_ACCESS_TOKEN)), Accept(MediaType.application.json))

    val conn = url.openConnection.asInstanceOf[HttpURLConnection]
    conn.setRequestProperty("Accept", "application/json")
    conn.setRequestProperty("Authorization", "Bearer " + APP_ACCESS_TOKEN)
    conn.setRequestMethod("GET")
    conn.connect()

    val status = conn.getResponseCode()
    println("response status : " + status)

    val in:BufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    var output:String = "";
    var dataArray: Array[String] = Array[String]()
    var count = 0;

    while ((output = in.readLine()) != null && count < recordLimit ) {
      dataArray =Array.concat(dataArray,Array(output))
      count = count + 1
    }
    in.close();
    dataArray
  }

}
