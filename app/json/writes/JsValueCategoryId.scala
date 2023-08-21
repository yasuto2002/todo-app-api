package json.writes

import ixias.util.json.JsonEnvWrites
import lib.model.Category
import play.api.libs.json.{JsNumber, Json, Writes}

case class JsValueCategoryId(
    id:           Category.Id,
)

object JsValueCategoryId extends JsonEnvWrites {

  implicit val categoryIdWrites: Writes[Category.Id] = new Writes[Category.Id] {
    def writes(tag: Category.Id) = JsNumber(tag)
  }

  implicit val writes: Writes[JsValueCategoryId] = Json.writes[JsValueCategoryId]
}