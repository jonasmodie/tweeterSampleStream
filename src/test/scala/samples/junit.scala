package samples

import org.junit._
import Assert._
import com.typesafe.config.ConfigFactory
import org.simpleetl.TwitterApiConnecter

import java.io.File

@Test
class AppTest {

    val configFilePath = "/home/jonas/Academics/twitterAnalyser/config/application.conf"
    val recordLimit = 2

    @Test
    def testTweeterApiConnect_getStream() ={

        val config = ConfigFactory.parseFile(new File(configFilePath))
        val twitterInterface = new TwitterApiConnecter(config)
        val jsonObjectsList = twitterInterface.getFilteredStream(recordLimit)
        val functionReturnedValues = jsonObjectsList.length == recordLimit
        assertTrue(functionReturnedValues)
    }

//    @Test
//    def testKO() = assertTrue(false)

}


