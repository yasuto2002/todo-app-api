package controllers

import ixias.model.tag
import json.reads.JsValueCreateTodo
import lib.model.{Category, Todo}
import play.api.mvc.{AbstractController, ControllerComponents, MessagesActionBuilder}

import javax.inject.{Inject, Singleton}
import model.ViewValueTodoList
import lib.persistence.onMySQL.TodoRepository
import play.api.mvc.{AnyContent, MessagesRequest}
import lib.persistence.onMySQL.CategoryRepository

import scala.concurrent.{ExecutionContext, Future}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints.{maxLength, nonEmpty, pattern}
import json.writes.{JsValueErrorResponseItem, JsValueTodoListItem}
import play.api.libs.json.Json

import java.sql.{SQLException, SQLTransientConnectionException}
@Singleton
class TodoController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
(implicit executionContext: ExecutionContext)extends AbstractController(components){

  val todoForm: Form[Todo.WithNoId] = Form(
    mapping(
      "category" -> longNumber.transform[Category.Id]({id:Long => tag[Category][Long](id)},{categoryId:Category.Id => categoryId.toLong }),
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
      val todosJson = todos.map(todo => JsValueTodoListItem.apply(todo))
      Ok(Json.toJson(todosJson))
    }).recover {
      case e: SQLException => InternalServerError(Json.toJson(JsValueErrorResponseItem.apply(500,e.getMessage)))
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
      .validate[JsValueCreateTodo]
      .fold(
        errors => {
          Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem.apply(404,"Json Parse Error"))))
        },
        todoData => {
          CategoryRepository.get(tag[Category][Long](todoData.category_id)).flatMap(category => category
             match {
              case Some(category) =>
                val todo = Todo(category.id,todoData.title,todoData.body,Todo.Status(todoData.state_id))
                TodoRepository.add(todo).map(_ => Ok("success"))
              case None => {
                val error = JsValueErrorResponseItem.apply(code = 404, message = "category is incorrect")
                Future.successful(BadRequest(Json.toJson(error)))
              }
            }
          ).recover {
            case e: SQLException => InternalServerError(Json.toJson(JsValueErrorResponseItem.apply(500, e.getMessage)))
          }
        }
      )
  }

  def edit(todoId: Long) = messagesAction.async { implicit req =>
    val vv = ViewValueTodoList(
      title = "Todo編集",
      cssSrc = Seq("main.css", "todoForm.css"),
      jsSrc = Seq("main.js")
    )
    CategoryRepository.all().flatMap { categories =>
      TodoRepository.get(tag[Todo][Long](todoId)).map(todo => {
        todo match {
          case Some(value) => {
            val filledForm = todoForm.fill(Todo(value.v.category_id, value.v.title, value.v.body, value.v.state))
            Ok(views.html.Todo.Edit(vv)(filledForm)(categories)(todoId))
          }
          case None => BadRequest(views.html.Todo.Create(vv)(todoForm.withError("category","Invalid value"))(categories))
        }
      })
    }
  }

  def update(todoId: Long) = messagesAction.async { implicit req =>

    val vv = ViewValueTodoList(
      title = "Todo編集",
      cssSrc = Seq("main.css", "todoForm.css"),
      jsSrc = Seq("main.js")
    )
    todoForm.bindFromRequest().fold(
      formWithErrors => {
        CategoryRepository.all().map(categories => {
          BadRequest(views.html.Todo.Edit(vv)(formWithErrors)(categories)(todoId))
        })
      },
      todoReq => {
        for{
          todoCheck <- TodoRepository.get(tag[Todo][Long](todoId)) // id確認
          categoryCheck <- CategoryRepository.get(todoReq.v.category_id) // category確認
          result <- (todoCheck, categoryCheck) match {
            case (Some(todo), Some(_)) => {
              // 更新
              val copyTodo: Todo.EmbeddedId = todo.v.copy(category_id = todoReq.v.category_id, state = todoReq.v.state, title = todoReq.v.title, body = todoReq.v.body).toEmbeddedId
              TodoRepository.update(copyTodo).map(_.fold{InternalServerError("Server Error")}{_ => Redirect(routes.TodoController.index())})
            }
            case (Some(_),None) => {
              Future.successful(BadRequest("The specified category does not exist"))
            }
            case (_, _) => {
               Future.successful(BadRequest("The specified todo does not exist."))
            }
          }
        }yield result
      }
    )
  }

  def delete(todoId: Long) = messagesAction.async { implicit req =>
    for{
      todoCheck <- TodoRepository.get(tag[Todo][Long](todoId))
      result <- todoCheck match {
        case Some(todo) => {
          TodoRepository.remove(todo.id).map(_.fold {
            InternalServerError("Server Error")
          } { _ => Redirect(routes.TodoController.index()) })
        }
        case _ => {
          Future.successful(NotFound("Invalid value"))
        }
      }
    }yield result
  }
}