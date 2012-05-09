package com.github.jrwest.scalamachine.core

import scalaz.{Functor, Monad}

sealed trait Res[+A]
// change to traits with apply, and equal/show instances?
case class ValueRes[+A](value: A) extends Res[A] {
  val isEmpty = false
}
case class ErrorRes(error: Any) extends Res[Nothing] {
  val isEmpty = true
}
case class HaltRes(code: Int) extends Res[Nothing] {
  val isEmpty = true
}
case object EmptyRes extends Res[Nothing] {
  val isEmpty = true
}

trait ResOps[A] {
  def res: Res[A]

  def map[B](f: A => B): Res[B] = {
    res match {
      case ValueRes(r) => ValueRes(f(r))
      case ErrorRes(e) => ErrorRes(e)
      case HaltRes(c)  => HaltRes(c)
      case EmptyRes => EmptyRes
    }
  }

  def flatMap[B](f: A => Res[B]): Res[B] = res match {
    case ValueRes(v) => f(v)
    case ErrorRes(e) => ErrorRes(e)
    case HaltRes(c) => HaltRes(c)
    case EmptyRes => EmptyRes
  }

  def getOrElse[B >: A](default: => B) = res match {
    case ValueRes(a) => a
    case _ => default
  }

  def |(default: => A) = getOrElse(default)

  def toOption: Option[A] = res match {
    case ValueRes(a) => Some(a)
    case _ => None
  }
}


object Res extends ResFunctions with ResInstances {

  implicit def resOps[T](r: Res[T]): ResOps[T] = new ResOps[T] {
    def res: Res[T] = r
  }

}

trait ResFunctions {
  def result[A](a: => A): Res[A] = ValueRes(a)
  def halt[A](code: => Int): Res[A] = HaltRes(code)
  def error[A](reason: => Any): Res[A] = ErrorRes(reason)
  def empty[A]: Res[A] = EmptyRes
}

trait ResInstances {
  import scalaz.{Monad, Traverse, Applicative}
  implicit val resScalazInstances = new Traverse[Res] with Monad[Res] {
    def point[A](a: => A): Res[A] = ValueRes(a)
    def traverseImpl[G[_],A,B](fa: Res[A])(f: A => G[B])(implicit G: Applicative[G]): G[Res[B]] =
      map(fa)(a => G.map(f(a))(ValueRes(_): Res[B])) match {
        case ValueRes(r) => r
        case HaltRes(c) => G.point(HaltRes(c))
        case ErrorRes(e) => G.point(ErrorRes(e))
        case _ => G.point(EmptyRes)
      }
    def bind[A, B](fa: Res[A])(f: A => Res[B]): Res[B] = fa flatMap f
  }
}

object ValueRes {
  import scalaz.Monad
  // Shouldn't be necessary but it is in order to assuage type inferencer in some cases
  // and sometimes useful when you only want to work withing ValueRes structure
  implicit val valueResScalazInstances = new Monad[ValueRes] {
    def point[A](a: => A): ValueRes[A] = ValueRes(a)
    def bind[A, B](fa: ValueRes[A])(f: A => ValueRes[B]): ValueRes[B] = f(fa.value)
  }
}

case class ResT[M[_],A](run: M[Res[A]]) {
  self =>
  def map[B](f: A => B)(implicit F: Functor[M]): ResT[M,B] = {
    ResT(F.map(self.run)((_: Res[A]) map f))
  }

  def flatMap[B](f: A => ResT[M,B])(implicit M: Monad[M]) = {
    ResT(M.bind(self.run) {
      case ValueRes(v) => f(v).run
      case r @ HaltRes(_) => M.point(r: Res[B])
      case r @ ErrorRes(_) => M.point(r: Res[B])
      case r @ EmptyRes => M.point(r: Res[B])
    })
  }
}

// TODO: ResT type class instances
object ResT extends ResTFunctions

trait ResTFunctions {
  import scalaz.~>
  def resT[M[_]] = new (({type λ[α] = M[Res[α]]})#λ ~> ({type λ[α] = ResT[M, α]})#λ) {
    def apply[A](a: M[Res[A]]) = new ResT[M, A](a)
  }
}

sealed trait AuthResult {
  def fold[T](success: T, failure: String => T): T = this match {
    case AuthSuccess => success
    case AuthFailure(s) => failure(s)
  }
}
case object AuthSuccess extends AuthResult
case class AuthFailure(headerValue: String) extends AuthResult
