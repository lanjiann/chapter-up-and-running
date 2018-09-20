package com.goticks

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class RestApi(system: ActorSystem, timeout: Timeout)
    extends RestRoutes {
  implicit val requestTimeout: Timeout = timeout
  implicit def executionContext: ExecutionContextExecutor = system.dispatcher

  def createBoxOffice : ActorRef = system.actorOf(BoxOffice.props, BoxOffice.name)
}

trait RestRoutes extends BoxOfficeApi
    with EventMarshalling {
  import StatusCodes._

  def routes: Route = eventsRoute ~ eventRoute ~ ticketsRoute

  def eventsRoute: server.Route =
    pathPrefix("events") {
      pathEndOrSingleSlash {
        get {
          // GET /events
          onSuccess(getEvents) { events =>
            complete(OK, events)
          }
        }
      }
    }

  def eventRoute: server.Route =
    pathPrefix("events" / Segment) { event =>
      pathEndOrSingleSlash {
        post {
          // POST /events/:event
          entity(as[EventDescription]) { ed =>
            onSuccess(createEvent(event, ed.tickets)) {
              case BoxOffice.EventCreated(e) => complete(Created, e)
              case BoxOffice.EventExists =>
                val err = Error(s"$event event exists already.")
                complete(BadRequest, err)
            }
          }
        } ~
        get {
          // GET /events/:event
          onSuccess(getEvent(event)) {
            _.fold(complete(NotFound))(e => complete(OK, e))
          }
        } ~
        delete {
          // DELETE /events/:event
          onSuccess(cancelEvent(event)) {
            _.fold(complete(NotFound))(e => complete(OK, e))
          }
        }
      }
    }



  def ticketsRoute: server.Route =
    pathPrefix("events" / Segment / "tickets") { event =>
      post {
        pathEndOrSingleSlash {
          // POST /events/:event/tickets
          entity(as[TicketRequest]) { request =>
            onSuccess(requestTickets(event, request.tickets)) { tickets =>
              if(tickets.entries.isEmpty) complete(NotFound)
              else complete(Created, tickets)
            }
          }
        }
      }
    }

}

trait BoxOfficeApi {
  import BoxOffice._

  def createBoxOffice: ActorRef

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val boxOffice: ActorRef = createBoxOffice

  def createEvent(event: String, nrOfTickets: Int): Future[BoxOffice.EventResponse] =
    boxOffice.ask(CreateEvent(event, nrOfTickets))
      .mapTo[EventResponse]

  def getEvents: Future[BoxOffice.Events] =
    boxOffice.ask(GetEvents).mapTo[Events]

  def getEvent(event: String): Future[Option[BoxOffice.Event]] =
    boxOffice.ask(GetEvent(event))
      .mapTo[Option[Event]]

  def cancelEvent(event: String): Future[Option[BoxOffice.Event]] =
    boxOffice.ask(CancelEvent(event))
      .mapTo[Option[Event]]

  def requestTickets(event: String, tickets: Int): Future[TicketSeller.Tickets] =
    boxOffice.ask(GetTickets(event, tickets))
      .mapTo[TicketSeller.Tickets]
}
//
