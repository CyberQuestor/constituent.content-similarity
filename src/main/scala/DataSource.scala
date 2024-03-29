package com.hs.haystack.tachyon.constituent.contentsimilarity

import org.apache.predictionio.controller.PDataSource
import org.apache.predictionio.controller.EmptyEvaluationInfo
import org.apache.predictionio.controller.EmptyActualResult
import org.apache.predictionio.controller.Params
import org.apache.predictionio.data.store.PEventStore
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

case class DataSourceParams(appName: String, word2VecTrainFile: String) extends Params

class DataSource(val dsp: DataSourceParams)
  extends PDataSource[TrainingData, EmptyEvaluationInfo, Query, EmptyActualResult] {

  @transient lazy val logger = Logger[this.type]

  override
  def readTraining(sc: SparkContext): TrainingData = {
    println("Gathering data from event server.")
    val docsRDD: RDD[(String,String,String,String)] = PEventStore.aggregateProperties(
      appName = dsp.appName,
      entityType = "doc",
      required = Some(List("id","text","extTrainWords","desc")))(sc).map { case (entityId, properties) =>
        try {
	  (properties.get[String]("id"), properties.get[String]("text"), properties.getOrElse[String]("extTrainWords",""), properties.getOrElse[String]("desc",""))
        } catch {
          case e: Exception => {
            logger.error(s"Failed to get properties ${properties} of" +
              s" ${entityId}. Exception: ${e}.")
            throw e
          }
        }
      }
		
    new TrainingData(docs = docsRDD, word2VecTrainFile = dsp.word2VecTrainFile)
  }
}

class TrainingData(
  val docs: RDD[(String, String, String, String)],
  val word2VecTrainFile: String
) extends Serializable
