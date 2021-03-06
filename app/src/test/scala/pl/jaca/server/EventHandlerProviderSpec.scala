package pl.jaca.server

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}
import pl.jaca.server.EventHandlerProviderSpec.HandlerA
import pl.jaca.server.eventhandling.EventActor
import pl.jaca.server.providers.{ServiceDependencyResolver, EventHandlerProvider, ServiceDependencyResolver$}
import pl.jaca.server.service.Service

import scala.concurrent.ExecutionContext.Implicits._
import scala.language.postfixOps

/**
 * @author Jaca777
 *         Created 2015-12-17 at 20
 */
class EventHandlerProviderSpec extends TestKit(ActorSystem("EventHandlerProviderSpec")) with WordSpecLike with Matchers {

  val properConfig1 = ConfigFactory.load("server/conf1.conf")
  val wrongConfig1 = ConfigFactory.load("server/conf2.conf")
  val wrongConfig2 = ConfigFactory.load("server/conf3.conf")
  val wrongConfig3 = ConfigFactory.load("server/conf4.conf")
  val wrongConfig4 = ConfigFactory.load("server/conf5.conf")

  val log = new DummyLoggingAdapter

  class DummyActor extends Actor {
    override def receive: Receive = {
      case _ =>
    }
  }

  val actorA = TestActorRef(new DummyActor)
  val actorB = TestActorRef(new DummyActor)
  
  def createHandler(p: Props, name: String) = TestActorRef(p)

  object DummyServiceProvider extends ServiceDependencyResolver(properConfig1, (_, _) => null, log) {

    override def getService(name: String): Option[ActorRef] = name match {
      case "serviceA" => Some(actorA)
      case "serviceB" => Some(actorB)
    }
  }

  "EventHandlerProvider" must {
    "load handlers from config" in {
      val eventHandlerProvider = new EventHandlerProvider(properConfig1, DummyServiceProvider, createHandler)
      val handlers = eventHandlerProvider.eventActors
      val actors = handlers.map(_.asInstanceOf[TestActorRef[_]]).map(_.underlyingActor)
      val handlerA = actors.find(_.isInstanceOf[HandlerA]).get.asInstanceOf[HandlerA]
      handlerA.serviceA should be(actorA)
      handlerA.serviceB should be(actorB)
      ()
    }
    "throw exception when class in not type of handler" in {
      intercept[ServerConfigException] {
        new EventHandlerProvider(wrongConfig1, DummyServiceProvider, createHandler)
      }.getMessage should be("pl.jaca.server.EventHandlerProviderSpec$NotAHandler is not type of EventActor.")
    }
    "throw exception when class is not found" in {
      intercept[ServerConfigException] {
        new EventHandlerProvider(wrongConfig2, DummyServiceProvider, createHandler)
      }.getCause shouldBe a[ClassNotFoundException]
    }
    "throw exception when class has no injectable constructor" in {
      intercept[ServerConfigException] {
        new EventHandlerProvider(wrongConfig3, DummyServiceProvider, createHandler)
      }.getMessage should be("Event handler pl.jaca.server.EventHandlerProviderSpec$NonInjectableHandler has no constructor with injectable parameters.")
    }
    "throw exception when class is abstract" in {
      intercept[ServerConfigException] {
        new EventHandlerProvider(wrongConfig4, DummyServiceProvider, createHandler)
      }.getMessage should be("Event handler pl.jaca.server.EventHandlerProviderSpec$AbstractHandler is an abstract class.")
    }
  }

}

object EventHandlerProviderSpec {

  class NotAHandler

  abstract class AbstractHandler extends EventActor

  class NonInjectableHandler(service: Service) extends EventActor

  class HandlerA(@Inject(serviceName = "serviceA") val serviceA: ActorRef, @Inject(serviceName = "serviceB") val serviceB: ActorRef) extends EventActor

  class HandlerB extends EventActor

  class HandlerC extends EventActor

}
