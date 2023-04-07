package com.sushant.demo

import cats.effect._
import cats.implicits._
import fs2._
import fs2.io.file.{Files, Flags, Path}
import com.sushant.demo.model._
import com.sushant.demo.util.CsvUtil.csvParser

import scala.annotation.tailrec
import scala.collection.immutable.HashMap
import scala.util.control.Exception.allCatch

object Application extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    println("Please Enter Directory Path:")
    val input = scala.io.StdIn.readLine()
    val pathDir: Path = Path(input)
    sensorStatisticsProcessor(pathDir).as(ExitCode.Success)
  }

  def directory(directoryPath: Path): Stream[IO, Path] = Files[IO].list(directoryPath)

  def parser(path: Path): Stream[IO, SensorData] =
    Files[IO].readAll(path, 2048, Flags(Flags.Read.value))
      .through(csvParser)
      .map(SensorData.parseSensorData)
      .unNoneTerminate

  def sensorStatisticsProcessor(path: Path): IO[Unit] = {
    for{
      files <- directory(path).compile.toList
      parsedFiles <- IO.apply(files.map(path => parser(path).compile.toList))
      parsedResult <- parsedFiles.sequence
      allCsvContent = parsedResult.flatten
      sensorStatisticsMap <- IO(processContent(allCsvContent, HashMap.empty[String, SensorStatistics]))
      highestHumidity <- findHighestAverageHumidity(sensorStatisticsMap)
      measurementResult = highestHumidity.processingCount.copy(processedFiles = files.size)
      _ <- IO(generateResponse(measurementResult, highestHumidity.sensorStatisticsResult))
    } yield()
  }

  @tailrec
  def processContent(sensorDataSet : Seq[SensorData], map: HashMap[String,SensorStatistics]): HashMap[String,SensorStatistics] = {
    sensorDataSet match {
      case Nil => map
      case currentSensorData :: sensorData =>
        val currentMap: HashMap[String,SensorStatistics] =  map.get(currentSensorData.id) match {
          case Some(stats) => (map + (currentSensorData.id -> evaluateSensorStatistics(currentSensorData, stats)))
          case _ => (map + (currentSensorData.id -> evaluateSensorStatistics(currentSensorData, SensorStatistics(None, None, None), isNew = true)))
        }
        processContent(sensorData, currentMap)
    }
  }

  def evaluateSensorStatistics(sensorData: SensorData, sensorStatistics: SensorStatistics, isNew: Boolean = false): SensorStatistics = {
    allCatch.opt(sensorData.humidity.toInt) match{
      case Some(i) if isNew => sensorStatistics.copy(min = Some(i), avg = Some(i), max = Some(i), totalCount = sensorStatistics.totalCount + 1)
      case Some(i) if i >= sensorStatistics.max.getOrElse(0) => sensorStatistics.copy(avg = sensorStatistics.avg.map(r => r + i), max = Some(i), totalCount = sensorStatistics.totalCount + 1)
      case Some(i) if i <= sensorStatistics.min.getOrElse(0) => sensorStatistics.copy(min = Some(i), avg = sensorStatistics.avg.map(r => r + i), totalCount = sensorStatistics.totalCount + 1)
      case Some(i)  => sensorStatistics.copy(avg = sensorStatistics.avg.map(r => r + i), totalCount = sensorStatistics.totalCount + 1)
      case _  => sensorStatistics.copy(totalCount = sensorStatistics.totalCount + 1, totalFailed = sensorStatistics.totalFailed + 1)
    }
  }

  def findHighestAverageHumidity(map: HashMap[String, SensorStatistics]): IO[SensorStatisticsAndProcessResult] = IO{
    val result = map.foldLeft((Seq.empty[SensorStatisticsResult], ProcessingCount()))((accumulator, elem) =>{
      val data = accumulator._1 ++ Seq(SensorStatisticsResult.apply(elem._1, elem._2))
      val processingCount = accumulator._2.copy(processedMeasurements = accumulator._2.processedMeasurements + elem._2.totalCount,
        failedMeasurements = accumulator._2.failedMeasurements + elem._2.totalFailed)
      (data,  processingCount)
    })
    model.SensorStatisticsAndProcessResult(result._1.sortWith(_.avg > _.avg), result._2)
  }

  private def generateResponse(measurementResult: ProcessingCount, statisticsResult: Seq[SensorStatisticsResult] ): Unit ={
    println(s"Num of processed files: ${measurementResult.processedFiles}")
    println(s"Num of processed measurements: ${measurementResult.processedMeasurements}")
    println(s"Num of failed measurements: ${measurementResult.failedMeasurements}")
    println("\nSensors with highest avg humidity:\n")
    println("sensor-id,min,avg,max")
    statisticsResult.foreach( res => println(res))
  }
}