package json.writes

import ixias.util.json.JsonEnvWrites
import lib.model.{Category, Todo}
import play.api.libs.json.{JsNumber, JsValue, Json, Writes}

case class JsValueTodoId(
  id:           Todo.Id,
)

object JsValueTodoId extends JsonEnvWrites {

  implicit val todoIdWrites: Writes[Todo.Id] = new Writes[Todo.Id] {
    def writes(tag: Todo.Id) = JsNumber(tag)
  }

  implicit val writes: Writes[JsValueTodoId] = Json.writes[JsValueTodoId]
}