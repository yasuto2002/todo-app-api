package model

case class ViewValueTodoList(
  title:  String,
  cssSrc: Seq[String],
  jsSrc:  Seq[String],
) extends ViewValueCommon

case class Todo(category:Long,title:String,body:String,State:Short)// Form