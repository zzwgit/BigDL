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

package com.intel.analytics.bigdl.models.inception

import scopt.OptionParser

object Options {

  case class TrainParams(
    folder: String = "./",
    checkpoint: Option[String] = None,
    modelSnapshot: Option[String] = None,
    stateSnapshot: Option[String] = None,
    classNumber: Int = 1000,
    batchSize: Int = -1,
    learningRate: Double = 0.01,
    env: String = "local",
    overWriteCheckpoint: Boolean = false,
    nesterov: Boolean = false,
    maxLr: Double = 0.14,
    warmUpEpoch: Int = 5,
    maxEpoch: Option[Int] = None,
    resumeEpoch: Option[Int] = None,
    maxIteration: Int = 62000,
    weightDecay: Double = 0.0001,
    checkpointIteration: Int = 620,
    graphModel: Boolean = false,
    gradientClipMax: Double = 0
  )

  val trainParser = new OptionParser[TrainParams]("BigDL Inception Example") {
    opt[String]('f', "folder")
      .text("url of hdfs folder store the hadoop sequence files")
      .action((x, c) => c.copy(folder = x))
    opt[String]("model")
      .text("model snapshot location")
      .action((x, c) => c.copy(modelSnapshot = Some(x)))
    opt[String]("state")
      .text("state snapshot location")
      .action((x, c) => c.copy(stateSnapshot = Some(x)))
    opt[String]("checkpoint")
      .text("where to cache the model")
      .action((x, c) => c.copy(checkpoint = Some(x)))
    opt[Int]('e', "maxEpoch")
      .text("epoch numbers")
      .action((x, c) => c.copy(maxEpoch = Some(x)))
    opt[Int]("resumeEpoch")
      .text("which epoch to start")
      .action((x, c) => c.copy(resumeEpoch = Some(x)))
    opt[Int]('i', "maxIteration")
      .text("iteration numbers")
      .action((x, c) => c.copy(maxIteration = x))
    opt[Double]('l', "learningRate")
      .text("inital learning rate")
      .action((x, c) => c.copy(learningRate = x))
      .required()
    opt[Int]('b', "batchSize")
      .text("batch size")
      .action((x, c) => c.copy(batchSize = x))
      .required()
    opt[Int]("classNum")
      .text("class number")
      .action((x, c) => c.copy(classNumber = x))
    opt[Unit]("overWrite")
      .text("overwrite checkpoint files")
      .action( (_, c) => c.copy(overWriteCheckpoint = true) )
    opt[Unit]("nesterov")
      .text("enable nesterov momentum")
      .action( (_, c) => c.copy(nesterov = true) )
    opt[Double]("maxLr")
      .text("max Lr after warm up")
      .action((x, c) => c.copy(maxLr = x))
    opt[Int]("warmUpEpoch")
      .text("how many epoch to warm up")
      .action((x, c) => c.copy(warmUpEpoch = x))
    opt[Double]("weightDecay")
      .text("weight decay")
      .action((x, c) => c.copy(weightDecay = x))
    opt[Int]("checkpointIteration")
      .text("checkpoint interval of iterations")
      .action((x, c) => c.copy(checkpointIteration = x))
    opt[Unit]('g', "graphModel")
      .text("use graph model")
      .action((x, c) => c.copy(graphModel = true))
    opt[Double]("gradientClipMax")
      .text("max norm2 of gradient clipping")
      .action((x, c) => c.copy(gradientClipMax = x))
  }

  case class TestParams(
    folder: String = "./",
    model: String = "",
    batchSize: Option[Int] = None
  )

  val testParser = new OptionParser[TestParams]("BigDL Inception Test Example") {
    opt[String]('f', "folder")
      .text("url of hdfs folder store the hadoop sequence files")
      .action((x, c) => c.copy(folder = x))
    opt[String]("model")
      .text("model snapshot location")
      .action((x, c) => c.copy(model = x))
      .required()
    opt[Int]('b', "batchSize")
      .text("batch size")
      .action((x, c) => c.copy(batchSize = Some(x)))
  }
}
