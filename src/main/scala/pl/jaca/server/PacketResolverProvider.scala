package pl.jaca.server

import java.lang.reflect.Modifier

import com.typesafe.config.Config
import pl.jaca.server.PacketResolverProvider.resolversPath
import pl.jaca.server.networking.PacketResolver


/**
 * @author Jaca777
 *         Created 2015-12-16 at 16
 */
private[server] class PacketResolverProvider(config: Config) {
  private val resolversEntry = config.getStringList(resolversPath).toArray.map(_.asInstanceOf[String])
  private val resolvers = resolversEntry.map(createResolver)

  def createResolver(className: String) = {
      val clazz: Class[_] = getResolverClass(className)
      clazz.newInstance().asInstanceOf[PacketResolver]
    }

    private def getResolverClass(className: String): Class[PacketResolver] = {
      try {
        val classLoader = this.getClass.getClassLoader
        val clazz = classLoader.loadClass(className)
        if (!classOf[PacketResolver].isAssignableFrom(clazz))
          throw new ServerConfigException("Resolver " + className + " is not type of PacketResolver.")
        if (!clazz.getConstructors.exists(_.getParameterCount == 0))
          throw new ServerConfigException("Resolver " + className + " has no parameterless constructor defined.")
        if (Modifier.isAbstract(clazz.getModifiers))
          throw new ServerConfigException("Resolver " + className + " is an abstract class.")
        clazz.asInstanceOf[Class[PacketResolver]]
      } catch {
        case c: ClassNotFoundException =>
          val e = new ServerConfigException("Resolver class not found.")
          e.initCause(c)
          throw e
      }
    }

    private val resolver = mergeResolvers(resolvers)

    def mergeResolvers(resolvers: Array[PacketResolver]) = resolvers.reduceLeft(_ and _)

    def getResolver: PacketResolver = resolver
  }

  object PacketResolverProvider {
    val resolversPath = "server-app.context.resolvers"
  }