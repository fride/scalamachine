package com.github.jrwest.scalamachine.lift

import net.liftweb.http.{InMemoryResponse, Req, LiftResponse}
import net.liftweb.common.{Full, Box}
import com.github.jrwest.scalamachine.core._
import dispatch.DispatchTable
import v3.V3DispatchTable


trait LiftWebmachine {
  this: DispatchTable[Req, Box[LiftResponse], Function0] =>

  def path(req: Req): List[String] = req.path.partPath

  def wrap(res: => Box[LiftResponse]): () => Box[LiftResponse] = () => res  

  def toData(req: Req) = ReqRespData(method = parseMethod(req.request.method), pathParts = path(req)) // TODO: complete conversion once ReqRespData is filled out

  def fromData(data: ReqRespData): Box[LiftResponse] = Full(InMemoryResponse(
    data = Array(), // FIXME
    headers = Nil, // FIXME
    cookies = Nil,
    code = data.statusCode
  ))

  private def parseMethod(methodStr: String): HTTPMethod = HTTPMethod.fromString(methodStr)

}

trait LiftWebmachineV3 extends V3DispatchTable[Req,Box[LiftResponse],Function0] with LiftWebmachine