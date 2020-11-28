package io.jobial.sclap

import com.typesafe.scalalogging.LazyLogging
import io.jobial.sclap.core.implicits.{ArgumentValueParserInstances, ArgumentValuePrinterInstances}
import io.jobial.sclap.core.{CommandLineParserSyntax, Logging}
import io.jobial.sclap.impl.picocli.PicocliCommandLineParser


trait CommandLineParser
  extends CommandLineParserSyntax
    with ArgumentValueParserInstances
    with ArgumentValuePrinterInstances
    with PicocliCommandLineParser
    with Logging
    with LazyLogging
