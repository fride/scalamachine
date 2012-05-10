package com.github.jrwest.scalamachine.core
package v3

import flow._
import scalaz.std.option._
import optionSyntax._
import scalaz.syntax.pointed._
import scalaz.State
import Decision.FlowState
import Res._
import ResT._
import ReqRespData._
import Metadata._
import Resource._


trait WebmachineDecisions {

  /* Service Available? */
  lazy val b13: Decision = Decision("v3b13", true, (r: Resource) => r.serviceAvailable(_: ReqRespData), b12, 503)

  /* Known Methods */
  lazy val b12: Decision = 
    Decision(
      "v3b12", 
      (r: Resource) => r.knownMethods(_: ReqRespData), 
      (l: List[HTTPMethod], d: ReqRespData) => l.contains(d.method), 
      b11, 
      501)

  /* URI Too Long? */
  lazy val b11: Decision = Decision("v3b11", true, (r: Resource) => r.uriTooLong(_: ReqRespData), 414, b10)

  /* Allowed Methods */
  lazy val b10: Decision =
    Decision(
      "v3b10",
      (r: Resource) => r.allowedMethods(_: ReqRespData),
      (l: List[HTTPMethod], d: ReqRespData) => l.contains(d.method),
      b9,
      (r: List[HTTPMethod]) => for {
          _ <- (statusCodeL := 405)
          _ <- (responseHeadersL += (("Allow" -> r.map(_.toString).mkString(", "))))
        } yield r
    )

  /* Malformed Request? */
  lazy val b9: Decision = Decision("v3b9", true, (r: Resource) => r.isMalformed(_: ReqRespData), 400, b8)

  /* Is Authorized? */
  lazy val b8: Decision = Decision(
    "v3b8",
    AuthSuccess,
    (r: Resource) => r.isAuthorized(_: ReqRespData),
    b7,
    (r: AuthResult) => for {
      _ <- r.fold(
        failure = (failMsg: String) => (responseHeadersL += ("WWW-Authenticate" -> failMsg)),
        success = responseHeadersL.st
      )
      _ <- (statusCodeL := 401)
    } yield r
  )

  /* Is Forbidden? */
  lazy val b7: Decision = Decision("v3b7", true, (r: Resource) => r.isForbidden(_: ReqRespData), 403, b6)

  /* Content-* Headers Are Valid? */
  lazy val b6: Decision = Decision("v3b6", true, (r: Resource) => r.contentHeadersValid(_: ReqRespData), b5, 501)

  /* Is Known Content-Type? */
  lazy val b5: Decision = Decision("v3b5", true, (r: Resource) => r.isKnownContentType(_: ReqRespData), b4, 415)

  /* Request Entity Too Large? */
  lazy val b4: Decision = Decision("v3b4", true, (r: Resource) => r.isValidEntityLength(_: ReqRespData), b3, 413)

  /* OPTIONS? */
  lazy val b3: Decision = new Decision {
    val name: String = "v3b3"
    val default = HaltRes(200)
    protected def decide(resource: Resource): State[ReqRespData, Res[Decision]] = {
      def handle(method: HTTPMethod): State[ReqRespData,Res[Decision]] = method match {
        case OPTIONS => {
          val set = for {
            hdrs <- resT[FlowState](State((d: ReqRespData) => resource.options(d)))
            _ <- resT[FlowState]((responseHeadersL ++= hdrs.toList).map(_.point[Res]))
            _ <- resT[FlowState](State((d: ReqRespData) => (halt[Decision](200), d)))
          } yield c3 // we will never get here
          set.run
        }
        case _ => c3.point[Res].point[FlowState]
      }
      methodL.st flatMap { handle(_) }
    }
  }

  /* Accept Exists? */
  lazy val c3: Decision = new Decision {
    import scalaz.syntax.std.allV.ToListVFromList

    // TODO: move somewhere more global it will probably be used elsewhere
    val defaultContentType = ContentType("text/plain")

    val name: String = "v3c3"

    protected def decide(resource: Resource): FlowState[Res[Decision]] = {
      performDecision(resource).map(_.point[Res])
    }

    private def performDecision(resource: Resource): State[ReqRespData,Decision] = for {
        mbAcceptHeader <- requestHeadersL member "accept"
        decision <- if (mbAcceptHeader.isDefined) State((c4, _: ReqRespData)) else resolveContentType(resource)
      } yield decision


    private def resolveContentType(r: Resource): State[ReqRespData,Decision] = for {
        res <- State[ReqRespData,Res[ContentTypesProvided]](s => r.contentTypesProvided(s))
        cType <- State((s: ReqRespData) => (firstOrDefault(res),s))
        _ <- (metadataL <=< contentTypeL) := Option(cType)
      } yield d4 

    private def firstOrDefault(res: Res[ContentTypesProvided]): ContentType =  
      (res map { (_: ContentTypesProvided).toNel.map(_.head._1) getOrElse defaultContentType }) | defaultContentType            
  }

  /* Acceptable Media Type Available? */
  lazy val c4: Decision = new Decision {
    val name: String = "v3c4"

    protected def decide(resource: Resource): FlowState[Res[Decision]] = {
      for {
        acceptHeader <- ((requestHeadersL member "accept").st map { _ getOrElse "*/*" })
        providedResult <- State((d: ReqRespData) => resource.contentTypesProvided(d))
        provided <- (providedResult getOrElse Nil).unzip._1.point[FlowState]
        contentType <- Util.chooseMediaType(provided, acceptHeader).point[FlowState]
        _ <- (metadataL <=< contentTypeL) := contentType
      } yield contentType >| d4.point[Res] | HaltRes(406)
    }
  }

  lazy val d4: Decision = new Decision {
    val name = "v3d4" 

    protected def decide(resource: Resource): FlowState[Res[Decision]] = {
      (requestHeadersL member "accept-language").st.map(_ >| d5.point[Res] | e5.point[Res])
    }
  }

  lazy val d5: Decision = Decision("v3d5", true, (r: Resource) => r.isLanguageAvailable(_: ReqRespData), e5, 406)

  lazy val e5: Decision = new Decision {
    def name: String = "v3e5"


    protected def decide(resource: Resource): FlowState[Res[Decision]] = {
      def choose(mbProvided: Resource.CharsetsProvided): Res[(Decision, Option[String])] =
        mbProvided.map { provided =>
          Util.chooseCharset(provided.unzip._1, "*")
            .map(c => result((f6, some(c))))
            .getOrElse(halt(406))
        } getOrElse { result((f6, none)) }

      val missingAccept: ResT[FlowState, (Decision,Option[String])] = for {
        mbProvided <- resT[FlowState](State((d: ReqRespData) => resource.charsetsProvided(d)))
        r <- resT[FlowState](choose(mbProvided).point[FlowState])
      } yield r

      val act = for {
        mbHeader <- resT[FlowState]((requestHeadersL member "accept-charset").st map { _.point[Res] })
        (decision, chosen) <- mbHeader >| resT[FlowState](result((e6, none[String])).point[FlowState]) | missingAccept
        _ <- resT[FlowState](((metadataL <=< chosenCharsetL) := chosen).map(_.point[Res]))
      } yield decision
      act.run
    }

  }

  lazy val e6: Decision = new Decision {
    def name: String = "v3e6"

    protected def decide(resource: Resource): FlowState[Res[Decision]] = null
  }

  lazy val f6: Decision = new Decision {
    def name: String = "v3f6"

    protected def decide(resource: Resource): FlowState[Res[Decision]] = null
  }
}
