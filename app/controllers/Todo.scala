package controllers
import play.api.mvc.{BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import model.ViewValueHome
@Singleton
class TodoController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def index() = Action { implicit req =>
    val vv = ViewValueHome(
      title = "TODOリスト",
      cssSrc = Seq("main.css"),
      jsSrc = Seq("main.js")
    )
    Ok(views.html.Todo.List(vv))
  }
}