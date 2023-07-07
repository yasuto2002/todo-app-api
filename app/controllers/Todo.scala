package controllers

import ixias.model.tag
import lib.model.{Category, Todo}
import play.api.mvc.{AbstractController, ControllerComponents, MessagesActionBuilder}

import javax.inject.{Inject, Singleton}
import model.{TodoForm, ViewValueTodoList}
import lib.persistence.onMySQL.TodoRepository
import play.api.mvc.{AnyContent, MessagesRequest}
import lib.persistence.onMySQL.CategoryRepository.EntityEmbeddedId
import lib.persistence.onMySQL.CategoryRepository

import scala.concurrent.ExecutionContext
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints.{maxLength, nonEmpty}
@Singleton
class TodoController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
(implicit executionContext: ExecutionContext)extends AbstractController(components){

  val todoForm: Form[TodoForm] = Form(
    mapping(
      "category" -> longNumber.transform[Category.Id]({id:Long => tag[Category][Long](id)},{categoryId:Category.Id => categoryId.toLong }),
      "title" -> text.verifying(nonEmpty).verifying(maxLength(255)),
      "body" -> text.verifying(nonEmpty).verifying(maxLength(255)),
      "state" -> shortNumber.transform[lib.model.Todo.Status]({Todo.Status(_)},{_.code})
    )(TodoForm.apply)(TodoForm.unapply)
      .verifying(
      "Incorrect status!",
      fields => lib.model.Todo.Status.values.contains(fields.State)
    )
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
      cssSrc = Seq("main.css", "todoCreate.css"),
      jsSrc = Seq("main.js")
    )

    CategoryRepository.all().map(categories => {
      Ok(views.html.Todo.Create(vv)(todoForm)(categories))
    })
  }

  def store = messagesAction.async { implicit req =>
    val vv = ViewValueTodoList(
      title = "TODO追加",
      cssSrc = Seq("main.css", "todoCreate.css"),
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
            category.id.equals(todo.category)
          }) match {
            case Some(category) =>
              val todoWithNoId: lib.model.Todo.WithNoId = lib.model.Todo(category_id = category.id, title = todo.title, body = todo.body, state = todo.State)
              TodoRepository.add(todoWithNoId).map(_ => Redirect(routes.TodoController.index()))
            case None => CategoryRepository.all().map(categories => {
              BadRequest(views.html.Todo.Create(vv)(todoForm.withError("category","Invalid value"))(categories))
            })
          }
        })
      }
    )
  }
}