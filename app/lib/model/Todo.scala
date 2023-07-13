package lib.model

import ixias.model.{@@, Entity, EntityModel, Identity, NOW, the}
import ixias.util.EnumStatus
import java.time.LocalDateTime
import Todo._

case class Todo(
   id :         Option[Id],
   category_id: Category.Id,
   title:       String,
   body:        String,
   state:       Status,
   updatedAt:   LocalDateTime = NOW,
   createdAt:   LocalDateTime = NOW
) extends EntityModel[Id]

object Todo {
  val   Id = the[Identity[Id]]
  type  Id = Long @@ Todo
  type  WithNoId = Entity.WithNoId[Id,Todo]
  type  EmbeddedId = Entity.EmbeddedId[Id,Todo]

  sealed abstract class Status(val code:Short,val name:String) extends EnumStatus
  object Status extends EnumStatus.Of[Status]{

    case object IS_INACTIVE extends Status(code = 0, name = "未着手")

    case object IS_ACTIVE extends Status(code = 1, name = "進行中")

    case object ACTIVE extends Status(code = 2, name = "完了")
  }

  def apply(category_id:Category.Id,title:String,body:String,state:Status):WithNoId ={
    new Entity.WithNoId(
      new Todo(
        id = None,
        category_id = category_id,
        title = title,
        body = body,
        state = state
      )
    )
  }
  def unapply(todo: Todo.WithNoId): Option[(Category.Id, String, String, Status)] = {
    Some((todo.v.category_id, todo.v.title, todo.v.body, todo.v.state))
  }

  def build( todo:Todo ):Todo#EmbeddedId = {
    new Entity.EmbeddedId(todo)
  }

}