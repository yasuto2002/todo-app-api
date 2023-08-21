package controllers

import akka.util.ByteString
import json.reads.{JsValueTakeCategory}
import json.writes.{JsValueCategoryId, JsValueCategoryListItem, JsValueErrorResponseItem}
import lib.model.Category
import lib.model.Category.Color
import lib.persistence.onMySQL.{CategoryRepository}
import play.api.data.Form
import play.api.data.Forms.{mapping, shortNumber, text}
import play.api.data.validation.Constraints.{maxLength, nonEmpty, pattern}
import play.api.http.HttpEntity
import play.api.libs.json.Json

import javax.inject._
import play.api.mvc._

import java.sql.SQLException
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CategoryController @Inject()(messagesAction: MessagesActionBuilder, components: ControllerComponents)
                                  (implicit executionContext: ExecutionContext)extends AbstractController(components){
  val categoryForm:Form[Category.WithNoId] = Form(
    mapping(
      "name"  -> text.verifying(nonEmpty).verifying(maxLength(255)).verifying(pattern(("^[0-9a-zA-Zぁ-んーァ-ンヴー一-龠]*$").r,error = "英数字・日本語のみ入力できます")),
      "slug"  -> text.verifying(nonEmpty).verifying(maxLength(64)).verifying(pattern(("^[0-9a-zA-Z]*$").r,error = "英数字のみ入力できます")),
      "color" -> shortNumber.transform[Color](Color(_),_.code)
    )(Category.apply)(Category.unapply)
      .verifying(
        "Incorrect status!",
        fields => Category.Color.values.contains(fields.v.color)
      )
  )

  def index() = Action.async { implicit req =>
    val vv = model.ViewValueCategory(
      title  = "Category一覧",
      cssSrc = Seq("main.css","categoryList.css"),
      jsSrc  = Seq("main.js")
    )
    CategoryRepository.all().map(categories => {
      val categoryJson = categories.map(category => JsValueCategoryListItem(category))
      Ok(Json.toJson(categoryJson))
    }).recover {
      case e: SQLException => InternalServerError(Json.toJson(JsValueErrorResponseItem(500, e.getMessage)))
    }
  }

  def create() = messagesAction { implicit req =>
    val vv = model.ViewValueCategory(
      title = "Category追加",
      cssSrc = Seq("main.css", "categoryForm.css"),
      jsSrc = Seq("main.js")
    )
    Ok(views.html.Category.Create(vv)(categoryForm))
  }

  def store() = Action(parse.json).async { implicit req =>
    req.body
      .validate[JsValueTakeCategory]
      .fold(
        errors => {
          Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(404, "Json Parse Error"))))
        },
      categoryData => {
        val categoryNoId:Category.WithNoId = Category(categoryData.name,categoryData.slug,Category.Color(categoryData.color_code))
        CategoryRepository.add(categoryNoId).map(categoryId => {
          val jsonId = JsValueCategoryId(categoryId)
          Result(
            header = ResponseHeader(201, Map("Location" -> routes.CategoryController.edit(categoryId.toLong).url)),
            body = HttpEntity.Strict(ByteString(Json.toBytes(Json.toJson(jsonId))), None)
          )
        }).recover {
          case e: SQLException => InternalServerError(Json.toJson(JsValueErrorResponseItem(500, e.getMessage)))
        }
      }
    )
  }

  def edit(categoryId:Long) = Action.async { implicit req =>

    CategoryRepository.get(Category.Id(categoryId)).map{ value =>
      value.fold(InternalServerError(Json.toJson(JsValueErrorResponseItem(500, "I couldn't find the category."))))
      {category =>
        val categoryJson = JsValueCategoryListItem(category)
        Ok(Json.toJson(categoryJson))
      }
    }
  }

  def update(categoryId:Long) = Action(parse.json).async { implicit req =>

    req.body
      .validate[JsValueTakeCategory]
      .fold(
        errors => {
          Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(404, "Json Parse Error"))))
        },
      categoryData => {
        CategoryRepository.get(Category.Id(categoryId)).flatMap{ _ match {
          case Some(category) => {
            val copyCategory = category.v.copy(name = categoryData.name,slug = categoryData.slug,color = Category.Color(categoryData.color_code)).toEmbeddedId
            CategoryRepository.update(copyCategory).map{_.fold{InternalServerError(Json.toJson(JsValueErrorResponseItem(500, "server error")))}{category => Ok(Json.toJson(JsValueCategoryListItem(copyCategory)))}}
          }
          case None => {
            Future.successful(BadRequest(Json.toJson(JsValueErrorResponseItem(400,"The specified category does not exist"))))
          }
        }}
      }
    )
  }

  def delete(categoryId:Long) = Action.async { implicit req =>
    CategoryRepository.get(Category.Id(categoryId)).flatMap(_ match {
      case Some(category) => {
          CategoryRepository.cascadeDelete(category.id).map(_.fold{InternalServerError(Json.toJson(JsValueErrorResponseItem(500, "server error")))}{_ => Ok})
      }
      case None => {
        Future.successful(NotFound(Json.toJson(JsValueErrorResponseItem(404, "There was no todo."))))
      }
    })
  }
}
