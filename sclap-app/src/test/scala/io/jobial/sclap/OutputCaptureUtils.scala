package io.jobial.sclap

import cats.effect.IO
import io.jobial.sclap.core.implicits.TryExtensionInstance

import java.io.{BufferedOutputStream, ByteArrayOutputStream, FilterOutputStream, OutputStream, PrintStream}
import java.lang.reflect.{Field, Modifier}
import java.security.Permission
import scala.util.{DynamicVariable, Failure, Try}

object OutputCaptureUtils extends TryExtensionInstance {

  try {
    System.setSecurityManager(new SecurityManager {

      override def checkPermission(perm: Permission) = {
        // Allow other activities by default
      }
    })
  } catch {
    case t: Throwable =>
  }

  val originalSystemOut = System.out
  val originalSystemErr = System.err

  val testOut = new FilterOutputStream(originalSystemOut) {
    def setOut(o: OutputStream) = synchronized {
      out = o
    }
  }

  val testErr = new FilterOutputStream(originalSystemErr) {

    def setOut(o: OutputStream) = synchronized {
      out = o
    }
  }

  val consoleOut = new PrintStream(testOut)

  val consoleErr = new PrintStream(testErr)

  def writeField(target: AnyRef, fieldName: String, value: Any) = {
    import sun.misc.Unsafe
    import java.lang.reflect.Field
    val unsafeField = classOf[Unsafe].getDeclaredField("theUnsafe")
    unsafeField.setAccessible(true)
    
    val unsafe = unsafeField.get(null).asInstanceOf[Unsafe]
    val field = Try(target.getClass.getField(fieldName)).getOrElse {
      target.getClass.getDeclaredFields.find { f =>
        f.setAccessible(true)
        f.getName == fieldName
      }.get
    }
    Try {
      val modifiers = classOf[Field].getDeclaredField("modifiers");
      modifiers.setAccessible(true);
      modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    }
    
    Try {
      field.setAccessible(true)
      field.set(target, value)
      val staticFieldBase = unsafe.staticFieldBase(field)
      val staticFieldOffset = unsafe.staticFieldOffset(field)
      unsafe.putObject(staticFieldBase, staticFieldOffset, value)
    }
  }

  def redirectSystemOutAndErr = {
    System.out.flush
    System.err.flush
    System.setOut(consoleOut)
    System.setErr(consoleErr)
    redirectScalaOutAndErr
  }
  
  def redirectScalaOutAndErr = {
    // TODO: this only works for the current thread at the moment
    val setOutDirect = Console.getClass.getDeclaredMethod("setOutDirect", classOf[PrintStream])
    setOutDirect.setAccessible(true)
    setOutDirect.invoke(Console, consoleOut)
    val setErrDirect = Console.getClass.getDeclaredMethod("setErrDirect", classOf[PrintStream])
    setErrDirect.setAccessible(true)
    setErrDirect.invoke(Console, consoleErr)
  }

  def setSystemOutAndErr(out: OutputStream, err: OutputStream) = {
    testOut.setOut(new TeeOutputStream(out, originalSystemOut))
    testErr.setOut(new TeeOutputStream(err, originalSystemErr))
  }

  def resetSystemOutAndErr = {
    testOut.setOut(originalSystemOut)
    testErr.setOut(originalSystemErr)
  }

}

trait OutputCaptureUtils {

  import OutputCaptureUtils._

  redirectSystemOutAndErr

  /**
   * Capture output for a block of code. It is done on a best-effort basis and should be used only in tests.
   */
  def captureOutput[T](f: => T) = IO {
    OutputCaptureUtils.synchronized {
      redirectScalaOutAndErr
      val inIntellij = Thread.currentThread().getStackTrace.find(_.getClassName.contains("org.jetbrains")).isDefined
      if (inIntellij)
        Thread.sleep(200)
      // Override standard out & err
      val outBuffer = new ByteArrayOutputStream
      val errBuffer = new ByteArrayOutputStream

      setSystemOutAndErr(new PrintStream(outBuffer), new PrintStream(errBuffer))

      val r = Try(f).toEither

      System.out.flush
      System.err.flush

      val result = OutputCaptureResult(
        r,
        new String(outBuffer.toByteArray),
        new String(errBuffer.toByteArray)
      )

      // Restore global state
      resetSystemOutAndErr

      result
    }
  }
}

class TeeOutputStream(out: OutputStream, branch: OutputStream) extends BufferedOutputStream(out) {

  override def write(b: Array[Byte]): Unit = {
    super.write(b)
    this.branch.write(b)
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    super.write(b, off, len)
    this.branch.write(b, off, len)
  }

  override def write(b: Int): Unit = {
    super.write(b)
    this.branch.write(b)
  }

  override def flush(): Unit = {
    super.flush
    this.branch.flush
  }

  override def close(): Unit = {
    try super.close
    finally this.branch.close
  }
}

case class OutputCaptureResult[T](result: Either[Throwable, T], out: String, err: String) {

  lazy val outLines = out.split('\n').toList

  lazy val errLines = err.split('\n').toList
}