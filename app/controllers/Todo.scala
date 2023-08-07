package controllers

import ixias.model.tag
import json.reads.JsValueTakeTodo
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
import json.writes.{JsValueCategoryListItem, JsValueEditItem, JsValueErrorResponseItem, JsValueTodoItem, JsValueTodoListItem}
import play.api.libs.json.Json

import java.sql.{SQLException, SQLTransientConnectionException}
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
      val todosJson = todos.map(todo => JsValueTodoListItem(todo))
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
          Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(404,"Json Parse Error"))))
        },
        todoData => {
          CategoryRepository.get(Category.Id(todoData.category_id)).flatMap(category => category
             match {
              case Some(category) =>
                val todo = Todo(category.id,todoData.title,todoData.body,Todo.Status(todoData.state_id))
                TodoRepository.add(todo).map(_ => Ok("success"))
              case None => {
                val error = JsValueErrorResponseItem(code = 404, message = "category is incorrect")
                Future.successful(BadRequest(Json.toJson(error)))
              }
            }
          ).recover {
            case e: SQLException => InternalServerError(Json.toJson(JsValueErrorResponseItem(500, e.getMessage)))
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
      TodoRepository.get(Todo.Id(todoId)).map(oTodo => {
        oTodo match {
          case Some(todo) => {
            val todoJson = JsValueTodoItem(todo)
            val categoriesJson = categories.map(category => JsValueCategoryListItem(category))
            val editInfoJson = JsValueEditItem(categoriesJson,todoJson)
            Ok(Json.toJson(editInfoJson))
          }
          case None => {
            val error = JsValueErrorResponseItem(code = 404, message = "I couldn't find todo.")
            BadRequest(Json.toJson(error))
          }
        }
      })
    }
  }

  def update(todoId: Long) = Action(parse.json).async { implicit req =>

    val vv = ViewValueTodoList(
      title = "Todo編集",
      cssSrc = Seq("main.css", "todoForm.css"),
      jsSrc = Seq("main.js")
    )
    req.body
      .validate[JsValueTakeTodo]
      .fold(
        errors => {
          Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(404,"Json Parse Error"))))
        },
        todoData => {
            for{
              todoCheck <- TodoRepository.get(Todo.Id(todoId)) // id確認
              categoryCheck <- CategoryRepository.get(Category.Id(todoId)) // category確認
              result <- (todoCheck, categoryCheck) match {
                case (Some(todo), Some(category)) => {
                  // 更新
                  val copyTodo: Todo.EmbeddedId = todo.v.copy(category_id = category.id, state = Todo.Status(todoData.state_id), title = todoData.title, body = todoData.body).toEmbeddedId
                  TodoRepository.update(copyTodo).map(_.fold{InternalServerError(Json.toJson(JsValueErrorResponseItem(500, "server error")))}{_ => Ok})
                }
                case (Some(_),None) => {
                  Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(404, "The specified category does not exist"))))
                }
                case (_, _) => {
                   Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(404, "The specified todo does not exist."))))
                }
              }
            }yield result
        }
      )
  }

  def delete(todoId: Long) = messagesAction.async { implicit req =>
    for{
      todoCheck <- TodoRepository.get(Todo.Id(todoId))
      result <- todoCheck match {
        case Some(todo) => {
          TodoRepository.remove(todo.id).map(_.fold {
            InternalServerError(Json.toJson(JsValueErrorResponseItem(500, "server error")))
          } { _ => Ok })
        }
        case _ => {
          Future.successful(NotFound(Json.toJson(JsValueErrorResponseItem(404, "There was no todo."))))
        }
      }
    }yield result
  }
}