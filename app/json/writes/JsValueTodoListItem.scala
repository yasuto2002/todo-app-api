package json.writes

import play.api.libs.json.{Json, Writes}

case class JsValueTodoListItem(
  id:             Long,
  title:          String,
  body:           String,
  state:          Short,
  category_name:  String
)

object JsValueTodoListItem{
  implicit val writes: Writes[JsValueTodoListItem] = Json.writes[JsValueTodoListItem]

  def apply(todo:(lib.model.Todo.EmbeddedId,lib.model.Category.EmbeddedId)): JsValueTodoListItem =
    JsValueTodoListItem(
      id              = todo._1.id,
      title           = todo._1.v.title,
      body            = todo._1.v.body,
      state           = todo._1.v.state.code,
      category_name   = todo._2.v.name
    )
}