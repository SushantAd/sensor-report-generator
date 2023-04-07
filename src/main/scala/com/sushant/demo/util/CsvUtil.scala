package com.sushant.demo.util

import fs2.{Pipe, text}

object CsvUtil {
  def csvParser[F[_]]: Pipe[F, Byte, List[String]] =
    _.through(text.utf8.decode)
      .through(text.lines)
      .drop(1)
      .map(_.split(',').toList)
}
