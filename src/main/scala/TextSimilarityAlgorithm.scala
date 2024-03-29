package com.hs.haystack.tachyon.constituent.contentsimilarity

import org.apache.predictionio.controller.P2LAlgorithm
import org.apache.predictionio.controller.Params
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.feature.{Word2Vec,Word2VecModel}
import grizzled.slf4j.Logger
import org.apache.spark.mllib.feature.Normalizer
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.sql.SQLContext

case class AlgorithmParams(
  val seed: Int,
  val minCount: Int,
  val learningRate: Double,
  val numIterations: Int,
  val vectorSize: Int,
  val minTokenSize: Int,
  val showText: Boolean,
  val showDesc: Boolean,
  val useExtTrainWords: Boolean,
  val storeClearText: Boolean
) extends Params

class TSModel(
  val word2VecModel: Word2VecModel,
  val docPairs: List[((String,String,String,String), breeze.linalg.DenseVector[Double])],
  val vectorSize: Int,
  val showText: Boolean,
  val showDesc: Boolean
) extends Serializable {}

class TextSimilarityAlgorithm(val ap: AlgorithmParams) extends P2LAlgorithm[PreparedData, TSModel, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): TSModel = {
    println("Training text similarity model.")

    val art1 = data.docs.map(x=>((x._2+{if (ap.useExtTrainWords) " "+x._3 else ""}).toLowerCase.replace("."," ").split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>=ap.minTokenSize).toSeq, (x._1,x._2,x._3,x._4))).filter(_._1.size>0)
    
    val word2vec = new Word2Vec()
    word2vec.setSeed(ap.seed)
    word2vec.setMinCount(ap.minCount)
    word2vec.setLearningRate(ap.learningRate)
    word2vec.setNumIterations(ap.numIterations)
    word2vec.setVectorSize(ap.vectorSize)	

    val vwtrain = if (data.word2VecTrainFile=="") art1.map(_._1).cache else {
      val sqlContext = new SQLContext(sc)      

      val df = sqlContext.read.parquet(data.word2VecTrainFile)
      val aa = df.select("properties").rdd.map(x=>(x.getStruct(0).getString(3).toLowerCase.replace("."," ").split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>=ap.minTokenSize).toSeq)).filter(_.size>0)

      aa.cache
    }

    val model = word2vec.fit(vwtrain)

    val art_pairs = art1.map(x => ( {if (ap.storeClearText) (x._2._1,x._2._2,x._2._3,x._2._4) else (x._2._1,"","",x._2._4)}, new DenseVector(divArray(x._1.map(m => wordToVector(m, model, ap.vectorSize).toArray).reduceLeft(sumArray),x._1.length)).asInstanceOf[Vector]))	
    
    val normalizer1 = new Normalizer()
    val art_pairsb = art_pairs.map(x=>(x._1, normalizer1.transform(x._2))).map(x=>(x._1,{new breeze.linalg.DenseVector(x._2.toArray)}))	

    new TSModel(model, art_pairsb.collect.toList, ap.vectorSize, ap.showText, ap.showDesc)
  }

  def predict(model: TSModel, query: Query): PredictedResult = {
    //Prepare query vector
    val td02 = query.doc.split(" ").filter(k => !stopwords.contains(k)).map(normalizet).filter(_.trim.length>1).toSeq
    val td02w2v = new DenseVector(divArray(td02.map(m => wordToVector(m, model.word2VecModel, model.vectorSize).toArray).reduceLeft(sumArray),td02.length)).asInstanceOf[Vector]
    val normalizer1 = new Normalizer()
    val td02w2vn = normalizer1.transform(td02w2v)
    val td02bv = new breeze.linalg.DenseVector(td02w2vn.toArray)
        
    val r = model.docPairs.map(x=>(td02bv.dot(x._2),x._1)).sortWith(_._1>_._1).take(query.limit).map(x=>{new DocScore(x._1, x._2._1, if(model.showText) x._2._2 else "", if (model.showDesc) x._2._4 else "")})
 
    PredictedResult(docScores = r.toArray)
  }

  def sumArray (m: Array[Double], n: Array[Double]): Array[Double] = {
    for (i <- 0 until m.length) {m(i) += n(i)}
    return m
  }

  def divArray (m: Array[Double], divisor: Double) : Array[Double] = {
    for (i <- 0 until m.length) {m(i) /= divisor}
    return m
  }

  def wordToVector (w:String, m: Word2VecModel, s: Int): Vector = {
    try {
      return m.transform(w)
    } catch {
      case e: Exception => return Vectors.zeros(s)
    }  
  }

  def normalizet(line: String) = java.text.Normalizer.normalize(line,java.text.Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+","").toLowerCase

  val stopwords = Array("foo").toSet
}
