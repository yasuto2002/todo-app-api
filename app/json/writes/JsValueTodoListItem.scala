package json.writes

import play.api.libs.json.{JsValue,JsNumber, Json, Writes}
import ixias.util.json.JsonEnvWrites
import lib.model.{Category, Todo}
case class JsValueTodoListItem(
  id:             Todo.Id,
  title:          String,
  body:           String,
  state_code:     Todo.Status,
  category:       JsValueCategoryListItem
)

object JsValueTodoListItem extends JsonEnvWrites {

  implicit val todoIdWrites: Writes[Todo.Id] = new Writes[Todo.Id] {
    def writes(tag: Todo.Id) = JsNumber(tag)
  }

  implicit val statusWrites: Writes[Todo.Status] = new Writes[Todo.Status] {
    def writes(st: Todo.Status): JsValue = EnumStatusWrites.writes(st)
  }

  implicit val writes: Writes[JsValueTodoListItem] = Json.writes[JsValueTodoListItem]
  def apply(todo: Todo.EmbeddedId, category: Category.EmbeddedId): JsValueTodoListItem =
    JsValueTodoListItem(
      id              = todo.id,
      title           = todo.v.title,
      body            = todo.v.body,
      state_code      = todo.v.state,
      category        = JsValueCategoryListItem(category)
    )
}