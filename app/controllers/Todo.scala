package controllers

import play.api.mvc.{AbstractController, ControllerComponents, MessagesActionBuilder}
import javax.inject.{Inject, Singleton}
import model.ViewValueTodoList
import lib.model.{Category}
import lib.persistence.onMySQL.TodoRepository
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@Singleton
class TodoController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
  extends AbstractController(components) {
  def index() = Action { implicit req =>
    val vv = ViewValueTodoList(
      title = "TODOリスト",
      cssSrc = Seq("main.css","todoList.css"),
      jsSrc = Seq("main.js")
    )
    val todos:Seq[(lib.model.Todo,Category)] = Await.result(TodoRepository.all(), Duration.Inf)
    Ok(views.html.Todo.List(vv)(todos))
  }
}