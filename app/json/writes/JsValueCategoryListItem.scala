package json.writes

import play.api.libs.json.{Json, Writes}

case class JsValueCategoryListItem(
  id:       Long,
  name:    String,
  slug:     String,
  color_id:    Short,
)

object JsValueCategoryListItem{
  implicit val writes: Writes[JsValueCategoryListItem] = Json.writes[JsValueCategoryListItem]

  def apply(category: lib.model.Category.EmbeddedId): JsValueCategoryListItem =
    JsValueCategoryListItem(
      id = category.id,
      name = category.v.name,
      slug = category.v.slug,
      color_id = category.v.color.code,
    )
  }