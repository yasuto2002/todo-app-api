package controllers

import play.api.mvc.{AbstractController, ControllerComponents, MessagesActionBuilder}

import javax.inject.{Inject, Singleton}
import model.ViewValueTodoList
import lib.persistence.onMySQL.TodoRepository

import scala.concurrent.ExecutionContext
@Singleton
class TodoController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
(implicit executionContext: ExecutionContext)extends AbstractController(components) {
  def index = Action.async { implicit req =>
    val vv = ViewValueTodoList(
      title = "TODOリスト",
      cssSrc = Seq("main.css","todoList.css"),
      jsSrc = Seq("main.js")
    )
    TodoRepository.all().map(todos => {
      Ok(views.html.Todo.List(vv)(todos))
    })
  }
}