package pl.jaca.server.networking

import pl.jaca.server.Session
import pl.jaca.server.packets.InPacket


/**
 * @author Jaca777
 *         Created 2015-06-13 at 17
 */
abstract class PacketResolver {

  type Resolve = PartialFunction[Short, (Short, Short, Array[Byte], Session) => InPacket]

  def resolve(id: Short, length: Short, data: Array[Byte], sender: Session): InPacket = {
    try {
      (resolve orElse unknown)(id)(id, length, data, sender)
    } catch {
      case exception: Exception =>
        UnresolvedPacket(s"Exception thrown when resolving packet: ${exception.toString}",
          id, length, data, sender)
    }
  }

  private val unknown: Resolve = {
    case _ => (i, l, d, s) => UnresolvedPacket(s"Unknown packet of id: $i", i, l, d, s)
  }

  def resolve: Resolve

  /**
   * Combines @resolver with @this. Given resolver has higher priority, i.e. it's called as first.
   */
  def and(resolver: PacketResolver): PacketResolver = {
    new PacketResolver {
      override def resolve: Resolve = resolver.resolve orElse PacketResolver.this.resolve
    }
  }
}

case class UnresolvedPacket(cause: String, i: Short, l: Short, data: Array[Byte], s: Session) extends InPacket(i, l, data, s)
