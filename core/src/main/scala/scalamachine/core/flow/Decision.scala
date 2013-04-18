package scalamachine.core
package flow

import scalaz.{Id, IndexedStateT, State}
import scalaz.syntax.monad._

trait Decision {
  import Decision.FlowState
  import ReqRespData.{statusCodeL, respBodyL}

  def name: String

  def apply(resource: Resource): FlowState[Option[Decision]] = {
    for {
      res <- decide(resource)
      _ <- res match {
        case HaltRes(code, body) => setError(code, body)
        case ErrorRes(error) => setError(500, Option(error))
        case _ => ().point[FlowState]
      }
    } yield res.toOption
  }

  protected def decide(resource: Resource): FlowState[Res[Decision]]

  private def setError(code: Int, errorBody: Option[HTTPBody]): IndexedStateT[Id.Id, ReqRespData, ReqRespData, Unit] = {
    for {
      _ <- statusCodeL := code
      body <- respBodyL.st
      _ <- {
        if (body.isEmpty) {
          val respBodyState: IndexedStateT[Id.Id, ReqRespData, ReqRespData, HTTPBody] = errorBody.map { errBody =>
            respBodyL := errBody
          } getOrElse {
            respBodyL.st
          }
          respBodyState
        } else {
          respBodyL.st: IndexedStateT[Id.Id, ReqRespData, ReqRespData, HTTPBody]
        }
      }
    } yield ()
  }

  override def equals(o: Any): Boolean = o match {
    case o: Decision => o.name == name
    case _ => false
  }

  override def toString = "Decision(%s)" format name

}

object Decision {
  import ReqRespData.statusCodeL
  import Res._
  import ResTransformer._

  type FlowState[+T] = State[ReqRespData, T]
  type ResourceF[T] = Resource => ReqRespData => (ReqRespData, Res[T])
  type CheckF[T] = (T, ReqRespData) => Boolean
  type HandlerF[T] = T => FlowState[T]
  type Handler[T] = Either[HandlerF[T],Decision]

  // TODO: this is a possible type of helper method that can be moved to ReqRespData object
  private[this] def setStatus[T](code: Int): HandlerF[T] = v => for { _ <- statusCodeL := code } yield v

  def apply[T](decisionName: String, test: ResourceF[T], check: CheckF[T], onSuccess: Handler[T], onFailure: Handler[T]) = new Decision {
    def name: String = decisionName

    protected def decide(resource: Resource): FlowState[Res[Decision]] = {
      val nextT = for {
        value <- resT[FlowState](State((d: ReqRespData) => test(resource)(d)))
        handler <- resT[FlowState](State((d: ReqRespData) => if (check(value, d)) (d, result(onSuccess)) else (d, result(onFailure))))
        next <- resT[FlowState](handler match {
          case Left(f) => f(value) >| empty[Decision]
          case Right(decision) => result(decision).point[FlowState]
        })
      } yield next

      nextT.run
    }
  }

  def apply[T](decisionName: String,
               expected: T,
               test: ResourceF[T],
               onSuccess: Decision,
               onFailure: Int): Decision = apply(decisionName, expected, test, Right(onSuccess), Left(setStatus[T](onFailure)))

  def apply[T](decisionName: String,
               expected: T,
               test: ResourceF[T],
               onSuccess: Int,
               onFailure: Decision): Decision = apply(decisionName, expected, test, Left(setStatus[T](onSuccess)), Right(onFailure))

  def apply[T](decisionName: String,
               expected: T,
               test: ResourceF[T],
               onSuccess: Decision,
               onFailure: Decision): Decision = apply(decisionName, expected, test, Right(onSuccess), Right(onFailure))

  def apply[T](decisionName: String,
               expected: T,
               test: ResourceF[T],
               onSuccess: Decision,
               onFailure: HandlerF[T]): Decision = apply(decisionName, expected, test, Right(onSuccess), Left(onFailure))

  def apply[T](decisionName: String,
               expected: T,
               test: ResourceF[T],
               onSuccess: Handler[T],
               onFailure: Handler[T]): Decision = apply(decisionName, test, (res: T, _: ReqRespData) => res == expected, onSuccess, onFailure)


  def apply[T](decisionName: String,
               test: ResourceF[T],
               check: CheckF[T],
               onSuccess: Decision,
               onFailure: Int): Decision = apply(decisionName, test, check, onSuccess, setStatus[T](onFailure))

  def apply[T](decisionName: String,
               test: ResourceF[T],
               check: CheckF[T],
               onSuccess: Decision,
               onFailure: Decision): Decision = apply(decisionName, test, check, Right(onSuccess), Right(onFailure))

  def apply[T](decisionName: String,
               test: ResourceF[T],
               check: CheckF[T],
               onSuccess: Decision,
               onFailure: HandlerF[T]): Decision = apply(decisionName, test, check, Right(onSuccess), Left(onFailure))

}
