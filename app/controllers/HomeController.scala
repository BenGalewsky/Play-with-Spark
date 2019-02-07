package controllers

import forms.{SearchCriteria, SearchCriteriaForm}
import javax.inject.Inject
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

// Spark
import adaptors.SparkAdaptor


class HomeController @Inject()(cc: ControllerComponents, spark: SparkAdaptor)
  extends AbstractController(cc)
    with play.api.i18n.I18nSupport {

  def index = Action { implicit request =>
    val datasetOptions = Set(("*", "*")) ++  spark.primary_datasets.map(d=>(d,d))
    val processingVersionOptions = Set(("*", "*")) ++  spark.processing_version.map(d=>(d,d))
    val dataTierOptions = Set(("*", "*")) ++  spark.data_tier.map(d=>(d,d))

    val data = SearchCriteria("*", "*", "*")
    val form = SearchCriteriaForm.searchCriteriaForm.fill(
      data
    )

    Ok(views.html.index(datasetOptions.toList, processingVersionOptions.toList, dataTierOptions.toList, form))
  }

  def provisionAnalysis = Action(parse.form(SearchCriteriaForm.searchCriteriaForm)) { implicit request =>
    val searchCriteia = request.body
    val rootFiles = spark.queryRootFiles(searchCriteia)
    val df0 = spark.ingestRootFiles(rootFiles)

    Ok(views.html.provisioning(rootFiles, df0))
  }

  def filterEvents = Action(parse.json){ implicit request:Request[JsValue] =>
    val handle = request.getQueryString("dataframe")
    val query = (request.body \ "query").as[String]
    Ok(spark.filterEvents(handle.get, query))
  }

  def filterCandidates = Action(parse.json) { implicit request: Request[JsValue] =>
    val handle = request.getQueryString("dataframe")
    val query = (request.body \ "query").as[String]
    val physicsObject = (request.body \ "physics_object").as[String]
    val newHandle = spark.filterCandidates(handle.get, physicsObject, query)
    Ok(newHandle)
  }

  def reassembleEvents  = Action(parse.json) { implicit request: Request[JsValue] =>
    val handle = request.getQueryString("dataframe")
    val physicsObject = (request.body \ "physics_object").as[String]
    val newHandle = spark.reAssembleEvent(handle.get, physicsObject)
    Ok(newHandle)
  }

  def renderDataset(handle:String) = Action { implicit  request=>
    val rows = spark.take(handle)
    val headers = rows(0).schema

    Ok(views.html.dataset(handle, rows))
  }

    // A simple example to call Apache Spark
  def test = Action { implicit request =>
  	val sum = spark.Example
    Ok(views.html.test_args(s"A call to Spark, with result: $sum"))
  }

  // A non-blocking call to Apache Spark 
  def testAsync = Action.async{
  	val futureSum = Future{spark.Example}
    futureSum.map{ s => Ok(views.html.test_args(s"A non-blocking call to Spark with result: ${s + 1000}"))}
  }

}
