package lib.persistence.db

import akka.http.scaladsl.model.headers.LinkParams.title
import ixias.persistence.model.{DataSourceName, Table}
import lib.model.Todo
import lib.model.Todo.{Id, Status}
import slick.jdbc.JdbcProfile
import slick.lifted.ProvenShape

import java.time.LocalDateTime

case class TodoTable[P <: JdbcProfile]()(implicit val driver: P)extends Table[Todo,P]{
  import api._

  override val dsn = Map(
    "master" -> DataSourceName("ixias.db.mysql://master/to_do"),
    "slave" -> DataSourceName("ixias.db.mysql://slave/to_do")
  )
  class Query extends BasicQuery(new Table(_)) {}
  lazy val query = new Query

  class Table(tag: Tag) extends BasicTable(tag,"to_do"){
    import Todo._

    // Columns
    /* @1 */ def id = column[Id]("id", O.UInt64, O.PrimaryKey, O.AutoInc)

    /* @2 */ def category_id = column[lib.model.Category.Id]("category_id", O.UInt64)

    /* @3 */ def title = column[String]("title", O.Utf8Char255)

    /* @4 */ def body = column[String]("body", O.Text)

    /* @5 */ def state = column[Status]("state", O.UInt8)

    /* @6 */ def updatedAt = column[LocalDateTime]("updated_at", O.TsCurrent)

    /* @7 */ def createdAt = column[LocalDateTime]("created_at", O.Ts)

    type TableElementTuple = (
      Option[Id], lib.model.Category.Id,String, String, Status, LocalDateTime, LocalDateTime
    )
    override def *  = (id.?,category_id,title,body,state,updatedAt,createdAt)<>(
      (t: TableElementTuple) => Todo(
        t._1, t._2, t._3, t._4, t._5, t._6,t._7
      ),
      (v: TableElementType) => Todo.unapply(v).map{t => (
        t._1, t._2, t._3, t._4, t._5, LocalDateTime.now(),LocalDateTime.now()
      )}
    )
  }
}