package com.hs.haystack.tachyon.constituent.contentsimilarity

import org.apache.predictionio.controller.{Engine,EngineFactory}

case class Query(
  val doc: String,
  val limit: Int
) extends Serializable

case class PredictedResult(
  docScores: Array[DocScore]
) extends Serializable {
  override def toString: String = docScores.mkString(",")
}

case class DocScore(
  score: Double,
  id: String,
  similarText: String,
  textDesc: String	
) extends Serializable

object TextSimilarityEngine extends EngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("tsimilarity" -> classOf[TextSimilarityAlgorithm]),
      	classOf[Serving])
  }
}
