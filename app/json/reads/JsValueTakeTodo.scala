package json.reads



import lib.model.{Category, Todo}

import play.api.libs.json.{Json, Reads}

import ixias.util.json.{JsonEnvReads}

case class JsValueTakeTodo(

  title: String,

  body: String,

  state_code : Short,

  category_id: Category.Id,

)



object JsValueTakeTodo extends JsonEnvReads {

  implicit val categoryIdReads: Reads[Category.Id] = idAsNumberReads[Category.Id]

  implicit val reads: Reads[JsValueTakeTodo] = Json.reads[JsValueTakeTodo]

}
