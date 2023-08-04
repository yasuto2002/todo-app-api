package json.reads

import play.api.libs.json.{Json, Reads}

case class JsValueCreateTodo(
  title:      String,
  body:       String,
  state_id : Short,
  category_id: Long,
)

object JsValueCreateTodo {
  implicit val reads: Reads[JsValueCreateTodo] = Json.reads[JsValueCreateTodo]
}
