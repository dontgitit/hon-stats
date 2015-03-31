package utils

import scala.reflect.runtime.universe._
import play.api.libs.json._

// http://stackoverflow.com/questions/20089920/custom-scala-enum-most-elegant-version-searched
abstract class ClassEnumBase[A: TypeTag] {
  trait Value

  /**
   * Helper for .values; returns all subclasses if context is sealed
   */
  protected def sealedDescendants: Option[Set[Symbol]] = {
    val symbol = typeOf[A].typeSymbol
    val internal = symbol.asInstanceOf[scala.reflect.internal.Symbols#Symbol]
    if (internal.isSealed)
      Some(internal.sealedDescendants.map(_.asInstanceOf[Symbol]) - symbol)
    else None
  }

  /**
   * Generate a .values that has all the values of the Enum
   */
  val values = (sealedDescendants getOrElse Set.empty).map(
    symbol => symbol.owner.typeSignature.member(symbol.name.toTermName)).map(
      module => reflect.runtime.currentMirror.reflectModule(module.asModule).instance).map(
      obj => obj.asInstanceOf[A]
    ).toSeq

  /**
   * Allow instantiating from a String using .withName
   */
  def withName(name: String) = values.find(_.toString == name).get // TODO - should we pregenerate a Map or is this fine?

  /**
   * Define a JSON format
   */
  implicit val enumFormat: Format[A] = Format[A](
    new Reads[A] {
      def reads(js: JsValue): JsResult[A] = {
        js.validate[String].map { str =>
          withName(str)
        }
      }
    },
    new Writes[A] {
      def writes(d: A): JsValue = {
        JsString(d.toString)
      }
    }
  )

  implicit def ClassEnumBaseOrdering: Ordering[A] = Ordering.by(_.toString)
}