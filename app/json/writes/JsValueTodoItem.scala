package json.writes

import ixias.util.json.JsonEnvWrites
import lib.model.{Category, Todo}
import play.api.libs.json.{JsNumber, JsValue, Json, Writes}

case class JsValueTodoItem(
  id:           Todo.Id,
  title:        String,
  body:         String,
  state_code:   Todo.Status,
  category_id:  Category.Id
)

object JsValueTodoItem extends JsonEnvWrites {

  implicit val todoIdWrites: Writes[Todo.Id] = new Writes[Todo.Id] {
    def writes(tag: Todo.Id) = JsNumber(tag)
  }

  implicit val categoryIdWrites: Writes[Category.Id] = new Writes[Category.Id] {
    def writes(tag: Category.Id) = JsNumber(tag)
  }

  implicit val statusWrites: Writes[Todo.Status] = new Writes[Todo.Status] {
    def writes(st: Todo.Status): JsValue = EnumStatusWrites.writes(st)
  }

  implicit val writes: Writes[JsValueTodoItem] = Json.writes[JsValueTodoItem]

  def apply(todo: lib.model.Todo.EmbeddedId): JsValueTodoItem =
    JsValueTodoItem(
      id          = todo.id,
      title       = todo.v.title,
      body        = todo.v.body,
      state_code  = todo.v.state,
      category_id = todo.v.category_id
    )
}