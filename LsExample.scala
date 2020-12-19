import io.jobial.sclap.CommandLineApp

object LsExample extends CommandLineApp {

  def run =
    for {
      long <- opt("l", false).description("long format")
      dirname <- param[String].paramLabel("dir name")
    } yield
      myLs(long, dirname)

}