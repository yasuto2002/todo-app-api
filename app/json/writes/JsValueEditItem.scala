package json.writes

import play.api.libs.json.{Json, Writes}


case class JsValueEditItem(
  categories: Seq[JsValueCategoryListItem],
  todo:  JsValueTodoItem,
)

object JsValueEditItem{
  implicit val writes: Writes[JsValueEditItem] = Json.writes[JsValueEditItem]
}