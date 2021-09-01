package shopping.cart

import scala.util.control.NonFatal

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.grpc.GrpcClientSettings
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import org.slf4j.LoggerFactory
import shopping.cart.repository.ItemPopularityRepositoryImpl
import shopping.cart.repository.ScalikeJdbcSetup
import shopping.order.proto.ShoppingOrderService
import shopping.order.proto.ShoppingOrderServiceClient

object Main {

  val logger = LoggerFactory.getLogger("shopping.cart.Main")

  def main(args: Array[String]): Unit = {
    val system =
      ActorSystem[Nothing](Behaviors.empty, "ShoppingCartService")
    try {
      init(system)
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

  def init(system: ActorSystem[_]): Unit = {

    ScalikeJdbcSetup.init(system)

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    ShoppingCart.init(system)

    val itemPopularityRepository = new ItemPopularityRepositoryImpl()
    ItemPopularityProjection.init(system, itemPopularityRepository)

    ShardedDaemonProcess(system).init(
      name = "ticker",
      4,
      index => TickerBehavior(s"ticker-$index"),
      ShardedDaemonProcessSettings(system),
      None)

    val grpcInterface =
      system.settings.config.getString("shopping-cart-service.grpc.interface")
    val grpcPort =
      system.settings.config.getInt("shopping-cart-service.grpc.port")
    val grpcService =
      new ShoppingCartServiceImpl(system, itemPopularityRepository)
    ShoppingCartServer.start(grpcInterface, grpcPort, system, grpcService)

  }

  protected def orderServiceClient(
      system: ActorSystem[_]): ShoppingOrderService = {
    val orderServiceClientSettings =
      GrpcClientSettings
        .connectToServiceAt(
          system.settings.config.getString("shopping-order-service.host"),
          system.settings.config.getInt("shopping-order-service.port"))(system)
        .withTls(false)
    ShoppingOrderServiceClient(orderServiceClientSettings)(system)
  }

}
