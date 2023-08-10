package json.writes

import ixias.util.json.JsonEnvWrites
import lib.model
import lib.model.Category.Color
import lib.model.{Category, Todo}
import play.api.libs.json.{JsNumber, JsValue, Json, Writes}

case class JsValueCategoryListItem(
  id:         Category.Id,
  name:       String,
  slug:       String,
  color_code: Category.Color,
)

object JsValueCategoryListItem extends JsonEnvWrites {

  implicit val categoryIdWrites: Writes[Category.Id] = new Writes[Category.Id] {
    def writes(tag: Category.Id) = JsNumber(tag)
  }

  implicit val colorWrites: Writes[Category.Color] = new Writes[Category.Color] {
    def writes(color: Category.Color): JsValue = EnumStatusWrites.writes(color)
  }

  implicit val writes: Writes[JsValueCategoryListItem] = Json.writes[JsValueCategoryListItem]

  def apply(category: lib.model.Category.EmbeddedId): JsValueCategoryListItem =
    JsValueCategoryListItem(
      id          = category.id,
      name        = category.v.name,
      slug        = category.v.slug,
      color_code  = category.v.color,
    )
  }