package model

case class ViewValueTodoList(
  title:  String,
  cssSrc: Seq[String],
  jsSrc:  Seq[String],
) extends ViewValueCommon

case class TodoForm(category:lib.model.Category.Id,title:String,body:String,State:lib.model.Todo.Status)