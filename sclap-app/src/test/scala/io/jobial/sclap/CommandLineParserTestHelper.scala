package io.jobial.sclap

import org.scalatest.flatspec.AsyncFlatSpec

/**
 * Test helper. You can use it direclty in your apps to table based tests on your command line specs. 
 * If you do not want the default implicits, you can extend the CommandLineParserTestHelperNoImplicits instead.
 */
trait CommandLineParserTestHelper extends CommandLineParserTestHelperNoImplicits with CommandLineParser {
  this: AsyncFlatSpec =>
}