package com.sushant.demo.model

case class SensorStatistics(min: Option[Int], avg: Option[Int], max: Option[Int], totalCount: Int = 0, totalFailed: Int = 0)

case class SensorStatisticsResult(id: String, min: Option[Int], avg: Option[Int], max: Option[Int]){
  override def toString: String = s"${id}, ${min.getOrElse("NaN")}, ${avg.getOrElse("NaN")}, ${max.getOrElse("NaN")}"
}

object SensorStatisticsResult{
  def apply(id: String,  sensorStatistics: SensorStatistics): SensorStatisticsResult = {
    SensorStatisticsResult(id, sensorStatistics.min,
      sensorStatistics.avg.map(avgVal => Math.round(avgVal/(sensorStatistics.totalCount - sensorStatistics.totalFailed))),
      sensorStatistics.max)
  }
}