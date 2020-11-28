package io.jobial.sclap.core

import com.typesafe.scalalogging.Logger

/**
 * Isolate from Scala Logging and make wiring more flexible.  
 */
trait Logging {
  protected def logger: Logger
}
