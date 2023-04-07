package com.sushant.demo

import cats.effect.unsafe.implicits.global
import com.sushant.demo.model.{ProcessingCount, SensorData, SensorStatistics, SensorStatisticsAndProcessResult, SensorStatisticsResult}
import fs2.io.file.Path
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.ByteArrayOutputStream
import scala.collection.immutable.HashMap

class ApplicationIntegrationTest extends AnyWordSpec with Matchers with MockitoSugar{

  /**Please change directory path before testing*/
  private val dirPath = "src/test/resources/"

  "Application" should {

    "read process and print result" in{
      val path = Path(s"${dirPath}/test")
      val out = new ByteArrayOutputStream()
      Console.withOut(out){
        Application.sensorStatisticsProcessor(path).unsafeRunSync()
      }
      out.toString should include ("Num of processed files: 2")
      out.toString should include ("Num of processed measurements: 14")
      out.toString should include ("Num of failed measurements: 4")
      out.toString should include ("Sensors with highest avg humidity:")
      out.toString should include ("sensor-id,min,avg,max")
      out.toString should include ("s2, 78, 82, 88")
      out.toString should include ("s1, 10, 54, 98")
      out.toString should include ("s3, NaN, NaN, NaN")
    }

    "read files from a directory" in{
      val path = Path(dirPath)
      val files = Application.directory(path).compile.toList.unsafeRunSync()
      files.size should be >= 1
      }

    "read and parse a csv file" in{
      val path = Path(s"${dirPath}/test.csv")
      val parseResult = Application.parser(path).compile.toList.unsafeRunSync()
      parseResult.size shouldEqual 7
    }

    "should process and return map" in{
      val path = Path(s"${dirPath}/test.csv")
      val parseResult: Seq[SensorData] = Application.parser(path).compile.toList.unsafeRunSync()
      val map = Application.processContent(parseResult, HashMap.empty[String, SensorStatistics])
      map.keys.size should be >=1
    }

    "should evaluate sensor statistics for new valid data" in{
      val sensorData1 = SensorData("s1", "88")
      val sensorStatisticsNew = SensorStatistics(None, None, None)
      val sensorStatistics1 = SensorStatistics(Some(88), Some(88), Some(88), 1, 0)
      val result = Application.evaluateSensorStatistics(sensorData1, sensorStatisticsNew, true)
      result shouldEqual sensorStatistics1
    }

    "should evaluate sensor statistics as failed process for new invalid data" in{
      val sensorData1 = SensorData("s1", "NaN")
      val sensorStatisticsNew = SensorStatistics(None, None, None)
      val sensorStatistics1 = SensorStatistics(None, None, None, 1, 1)
      val result = Application.evaluateSensorStatistics(sensorData1, sensorStatisticsNew, true)
      result shouldEqual sensorStatistics1
    }

    "should evaluate sensor statistics for old valid data" in{
      val sensorData1 = SensorData("s1", "88")
      val sensorStatisticsOld = SensorStatistics(Some(88), Some(88), Some(88), 1)
      val sensorStatistics1 = SensorStatistics(Some(88), Some(176), Some(88), 2, 0)
      val result = Application.evaluateSensorStatistics(sensorData1, sensorStatisticsOld)
      result shouldEqual sensorStatistics1
    }

    "should evaluate sensor statistics as failed process for old invalid data" in{
      val sensorData1 = SensorData("s1", "NaN")
      val sensorStatisticsNew = SensorStatistics(None, None, None, 0, 1)
      val sensorStatistics1 = SensorStatistics(None, None, None, 1, 2)
      val result = Application.evaluateSensorStatistics(sensorData1, sensorStatisticsNew)
      result shouldEqual sensorStatistics1
    }

    "should find Highest Average Humidity" in{
      val path = Path(s"${dirPath}/test.csv")
      val sensorStatisticsAndProcessResult = SensorStatisticsAndProcessResult(Seq(
        SensorStatisticsResult("s2", Some(78), Some(82), Some(88)),
          SensorStatisticsResult("s1", Some(10), Some(54), Some(98)),
          SensorStatisticsResult("s3", None, None, None)), ProcessingCount(0, 7, 2))
      val parseResult: Seq[SensorData] = Application.parser(path).compile.toList.unsafeRunSync()
      val map = Application.processContent(parseResult, HashMap.empty[String, SensorStatistics])
      val result = Application.findHighestAverageHumidity(map).unsafeRunSync()
      result shouldEqual sensorStatisticsAndProcessResult
    }
  }

}
