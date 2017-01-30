/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.rdd

import java.io.{IOException, ObjectOutputStream}

import org.apache.spark.{Partition, SparkContext}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.util.control.NonFatal

object ZippedPartitionsWithLocalityRDD {
  def apply[T: ClassTag, B: ClassTag, V: ClassTag]
  (rdd1: RDD[T], rdd2: RDD[B], preservesPartitioning: Boolean = false)
    (f: (Iterator[T], Iterator[B]) => Iterator[V]): RDD[V] = rdd1.withScope {
    val sc = rdd1.sparkContext
    new ZippedPartitionsWithLocalityRDD(
      sc, sc.clean(f), rdd1, rdd2, preservesPartitioning)
  }
}

class ZippedPartitionsWithLocalityRDD[A: ClassTag, B: ClassTag, V: ClassTag](
  sc: SparkContext,
  _f: (Iterator[A], Iterator[B]) => Iterator[V],
  _rdd1: RDD[A],
  _rdd2: RDD[B],
  preservesPartitioning: Boolean = false)
  extends ZippedPartitionsRDD2[A, B, V](sc, _f, _rdd1, _rdd2, preservesPartitioning) {

  override def getPartitions: Array[Partition] = {
    require(rdds.length == 2, "this is only for 2 rdd zip")
    val numParts = rdds.head.partitions.length
    if (!rdds.forall(rdd => rdd.partitions.length == numParts)) {
      throw new IllegalArgumentException("Can't zip RDDs with unequal numbers of partitions")
    }

    val candidateLocs = new ArrayBuffer[(Int, Seq[String])]()
    (0 until numParts).foreach(p => {
      candidateLocs.append((p, rdds(1).context.getPreferredLocs(rdds(1), p).map(_.host).distinct))
    })
    val nonmatchPartitionId = new ArrayBuffer[Int]()
    val parts = new Array[Partition](numParts)

    (0 until  numParts).foreach { i =>
      val curPrefs = rdds(0).context.getPreferredLocs(rdds(0), i).map(_.host).distinct
      var p = 0
      var matchPartition: (Int, Seq[String]) = null
      var locs: Seq[String] = null
      while (p < candidateLocs.length) {
        locs = candidateLocs(p)._2.intersect(curPrefs)
        if (!locs.isEmpty) {
          matchPartition = candidateLocs.remove(p)
          p = Integer.MAX_VALUE - 1
        }
        p += 1
      }

      if (matchPartition != null) {
        parts(i) =
          new ZippedPartitionsLocalityPartition(i, Array(i, matchPartition._1), rdds, locs)
      } else {
        println(s"can't find locality partition for partition $i " +
          s"Partition locations are (${curPrefs}) Candidate partition locations are\n" +
          s"${candidateLocs.mkString("\n")}.")
        nonmatchPartitionId.append(i)
      }
    }

    require(nonmatchPartitionId.size == candidateLocs.size,
      "unmatched partition size should be the same with candidateLocs size")
    nonmatchPartitionId.foreach { i =>
      val locs = rdds(0).context.getPreferredLocs(rdds(0), i).map(_.host).distinct
      val matchPartition = candidateLocs.remove(0)
      parts(i) = new ZippedPartitionsLocalityPartition(i, Array(i, matchPartition._1), rdds, locs)
    }
    parts
  }
}

private[spark] class ZippedPartitionsLocalityPartition(
  idx: Int,
  @transient val indexes: Seq[Int],
  @transient val rdds: Seq[RDD[_]],
  @transient override val preferredLocations: Seq[String])
  extends ZippedPartitionsPartition(idx, rdds, preferredLocations) {

  override val index: Int = idx
  var _partitionValues = rdds.zip(indexes).map{ case (rdd, i) => rdd.partitions(i) }
  override def partitions: Seq[Partition] = _partitionValues

  @throws(classOf[IOException])
  private def writeObject(oos: ObjectOutputStream): Unit = {
    try {
      // Update the reference to parent split at the time of task serialization
      _partitionValues = rdds.zip(indexes).map{ case (rdd, i) => rdd.partitions(i) }
      oos.defaultWriteObject()
    } catch {
      case e: IOException =>
        throw e
      case NonFatal(e) =>
        throw new IOException(e)
    }
  }
}



