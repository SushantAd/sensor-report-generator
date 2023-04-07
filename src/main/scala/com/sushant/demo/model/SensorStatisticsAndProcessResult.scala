package com.sushant.demo.model

case class SensorStatisticsAndProcessResult(
                                             sensorStatisticsResult: Seq[SensorStatisticsResult],
                                             processingCount: ProcessingCount
                                           )