package org.jetbrains.plugins.scala
package lang
package psi
package types

import nonvalue.NonValueType

/**
 * Use this type if you want to resolve generics.
 * In conformance using ScUndefinedSubstitutor you can accumulate imformation
 * about possible generic type.
 */
case class ScUndefinedType(tpt: ScTypeParameterType) extends NonValueType {
  var level = 0
  def this(tpt: ScTypeParameterType, level: Int) {
    this(tpt)
    this.level = level
  }

  def inferValueType: ValueType = tpt

  override def equivInner(r: ScType, subst: ScUndefinedSubstitutor, falseUndef: Boolean): (Boolean, ScUndefinedSubstitutor) = {
    var undefinedSubst = subst
    r match {
      case _ if falseUndef => return (false, undefinedSubst)
      case u2: ScUndefinedType if u2.level > level =>
        return (true, undefinedSubst.addUpper((u2.tpt.name, u2.tpt.getId), this))
      case u2: ScUndefinedType if u2.level < level =>
        return (true, undefinedSubst.addUpper((tpt.name, tpt.getId), u2))
      case u2: ScUndefinedType if u2.level == level =>
        return (true, undefinedSubst)
      case rt => {
        undefinedSubst = undefinedSubst.addLower((tpt.name, tpt.getId), rt)
        undefinedSubst = undefinedSubst.addUpper((tpt.name, tpt.getId), rt)
        return (true, undefinedSubst)
      }
    }
  }
}

/**
 * This type works like undefined type, but you cannot use this type
 * to resolve generics. It's important if two local type
 * inferences work together.
 */
case class ScAbstractType(tpt: ScTypeParameterType, lower: ScType, upper: ScType) extends NonValueType {
  def inferValueType = tpt

  def simplifyType: ScType = {
    if (upper.equiv(Any)) lower else if (lower.equiv(Nothing)) upper else lower
  }

  override def removeAbstracts = simplifyType
}