package adaptors

import forms.SearchCriteria
import org.apache.spark.sql.{DataFrame, Row, SparkSession}

class SparkAdaptor(masterURI:String) {
  val sparkS = SparkSession
    .builder
    .config("spark.jars.packages", "org.diana-hep:spark-root_2.11:0.1.15")
    .master(masterURI).getOrCreate

  import sparkS.implicits._

  val db = sparkS.read.parquet("file:/Users/bengal1/dev/IRIS-HEP/data/db.parquet")

  val dataframes = scala.collection.mutable.Map[String, DataFrame]()


  def Example : Int = {
    val sum = Seq(3, 2, 4, 1, 0, 30,30,40,50,-4).toDS
    sum.count.toInt
  }

  def primary_datasets: Set[String] = {
    val rslt = db.select("primary_dataset").distinct.take(10)
    rslt.map(r=>r.getString(0)).to[Set]
  }

  def processing_version: Set[String] = {
    val rslt = db.select("processing_version").distinct.take(10)
    rslt.map(r=>r.getString(0)).to[Set]
  }

  def data_tier: Set[String] = {
    val rslt = db.select("data_tier").distinct.take(10)
    rslt.map(r=>r.getString(0)).to[Set]
  }

  def queryRootFiles(searchCriteria: SearchCriteria): List[String] = {
    val rslt = db.filter($"primary_dataset" === searchCriteria.primary_dataset)
      .select($"root_file").take(10)
    rslt.map(r=>r.getString(0)).toList
  }

  def ingestRootFiles(rootFiles: List[String]): String = {
    val df=sparkS.read.format("org.dianahep.sparkroot")
      .option("tree", "Events")
      .load("file:/Users/bengal1/dev/IRIS-HEP/data/DYJetsToLL_M-50_HT-100to200_TuneCUETP8M1_13TeV-madgraphMLM-pythia8.root")


      saveDataFrame(df)
  }

  def filterEvents(handle:String, query: String) = {
    val df = dataframes(handle)
    df.createOrReplaceTempView(handle)
    saveDataFrame(sparkS.sql(query))
  }

  def filterCandidates(handle:String, physicsObj: String, candidateQuery: String):String = {
    val df = dataframes(handle)
    df.createOrReplaceTempView(handle)

    val candidateCols = df.schema.map(d=>d.name).filter(s=>s.startsWith(physicsObj+"_"))
    print("will be lookinf for "+candidateCols)

    val selectStmt = "SELECT event, n"+physicsObj+", c0.count, " + candidateCols.zipWithIndex.map{case (col, index)=>"c"+index+"."+col}.mkString(",")
    val lateralViews = candidateCols.zipWithIndex.map{case(col, index) =>
      s"LATERAL VIEW posexplode($col) ${"c"+index} as count, $col" }

    val joins = (1 to candidateCols.size-1).map(c=>s"c0.count = c$c.count").mkString(" AND ")

    val query = selectStmt +" FROM "+handle+ " "+lateralViews.mkString(" ") + " WHERE "+joins
    print("\n\n---> "+query+"\n\n")
    val explodedDF = sparkS.sql(query)
    explodedDF.createOrReplaceTempView(handle+"Exploded")

    val result = sparkS.sql(candidateQuery)
    result.show
    saveDataFrame(result)
  }

  def reAssembleEvent(handle: String, physicsObj: String): String = {
    val df = dataframes(handle)
    df.createOrReplaceTempView(handle)

    val colNames = df.schema.map(d=>d.name).filter(c=>c != "count")
    val aggregatedCols = colNames.filter(s=>s.startsWith(physicsObj+"_"))
    val groupByCols = colNames diff aggregatedCols

    val query = "SELECT "+groupByCols.mkString(",") +
      ", "+aggregatedCols.map(c=>s"collect_list($c)").mkString(", ") +
      " FROM "+handle+
      " GROUP BY "+groupByCols.mkString(",")

    print("Query = "+query)
    val result = sparkS.sql(query)
    result.show
    saveDataFrame(result)

  }

  def take(handle:String, n:Int=10): Array[Row] = {
    val df = dataframes(handle)
    df.take(n)
  }

  private def saveDataFrame(dataFrame: DataFrame): String = {
    val handle = "Res"+ dataframes.size
    dataframes(handle) = dataFrame
    handle
  }
}
