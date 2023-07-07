package model

import lib.model.{Category, Todo}

case class ViewValueTodoList(
  title:  String,
  cssSrc: Seq[String],
  jsSrc:  Seq[String],
) extends ViewValueCommon

case class TodoForm(category:Category.Id,title:String,body:String,State:Todo.Status)