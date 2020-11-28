package io.jobial.sclap.impl.picocli.implicits

import io.jobial.sclap.core.{ArgumentValueParser, Opt, OptWithDefaultValue, OptWithRequiredValue}
import io.jobial.sclap.impl.picocli.{PicocliOpt, PicocliOptWithDefaultValue, PicocliOptWithRequiredValue}
import picocli.CommandLine.Model.OptionSpec

trait PicocliCommandLineParserImplicits {
  
  implicit class PicocliOptExtension[T](opt: Opt[T]) {
    def withPicocliOptionSpecBuilder(builder: OptionSpec.Builder => OptionSpec.Builder)
      (implicit valueParser: ArgumentValueParser[T], optionValueParser: ArgumentValueParser[Option[T]]) = {
      PicocliOpt(opt, builder)
    }
  }

  implicit class PicocliOptWithDefaultValueExtension[T: ArgumentValueParser](opt: OptWithDefaultValue[T]) {
    def withPicocliOptionSpecBuilder(builder: OptionSpec.Builder => OptionSpec.Builder) = {
      PicocliOptWithDefaultValue(opt, builder)
    }
  }

  implicit class PicocliOptWithRequiredValueBuilderExtension[T: ArgumentValueParser](opt: OptWithRequiredValue[T]) {
    def withPicocliOptionSpecBuilder(builder: OptionSpec.Builder => OptionSpec.Builder) = {
      PicocliOptWithRequiredValue(opt, builder)
    }
  }
}
