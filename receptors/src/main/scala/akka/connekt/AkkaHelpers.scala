package akka.connekt

import akka.stream.ActorMaterializer

object AkkaHelpers {

  implicit class ActorMaterializerFunctions(val mat: ActorMaterializer) {
    def getSystem = mat.system

  }

}
