package pl.jaca.server.proxy.server

import pl.jaca.server.proxy.Connection
import pl.jaca.server.proxy.packets.InPacket


/**
 * @author Jaca777
 *         Created 2015-06-13 at 17
 */
abstract class PacketResolver {
  type Resolve = PartialFunction[Short, (Short, Short, Array[Byte], Connection) => InPacket]

  def construct(t: Short, length: Short, rest: Array[Byte], sender: Connection): InPacket = (resolve orElse unknown)(t)(t, length, rest, sender)

  private val unknown: Resolve = {
    case _ => UnknownPacket
  }
  def resolve: Resolve
}
case class UnknownPacket(i: Short, l: Short, rest: Array[Byte], s: Connection) extends InPacket(i,l,rest,s)