package models

import utils.ClassEnumBase

object Team extends ClassEnumBase[Team] {
  case object Legion extends Team
  case object Hellbourne extends Team
}
sealed trait Team extends Team.Value