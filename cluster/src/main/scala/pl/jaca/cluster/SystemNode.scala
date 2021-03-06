package pl.jaca.cluster

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import pl.jaca.cluster.SystemNode.Shutdown
import pl.jaca.cluster.distribution.Distribution.DistributionInitializer
import pl.jaca.cluster.distribution.Receptionist.PreciseSelectionStrategy
import pl.jaca.cluster.distribution.{Distribution, Receptionist}

/**
 * @author Jaca777
 *         Created 2015-08-16 at 20
 */
class SystemNode extends Actor with DistributionInitializer with Distribution {

  val cluster = Cluster(context.system)
  val receptionist = context.actorOf(Props(new Receptionist(PreciseSelectionStrategy)))
  initReceptionist(receptionist)

  override def receive: Receive = {
    case SystemNode.Launch(appFactory) =>
      val app = context.actorOf(Props(appFactory()), "app")
      app ! Application.Launch
    case SystemNode.GetReceptionist => sender ! SystemNode.Receptionist(receptionist)
    case Shutdown => context.system.terminate()
  }
}

object SystemNode {
  
  case class Launch(appFactory: () => _ <: Application)

  object Shutdown

  /**
   * Input message. When received, Node responses with object containing reference to receptionist actor.
   */
  object GetReceptionist

  /**
   * Response to GetReceptionist message.
   * @param receptionist Reference to receptionist actor.
   */
  case class Receptionist(receptionist: ActorRef)
}
