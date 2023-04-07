package com.sushant.demo.model

import scala.util.Try

case class SensorData(
                       id: String,
                       humidity: String
                     )

object SensorData {
  val parseSensorData: List[String] => Option[SensorData] = {
    case (id :: humidity :: Nil) =>
      Try(
        SensorData(
          id = id,
          humidity = humidity,
        )
      ).toOption
    case _ => None
  }
}

