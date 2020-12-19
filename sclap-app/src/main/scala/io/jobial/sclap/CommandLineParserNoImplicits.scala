package io.jobial.sclap

import com.typesafe.scalalogging.LazyLogging
import io.jobial.sclap.core.{CommandLineParserDsl, Logging}
import io.jobial.sclap.impl.picocli.PicocliCommandLineParser

trait CommandLineParserNoImplicits
  extends CommandLineParserDsl
    with PicocliCommandLineParser
    with Logging
    with LazyLogging
