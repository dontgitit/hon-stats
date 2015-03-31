package models

import utils.ClassEnumBase

object HoNMap extends ClassEnumBase[HoNMap] {
  case object caldavar extends HoNMap
}
sealed trait HoNMap extends HoNMap.Value