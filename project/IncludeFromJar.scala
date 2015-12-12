import sbtassembly._
import java.io.File
import sbtassembly.AssemblyPlugin.autoImport._

class IncludeFromJar(val jarName: String) extends MergeStrategy {

  val name = "includeFromJar"

  def apply(tmp : java.io.File, path : scala.Predef.String, files : scala.Seq[java.io.File]): Either[String, Seq[(File, String)]] = {
    val includedFiles = files.flatMap { f =>
      val (source, _, _, isFromJar) = sbtassembly.AssemblyUtils.sourceOfFileForMerge(tmp, f)
      if(isFromJar && source.getName == jarName) Some(f -> path) else None
    }
    Right(includedFiles)
  }

}