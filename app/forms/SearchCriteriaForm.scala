package forms

case class SearchCriteria (primary_dataset: String, processing_version: String, data_tier:String)

import play.api.data._
import play.api.data.Forms._

object SearchCriteriaForm {
  val searchCriteriaForm = Form (
  mapping (
  "primary_dataset" -> text,
    "processing_version" -> text,
    "data_tier" -> text
  ) (SearchCriteria.apply) (SearchCriteria.unapply)
  )
}