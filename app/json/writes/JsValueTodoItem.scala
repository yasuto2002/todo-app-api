package json.writes

import play.api.libs.json.{Json, Writes}

case class JsValueTodoItem(
  id:           Long,
  title:        String,
  body:         String,
  state_id:     Short,
  category_id:  Long
)

object JsValueTodoItem{
  implicit val writes: Writes[JsValueTodoItem] = Json.writes[JsValueTodoItem]

  def apply(todo: lib.model.Todo.EmbeddedId): JsValueTodoItem =
    JsValueTodoItem(
      id          = todo.id,
      title       = todo.v.title,
      body        = todo.v.body,
      state_id    = todo.v.state.code,
      category_id = todo.v.category_id
    )
}