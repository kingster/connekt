package com.flipkart.connekt.commons.utils

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
        val result = if (iterators.nonEmpty) {
          inUseItrIndex = (1 + inUseItrIndex) % iterators.size
          iterators(inUseItrIndex).hasNext || {
            def r(stopIdx: Int): Boolean = {
              inUseItrIndex = (1 + inUseItrIndex) % iterators.size

              if (stopIdx != inUseItrIndex) {
                iterators(inUseItrIndex).hasNext || r(stopIdx)
              } else false
            }

            r(inUseItrIndex)
          }
        } else false

        result
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
