package com.sushant.demo.model

case class ProcessingCount(
                            processedFiles: Int = 0,
                            processedMeasurements: Int = 0,
                            failedMeasurements: Int = 0,
                          )
