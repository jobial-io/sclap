package io.jobial.sclap

import cats.instances.AllInstances
import cats.syntax.AllSyntax
import io.jobial.sclap.core.implicits.{ArgumentValueParserInstances, ArgumentValuePrinterInstances, CommandLineParserImplicits}
import io.jobial.sclap.impl.picocli.implicits.PicocliCommandLineParserImplicits

trait CommandLineParser
  extends CommandLineParserNoImplicits
    with CommandLineParserImplicits
    with ArgumentValueParserInstances
    with ArgumentValuePrinterInstances
    with PicocliCommandLineParserImplicits
    with AllSyntax
    with AllInstances
