package modules

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import play.api.{Configuration, Environment, Mode}
import adaptors.SparkAdaptor
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, _}

class Module(environment: Environment,
             config: Configuration) extends AbstractModule with ScalaModule{
  def configure(): Unit = {

    if(environment.mode != Mode.Test) {
      val sparkMaster = config.get[String]("spark.master")
      println("Connecting to " + sparkMaster)

      val spark = new SparkAdaptor(sparkMaster)

      bind[SparkAdaptor].toInstance(spark)
    }
  }
}

