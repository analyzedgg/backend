package com.leagueprojecto.api.services.riot

import java.util.Calendar

import akka.actor.{ActorLogging, Props, ActorRef, Actor}
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.collection._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag

object CacheService {
  case class CachedResponse[R](response: R, cacheInvalidationDate: Long)
  case object RemoveInvalidatedCache

  def props[R : ClassTag](target: ActorRef, keepCacheSeconds: Int) = Props(new CacheService[R](target, keepCacheSeconds))

  class Map3D[K, V, R] {
    private val wrapped = new mutable.HashMap[K, (V, R)]
    def put(k: K)(v: V)(r: R) = wrapped.put(k, (v, r))
    def -(k: K) = wrapped - k
    def isDefinedAt(k: K) = wrapped.isDefinedAt(k)
    def size = wrapped.size
    def apply(k: K) = wrapped(k)
    def apply(i: Iterator[(K, (V, R))]) =
      i.foreach {entree =>
        put(entree._1)(entree._2.productElement(0).asInstanceOf[V])(entree._2.productElement(1).asInstanceOf[R])
      }

    def iterator: scala.Iterator[(K, (V, R))] = wrapped.iterator
  }
}

class CacheService[R : ClassTag](target: ActorRef, keepCacheSeconds: Int) extends Actor with ActorLogging {
  import CacheService._

  implicit val timeout: Timeout = 1.second

  var cacheMap: Map3D[Any, R, Long] = new Map3D

  val cacheTimer = context.system.scheduler.schedule(1.second, 1.second, self, RemoveInvalidatedCache)

  override def receive: Receive = {
    case RemoveInvalidatedCache =>
      val timestamp = Calendar.getInstance().getTime.getTime
      val validCache = cacheMap.iterator.filter(x => x._2.productElement(1).toString.toLong < timestamp)
      cacheMap = new Map3D
      cacheMap.apply(validCache)

      println(cacheMap.size)

    case message: Any =>

      if (cacheMap.isDefinedAt(message)) {
        val cacheHit = cacheMap(message)
        sender() ! CachedResponse(cacheHit._1, cacheHit._2)
        log.debug(s"Successful hit on cache for message: $message")
      } else {
        val origSender = sender()

        // Since scala has type erasure the following code will capture the runtimeClass of the generic.
        // That enables us to match on the Class of the response
        val runtimeClass = implicitly[ClassTag[R]].runtimeClass
        (target ? message).collect[CachedResponse[R]] {
          case response if runtimeClass.isInstance(response) =>
            val typedResponse = response.asInstanceOf[R]

            val timestamp = Calendar.getInstance().getTime.getTime
            cacheMap.put(message)(typedResponse)(timestamp + keepCacheSeconds)

            log.debug(s"Added response for message '$message' in cache map")

            CachedResponse[R](typedResponse, timestamp)
          case _ =>
            throw new IllegalArgumentException("Response from service was not of type " + runtimeClass.getSimpleName)
        }.pipeTo(origSender)
      }



      target.?(message)
  }
}
