package callback.stages.httpStage

/**
  * Created by harshit.sinha on 03/06/16.
  */
case class HttpCallbackTracker(payload:String,
                               error: Int,
                               discarded: Boolean,
                               serverPath: String)
