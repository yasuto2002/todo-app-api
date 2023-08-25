package controllers

import akka.util.ByteString
import json.reads.JsValueTakeTodo
import lib.model.{Category, Todo}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, MessagesActionBuilder, MessagesRequest, ResponseHeader, Result}

import javax.inject.{Inject, Singleton}
import model.ViewValueTodoList
import lib.persistence.onMySQL.TodoRepository
import lib.persistence.onMySQL.CategoryRepository

import scala.concurrent.{ExecutionContext, Future}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints.{maxLength, nonEmpty, pattern}
import json.writes.{JsValueErrorResponseItem, JsValueTodoId, JsValueTodoItem, JsValueTodoListItem}
import play.api.http.HttpEntity
import play.api.libs.json.Json

import java.sql.{SQLException}
import cats.syntax.all._
import cats.data.OptionT
import cats.data.Nested

@Singleton
class TodoController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
(implicit executionContext: ExecutionContext)extends AbstractController(components){

  val todoForm: Form[Todo.WithNoId] = Form(
    mapping(
      "category" -> longNumber.transform[Category.Id]({id:Long => Category.Id(id)},{categoryId:Category.Id => categoryId.toLong }),
      "title" -> text.verifying(nonEmpty).verifying(maxLength(255)).verifying(pattern(("^[0-9a-zA-Zぁ-んーァ-ンヴー一-龠]*$").r,error = "英数字・日本語のみ入力可")),
      "body" -> text.verifying(nonEmpty).verifying(maxLength(255)).verifying(pattern(("^[0-9a-zA-Zぁ-んーァ-ンヴー一-龠\\s]*$").r,error = "英数字・日本語・改行のみ入力可")),
      "state" -> shortNumber.transform[lib.model.Todo.Status]({Todo.Status(_)},{_.code}),
    )(Todo.apply)(Todo.unapply)
      .verifying(
      "Incorrect status!",
      fields => lib.model.Todo.Status.values.contains(fields.v.state)
    )
  )

  def index() = Action.async { implicit req =>
    val vv = ViewValueTodoList(
      title = "TODOリスト",
      cssSrc = Seq("main.css","todoList.css"),
      jsSrc = Seq("main.js")
    )
    TodoRepository.all().map(todos => {
      val todosJson = todos.map { case (todo, category) => JsValueTodoListItem(todo, category) }
      Ok(Json.toJson(todosJson))
    }).recover {
      case e: SQLException => InternalServerError(Json.toJson(JsValueErrorResponseItem(500,e.getMessage)))
    }
  }

  def create() = messagesAction.async { implicit request: MessagesRequest[AnyContent] =>
    val vv = ViewValueTodoList(
      title = "TODOリスト",
      cssSrc = Seq("main.css", "todoForm.css"),
      jsSrc = Seq("main.js")
    )

    CategoryRepository.all().map(categories => {
      Ok(views.html.Todo.Create(vv)(todoForm)(categories))
    })
  }

  def store = Action(parse.json).async { implicit req =>
    req.body
      .validate[JsValueTakeTodo]
      .fold(
        errors => {
          Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(400,"Json Parse Error"))))
        },
        todoData => {
          CategoryRepository.get(todoData.category_id).flatMap {
            case Some(category) =>
              val todoNoId = Todo(category.id, todoData.title, todoData.body, Todo.Status(todoData.state_code))
              TodoRepository.add(todoNoId).map(id => {
                val jsonId = JsValueTodoId(id)
                Result(
                  header = ResponseHeader(201, Map("Location" -> routes.TodoController.edit(id.toLong).url)),
                  body = HttpEntity.Strict(ByteString(Json.toBytes(Json.toJson(jsonId))) ,None)
                )
              })
            case None => {
              val error = JsValueErrorResponseItem(code = 400, message = "category is incorrect")
              Future.successful(BadRequest(Json.toJson(error)))
            }
          }.recover {
            case e: SQLException => InternalServerError(Json.toJson(JsValueErrorResponseItem(500, e.getMessage)))
          }
        }
      )
  }

  def edit(todoId: Long) = messagesAction.async { implicit req =>
    OptionT(TodoRepository.get(Todo.Id(todoId))).map(todo => {
          val todoJson = JsValueTodoItem(todo)
          Ok(Json.toJson(todoJson))
    })
      .getOrElse{NotFound(Json.toJson(JsValueErrorResponseItem(code = 404, message = "I couldn't find todo.")))}
  }

  def update(todoId: Long) = Action(parse.json).async { implicit req =>
    req.body
      .validate[JsValueTakeTodo]
      .fold(
        errors => {
          Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(400,"Json Parse Error"))))
        },
        todoData => {
          (TodoRepository.get(Todo.Id(todoId)), CategoryRepository.get(todoData.category_id)).mapN[Future[Result]]{
            (todoCheck, categoryCheck) => (todoCheck, categoryCheck) match {
                case (Some(todo), Some(category)) => {
                  // 更新
                  val copyTodo: Todo.EmbeddedId = todo.v.copy(category_id = category.id, state = Todo.Status(todoData.state_code), title = todoData.title, body = todoData.body).toEmbeddedId
                  OptionT(TodoRepository.update(copyTodo)).fold{InternalServerError(Json.toJson(JsValueErrorResponseItem(500, "server error")))}{_ => Ok(Json.toJson(JsValueTodoItem(copyTodo)))}
                }
                case (Some(_),None) => {
                  Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(404, "The specified category does not exist"))))
                }
                case (_, _) => {
                   Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(404, "The specified todo does not exist."))))
                }
              }
          }.flatten
        }
      )
  }

  def delete(todoId: Long) = Action.async { implicit req =>

    OptionT(TodoRepository.get(Todo.Id(todoId))).map(todo => {
      TodoRepository.remove(todo.id).map(_.fold {
          InternalServerError(Json.toJson(JsValueErrorResponseItem(500, "server error")))
        } { _ => Ok })
    })
    .getOrElse {
      Future.successful(NotFound(Json.toJson(JsValueErrorResponseItem(404, "There was no todo."))))
    }.flatten
  }
}