package json.writes

import play.api.libs.json.{Json, Writes}

case class JsValueErrorResponseItem (
    code:Int,
    message:String
)

object JsValueErrorResponseItem {
  implicit val writes: Writes[JsValueErrorResponseItem] = Json.writes[JsValueErrorResponseItem]
}