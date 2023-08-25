package json.reads

import ixias.util.json.JsonEnvReads
import lib.model.Category
import play.api.libs.json.{Json, Reads}

case class JsValueTakeCategory(

  name: String,

  slug: String,

  color_code : Short,

)



object JsValueTakeCategory extends JsonEnvReads {

  implicit val reads: Reads[JsValueTakeCategory] = Json.reads[JsValueTakeCategory]

}
