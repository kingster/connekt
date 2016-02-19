package com.flipkart.connekt.receptors.directives

import java.io.File

import akka.Done
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, Multipart}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import com.flipkart.connekt.commons.utils.StringUtils._

import MultiPartFormData._

/**
 * Created by nidhi.mehla on 18/02/16.
 * Based upon https://github.com/clockfly/akka-http-file-server
 */

trait FileDirective {

  private def uploadFileImpl(implicit mat: Materializer, ec: ExecutionContext): Directive1[Future[Map[Name, Either[Value, FileInfo]]]] = {
    Directive[Tuple1[Future[Map[Name, Either[Value, FileInfo]]]]] { inner =>
      entity(as[Multipart.FormData]) { formData: Multipart.FormData =>
        val fileNameMap = formData.parts.mapAsync(1) { part =>
          if (part.filename.isDefined) {
            val targetPath = File.createTempFile(s"upload_${part.name}_", part.filename.getOrElse(""))
            val written = part.entity.dataBytes.runWith(FileIO.toFile(targetPath))
            written.map(written =>
              Map(part.name -> Right(FileInfo(part.filename.get, targetPath.getAbsolutePath, written.status))))
          } else {
            //simple value
            Future(Map(part.name -> Left(part.entity.getString)))
          }
        }.runFold(Map.empty[Name, Either[Value, FileInfo]])((set, value) => set ++ value)
        inner(Tuple1(fileNameMap))
      }
    }
  }

  /**
   * Map[Key -> Either[Value/FileInfo] ]
   * @return
   */
  def extractFormData: Directive1[Map[Name,  Either[Value, FileInfo]]] = {
    Directive[Tuple1[Map[Name,  Either[Value, FileInfo]]]] { inner =>
      extractMaterializer { implicit mat =>
        extractExecutionContext { implicit ec =>
          uploadFileImpl(mat, ec) { filesFuture =>
            ctx => {
              filesFuture.map(map => inner(Tuple1(map))).flatMap(route => route(ctx))
            }
          }
        }
      }
    }
  }

  def downloadFile(file: String): Route = {
    val f = new File(file)
    val responseEntity = HttpEntity(
      MediaTypes.`application/octet-stream`,
      f.length,
      FileIO.fromFile(f, chunkSize = 262144))
    complete(responseEntity)
  }
}


object MultiPartFormData {

  //form field name
  type Name = String
  type Value = String

  case class FileInfo(fileName: String, tmpFilePath: String, status: Try[Done])
}