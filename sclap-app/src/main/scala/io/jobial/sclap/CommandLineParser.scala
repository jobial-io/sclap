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
