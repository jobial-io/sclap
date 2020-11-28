package io.jobial.sclap

import io.jobial.sclap.core.implicits.CommandLineParserImplicits
import io.jobial.sclap.impl.picocli.implicits.PicocliCommandLineParserImplicits

package object implicits extends CommandLineParserImplicits with PicocliCommandLineParserImplicits 