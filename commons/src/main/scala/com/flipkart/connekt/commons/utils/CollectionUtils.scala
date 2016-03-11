package com.flipkart.connekt.commons.utils

import kafka.consumer.ConsumerTimeoutException

import scala.collection.{AbstractIterator, Iterator}

/**
 *
 *
 * @author durga.s
 * @version 3/10/16
 */
object CollectionUtils {

  implicit class iteratorUtils[T](iterators: List[Iterator[T]]) {
    def merge = new AbstractIterator[T] {

      private var inUseItrIndex = 0

      override def hasNext: Boolean = {
        if (iterators.nonEmpty) {
          inUseItrIndex = (1 + inUseItrIndex) % iterators.size

          def doesHaveNext(index: Int) = try { iterators(index).hasNext } catch { case e: ConsumerTimeoutException => false }

          def doOthersHaveNext() = {
            def r(stopIdx: Int): Boolean = {
              inUseItrIndex = (1 + inUseItrIndex) % iterators.size

              if (stopIdx != inUseItrIndex) {
                doesHaveNext(inUseItrIndex) || r(stopIdx)
              } else false
            }

            r(inUseItrIndex)
          }

          doesHaveNext(inUseItrIndex) || doOthersHaveNext
        } else false
      }

      override def next(): T = {
        if(iterators.nonEmpty)
          iterators(inUseItrIndex).next()
        else
          Iterator.empty.next()
      }
    }
  }

}
