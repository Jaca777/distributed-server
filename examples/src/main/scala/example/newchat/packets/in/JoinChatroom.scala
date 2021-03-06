package example.newchat.packets.in

import example.newchat.model.service.Chat
import pl.jaca.server.Session
import pl.jaca.server.packets.InPacket

/**
 * @author Jaca777
 *         Created 2015-12-17 at 19
 */
case class JoinChatroom(i: Short, l: Short, m: Array[Byte], s: Session) extends InPacket(i, l, m, s) {
  implicit val charset = Chat.charset
  private val nameLength = m.readShort()
  val channelName = m.readString(nameLength)
}
