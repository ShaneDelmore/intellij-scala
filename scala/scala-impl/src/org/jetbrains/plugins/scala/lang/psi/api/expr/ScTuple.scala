package org.jetbrains.plugins.scala
package lang
package psi
package api
package expr

/**
  * @author Alexander Podkhalyuzin
  *         Date: 06.03.2008
  */
trait ScTuple extends ScInfixArgumentExpression {
  def exprs: Seq[ScExpression] = findChildrenByClassScala(classOf[ScExpression]).toSeq

  override def accept(visitor: ScalaElementVisitor): Unit = {
    visitor.visitTupleExpr(this)
  }

}

object ScTuple {
  def unapply(e: ScTuple): Some[Seq[ScExpression]] = Some(e.exprs)
}