package shopping.cart

import scala.concurrent.duration.DurationInt

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object TickerBehavior {

  sealed trait Command
  case object Tick extends Command
  case object Stop extends Command

  def apply(name: String): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      Behaviors.withTimers { timer =>
        timer.startTimerAtFixedRate(Tick, 5.seconds, 10.seconds)
        ctx.log.debug(s"====> starting ticker $name <====")
        Behaviors
          .receiveMessage[Command] {
            case Tick =>
              ctx.log.info(s"====> got tick in $name <====")
              Behaviors.same
            case Stop =>
              ctx.log.info(s"====> got stop message in $name <====")
              Behaviors.stopped
          }
          .receiveSignal { case (ctx, any) =>
            ctx.log.info(s"====> got signal '$any' in $name <====")
            Behaviors.same
          }
      }
    }

}
