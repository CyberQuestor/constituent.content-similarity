package com.hs.haystack.tachyon.constituent.contentsimilarity

import org.apache.predictionio.controller.PPreparator
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class PreparedData(
  val docs: RDD[(String,String,String,String,String,String)],
  val word2VecTrainFile: String
) extends Serializable

class Preparator
  extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(docs = trainingData.docs, word2VecTrainFile = trainingData.word2VecTrainFile)
  }
}

