package pl.jaca.server

import akka.actor.{ActorLogging, ActorRef, Props}
import pl.jaca.cluster.Application.Launch
import pl.jaca.cluster.distribution.Distribution
import pl.jaca.cluster.{Application, Configurable, SystemNode}
import pl.jaca.server.ServerApplicationRoot.Shutdown
import pl.jaca.server.networking.Server
import pl.jaca.server.networking.Server.Subscribe
import pl.jaca.server.providers.{ServiceDependencyResolver, EventHandlerProvider, PacketResolverProvider, ServiceDependencyResolver$}

/**
  * @author Jaca777
  *         Created 2015-06-15 at 21
  *         ServerApplicationRoot is responsible for constructing and launching server. Creates services and handlers.
  *
  */
class ServerApplicationRoot extends Application with Distribution with Configurable with ActorLogging {

  private implicit val executor = context.dispatcher

  val systemConfig = context.system.settings.config
  val resolverProvider = new PacketResolverProvider(appConfig)
  val serviceProvider = new ServiceDependencyResolver(appConfig, createService, log)
  val handlerProvider = new EventHandlerProvider(appConfig, serviceProvider, createHandler)

  /**
    * Awaiting command to launch.
    */
  override def receive: Receive = {
    case Launch =>
      val server = launchServer()
      context become running(server)
  }

  def running(server: ActorRef): Receive = {
    case Shutdown =>
      server ! Server.Stop
      context.parent ! SystemNode.Shutdown
  }

  /**
    * Launches both server and client application.
    */
  def launchServer() = {
    val port = getPort
    val resolver = resolverProvider.getResolver
    val handlers = handlerProvider.eventActors
    val server = context.actorOf(Props(new Server(port, resolver)))
    for {
      handler <- handlers
    } server ! Subscribe(handler)
    server
  }


  def getPort: Int =
    systemConfig.intAt("server-app.port").getOrElse(ServerApplicationRoot.defaultPort)

  def createService(p: Props, name: String) = context.distribute(p, name)

  def createHandler(p: Props, name: String) = {
    context.distribute(p, name)
  }

}

object ServerApplicationRoot {
  val defaultPort = 41154

  //IN
  object Shutdown

}