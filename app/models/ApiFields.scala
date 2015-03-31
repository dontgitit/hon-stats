package models

import utils.ClassEnumBase

object ApiFields extends ClassEnumBase[ApiFields] {
  case object account_id extends ApiFields
  case object hero_id extends ApiFields
}
sealed trait ApiFields extends ApiFields.Value
