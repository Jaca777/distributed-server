package pl.jaca.server.networking

import java.io.IOException
import java.net.InetSocketAddress

import akka.actor.{ActorSystem, ActorRef}
import akka.event.Logging
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import pl.jaca.server.Session
import pl.jaca.server.packets.InPacket

/**
 * @author Jaca777
 *         Created 2015-06-12 at 16
 */
class ChannelHandler(proxyFactory: Channel => ActorRef, server: ActorRef, system: ActorSystem) extends ChannelInboundHandlerAdapter {

  private var session: Session = null

  var logging = Logging

  override def channelActive(ctx: ChannelHandlerContext) {
    val channel = ctx.channel()
    val address = channel.remoteAddress().asInstanceOf[InetSocketAddress]
    session = new Session(address.getHostString, address.getPort, channel.id(), proxyFactory(channel))
    server ! Server.EventOccurred(ServerEvent.SessionActive(session))
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    server ! Server.EventOccurred(ServerEvent.SessionInactive(session))
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: Object) {
    val partialPacket = msg.asInstanceOf[Session => InPacket]
    val packet = partialPacket(session)
    server ! Server.EventOccurred(packet)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) = cause match {
    case e: IOException => ctx.close()
    case any =>
      any.printStackTrace()
      ctx.close()
  }
}
