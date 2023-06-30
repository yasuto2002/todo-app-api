package controllers

import lib.model.Category
import play.api.mvc.{AbstractController, ControllerComponents, MessagesActionBuilder}

import javax.inject.{Inject, Singleton}
import model.ViewValueTodoList
import lib.persistence.onMySQL.TodoRepository
import play.api.mvc.{AnyContent, MessagesRequest}
import lib.persistence.onMySQL.CategoryRepository.EntityEmbeddedId
import lib.persistence.onMySQL.CategoryRepository

import scala.concurrent.{ExecutionContext, Future}
import play.api.data.Form
import play.api.data.Forms._
import model.Todo
import play.api.data.validation.Constraints
import play.api.data.validation.Constraints.{maxLength, nonEmpty}
@Singleton
class TodoController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
(implicit executionContext: ExecutionContext)extends AbstractController(components) {

  val todoForm: Form[Todo] = Form(
    mapping(
      "category_id" -> longNumber,
      "title" -> text.verifying(nonEmpty).verifying(maxLength(255)),
      "body" -> text.verifying(nonEmpty).verifying(maxLength(255)),
      "state" -> shortNumber
    )(Todo.apply)(Todo.unapply)
      .verifying(
      "Failed form constraints!",
      fields =>
        fields match {
          case todoData => lib.model.Todo.Status.values.find(st => st.code.equals(todoData.State)).isDefined
        }
    )
  )

  val error = ViewValueTodoList(
    title = "エラー",
    cssSrc = Seq("main.css"),
    jsSrc = Seq("main.js")
  )

  def index() = Action.async { implicit req =>
    val vv = ViewValueTodoList(
      title = "TODOリスト",
      cssSrc = Seq("main.css","todoList.css"),
      jsSrc = Seq("main.js")
    )
    TodoRepository.all().map(todos => {
      Ok(views.html.Todo.List(vv)(todos))
    })
  }

  def create() = messagesAction.async { implicit request: MessagesRequest[AnyContent] =>
    val vv = ViewValueTodoList(
      title = "TODOリスト",
      cssSrc = Seq("main.css", "create.css"),
      jsSrc = Seq("main.js")
    )

    CategoryRepository.all().map(categories => {
      Ok(views.html.Todo.Create(vv)(todoForm)(categories))
    })
  }

  def store = messagesAction.async { implicit req =>
    val vv = ViewValueTodoList(
      title = "TODO追加",
      cssSrc = Seq("main.css", "create.css"),
      jsSrc = Seq("main.js")
    )
    todoForm.bindFromRequest().fold(
      formWithErrors => {
        CategoryRepository.all().map(categories => {
          BadRequest(views.html.Todo.Create(vv)(formWithErrors)(categories))
        })
      },
      todo => {
        CategoryRepository.all().flatMap(categories => {
          categories.find((category:EntityEmbeddedId) => {
            category.v.id.get == todo.category_id // categoryがEntityEmbeddedIdなのでidがSomeであることが保証されている
          }) match {
            // カテゴリーが存在するか確認する
            case Some(category) =>
              val state = lib.model.Todo.Status.values.find(st => st.code.equals(todo.State))
              val todoWithNoId: lib.model.Todo.WithNoId = lib.model.Todo.apply(category_id = category.id, title = todo.title, body = todo.body, state = state.get)
              TodoRepository.add(todoWithNoId).map(_ => Redirect(routes.TodoController.index()))
            case None => CategoryRepository.all().map(categories => {
              BadRequest(views.html.Todo.Create(vv)(todoForm.withError("category_id","Invalid value"))(categories))
            })
          }
        })
      }
    )
  }
}