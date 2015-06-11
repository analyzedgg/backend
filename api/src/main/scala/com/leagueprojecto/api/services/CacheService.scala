package com.leagueprojecto.api.services

import java.util.Calendar
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout
import scala.collection._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.reflect.ClassTag

object CacheService {
  case class CachedResponse[R](response: R, cacheInvalidationDate: Long)
  case object RemoveInvalidatedCache

  def props[R : ClassTag](target: ActorRef, keepCacheMilliseconds: Long) = Props(new CacheService[R](target, keepCacheMilliseconds))
}

class CacheService[R : ClassTag](target: ActorRef, keepCacheMilliseconds: Long) extends Actor with ActorLogging {
  import CacheService._

  implicit val timeout: Timeout = 1.minute

  // 3 dimensional map of Message, Response, InvalidationDate
  val cacheMap = new mutable.HashMap[Any, (R, Long)]

  val cacheInvalidator = context.system.scheduler.schedule(0.second, 5.second, self, RemoveInvalidatedCache)

  override def receive: Receive = {
    case RemoveInvalidatedCache =>
      val timestamp = Calendar.getInstance().getTime.getTime
      cacheMap.iterator.filter(x => x._2.productElement(1).toString.toLong < timestamp).foreach(cacheMap -= _._1)

    case message: Any =>
      if (cacheMap.isDefinedAt(message)) {
        val cacheHit = cacheMap(message)
        sender() ! CachedResponse(cacheHit._1, cacheHit._2)
        println(s"Successful hit on cache for message '$message' with invalidation date: '${cacheHit._2}'")
      } else {
        val origSender = sender()

        // Since scala has type erasure the following code will capture the runtimeClass of the generic.
        // That enables us to match on the Class of the response
        val runtimeClass = implicitly[ClassTag[R]].runtimeClass
        (target ? message).collect[CachedResponse[R]] {
          case response if runtimeClass.isInstance(response) =>
            val typedResponse = response.asInstanceOf[R]

            val timestamp = Calendar.getInstance().getTime.getTime + (keepCacheMilliseconds * 1000)
            cacheMap.put(message, (typedResponse, timestamp))

            println(s"Added response for message '$message' in cache map with invalidation date: '$timestamp'")

            CachedResponse[R](typedResponse, timestamp)
          case _ =>
            throw new IllegalArgumentException("Response from service was not of type " + runtimeClass.getSimpleName)
        }.pipeTo(origSender)
      }
  }
}
