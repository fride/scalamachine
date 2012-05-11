package com.github.jrwest.scalamachine.core
package flow

import scalaz.State


// methods are split up to allow stacking at start/end of flow as well as start/end of each decision
// run decision is pretty much extraneous outside of that
trait FlowRunnerBase {

  // stacking this method gives access to start/end of flow
  def run(decision: Decision, resource: Resource, data: ReqRespData): ReqRespData

  protected def runDecision(decision: Decision, resource: Resource, data: ReqRespData): (Option[Decision], ReqRespData)
}



class FlowRunner extends FlowRunnerBase {

  def run(decision: Decision, resource: Resource, data: ReqRespData): ReqRespData = {
    // loop is here so stacked traits can tie into run using it as a entry and exit point for
    // the entire flow
    def loop(decision: Decision, resource: Resource, data: ReqRespData): ReqRespData = {
      runDecision(decision, resource, data) match {
        case (Some(next), newData) => run(next, resource, newData)
        case (None, newData) => newData
      }
    }
    loop(decision, resource, data)
  }

  protected def runDecision(decision: Decision, resource: Resource, data: ReqRespData): (Option[Decision], ReqRespData) =
    decision(resource)(data)
}