package model

case class ViewValueTodoList(
  title:  String,
  cssSrc: Seq[String],
  jsSrc:  Seq[String],
) extends ViewValueCommon