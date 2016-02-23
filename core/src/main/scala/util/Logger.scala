package silt
package util

import ch.qos.logback.classic._
import ch.qos.logback.classic.spi._
import ch.qos.logback.classic.pattern._

// Note: Adopted from `play.api.Logger$ColoredLevel.`

/** Logback converter generating colored, lower-case level names.
  *
  * Used for example as:
  * {{{
  * %coloredLevel %logger{15} - %message%n%xException{5}
  * }}}
  */
class ColoredLoggerLevel extends ClassicConverter {

  override def convert(event: ILoggingEvent): String = event.getLevel match {
    case Level.TRACE => "[" + Colors.blue("trace") + "]"
    case Level.DEBUG => "[" + Colors.cyan("debug") + "]"
    case Level.INFO => "[" + Colors.white("info") + " ]"
    case Level.WARN => "[" + Colors.yellow("warn") + " ]"
    case Level.ERROR => "[" + Colors.red("error") + "]"
  }

}

private[util] object Colors {

  import scala.Console._

  lazy val isANSISupported =
    Option(System.getProperty("sbt.log.noformat")).map(_ != "true").orElse {
      Option(System.getProperty("os.name"))
        .map(_.toLowerCase)
        .filter(_.contains("windows"))
        .map(_ => false)
    }.getOrElse(true)

  def red(str: String): String = if (isANSISupported) (RED + str + RESET) else str
  def blue(str: String): String = if (isANSISupported) (BLUE + str + RESET) else str
  def cyan(str: String): String = if (isANSISupported) (CYAN + str + RESET) else str
  def green(str: String): String = if (isANSISupported) (GREEN + str + RESET) else str
  def magenta(str: String): String = if (isANSISupported) (MAGENTA + str + RESET) else str
  def white(str: String): String = if (isANSISupported) (WHITE + str + RESET) else str
  def black(str: String): String = if (isANSISupported) (BLACK + str + RESET) else str
  def yellow(str: String): String = if (isANSISupported) (YELLOW + str + RESET) else str

}

