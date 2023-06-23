package controllers
import ixias.persistence.backend.SlickBackend
import play.api.mvc.{AbstractController, AnyContent, BaseController, ControllerComponents, MessagesActionBuilder, MessagesRequest, MessagesRequestHeader}

import javax.inject.{Inject, Singleton}
import model.ViewValueTodoList
import slick.jdbc.JdbcBackend.Database
import lib.model.{Category}
import lib.persistence.onMySQL.TodoRepository
import lib.persistence.onMySQL.TodoRepository.EntityEmbeddedId
import lib.persistence.onMySQL.UserRepository

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.data.Form
import play.api.data.Forms._
import model.Todo
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