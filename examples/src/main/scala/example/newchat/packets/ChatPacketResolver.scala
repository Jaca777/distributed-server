package example.newchat.packets

import pl.jaca.server.networking.PacketResolver

/**
 * @author Jaca777
 *         Created 2015-06-13 at 15
 */
class ChatPacketResolver extends PacketResolver{
  def resolve: Resolve = {
    case 0 => in.Login
    case 1 => in.Register
    case 2 => in.JoinChatroom
    case 3 => in.SendMessage
  }
}
