/*
 * Copyright (c) 2020 Jobial OÜ. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
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
