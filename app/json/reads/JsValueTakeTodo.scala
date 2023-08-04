package json.reads

import play.api.libs.json.{Json, Reads}

case class JsValueTakeTodo(
  title:      String,
  body:       String,
  state_id : Short,
  category_id: Long,
)

object JsValueTakeTodo {
  implicit val reads: Reads[JsValueTakeTodo] = Json.reads[JsValueTakeTodo]
}
