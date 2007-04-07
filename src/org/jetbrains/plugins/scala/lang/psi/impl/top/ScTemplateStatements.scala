package org.jetbrains.plugins.scala.lang.psi.impl.top.templateStatements 


  /**
  * User: Dmitry.Krasilschikov
  * Date: 13.11.2006
  * Time: 16:32:36
  */

  import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
  import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
  import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
  import org.jetbrains.plugins.scala.lang.psi.impl.patterns._
  import org.jetbrains.plugins.scala.lang.psi.impl.expressions._
  import org.jetbrains.plugins.scala.lang.psi.impl.primitives._
  import org.jetbrains.plugins.scala.lang.psi.impl.types._
  import org.jetbrains.plugins.scala.lang.psi.impl.top.params._

  import com.intellij.psi.tree.TokenSet
  import com.intellij.lang.ASTNode
  import com.intellij.psi.tree.IElementType;
  import com.intellij.psi._

  import org.jetbrains.annotations._
  import org.jetbrains.plugins.scala.lang.formatting.patterns.indent._
  import org.jetbrains.plugins.scala.lang.parser.parsing.top.template.DclDef
  import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
  import org.jetbrains.plugins.scala.icons.Icons

  trait ScTemplateStatement extends ScalaPsiElement {
    override def copy(): PsiElement = ScalaPsiElementFactory.createTemplateStatementFromText(this.getText, this.getManager).getPsi

    private def isDefinitionPredicate = (elementType: IElementType) => (elementType == ScalaTokenTypes.kTHIS)

    private def isThis = (elementType: IElementType) => (ScalaTokenTypes.kTHIS.equals(elementType))

    [Nullable]
    def getType: ScalaPsiElement = {
      def isType = (e: IElementType) => ScalaElementTypes.TYPE_BIT_SET.contains(e)
      val colon = getChild(ScalaTokenTypes.tCOLON)
      if (colon == null) return null

      childSatisfyPredicateForElementType(isType, colon.getNextSibling).asInstanceOf[ScalaPsiElement]
    }

    def getShortName: String = {
      val theName = names.elements.next
      if (theName != null) theName.getText
      else "no name"
    }

    override def getTextOffset = {
      val theName = names.elements.next;
      if (theName != null) theName.getTextRange.getStartOffset
      else this.getTextRange.getStartOffset
    }

    def isManyDeclarations: Boolean = (getChild(ScalaElementTypes.IDENTIFIER_LIST) != null)
    def getDeclarations: ScalaPsiElement = getChild(ScalaElementTypes.IDENTIFIER_LIST).asInstanceOf[ScalaPsiElement]

    [NotNull]
    def names: Iterable[PsiElement] = {
      if (isManyDeclarations) return getDeclarations.childrenOfType[PsiElement](TokenSet.create(Array(ScalaTokenTypes.tIDENTIFIER)))

      childrenOfType[PsiElement](TokenSet.create(Array(ScalaTokenTypes.tIDENTIFIER)))
    }
  }

  trait ScFunction extends ScParametrized {
    private def isManyParamClauses = (getChild(ScalaElementTypes.PARAM_CLAUSES) != null)
    private def getParamClausesNode: ScParamClauses = getChild(ScalaElementTypes.PARAM_CLAUSES).asInstanceOf[ScParamClauses]

    def paramClauses: Iterable[ScParamClause] = {
      if (isManyParamClauses) return getParamClausesNode.paramClauses

      childrenOfType[ScParamClause](TokenSet.create(Array(ScalaElementTypes.PARAM_CLAUSE)))
    }

    override def getIcon(flags: Int) = Icons.METHOD
  }

  /***************** definition ***********************/

  trait ScDefinition extends ScTemplateStatement {
    override def toString: String = "definition"

    [Nullable]
    def getExpr: ScalaPsiElement = {
      getLastChild match {
        case under: ScalaPsiElement if (ScalaTokenTypes.tUNDER.equals(under.getNode.getElementType)) => under
        case expr: ScalaPsiElement => expr
        case _ => null
      }
    }
  }

  trait ScParametrized extends ScTemplateStatement {
    private def isTypeParamClause = (e: PsiElement) => e.isInstanceOf[ScTypeParamClause]
    def getTypeParamClause: ScTypeParamClause = childSatisfyPredicateForPsiElement(isTypeParamClause).asInstanceOf[ScTypeParamClause]
  }

  trait ScValue extends ScTemplateStatement {
    override def isManyDeclarations = (getChild(ScalaElementTypes.PATTERN2_LIST) != null)
    override def getDeclarations: ScalaPsiElement = getChild(ScalaElementTypes.PATTERN2_LIST).asInstanceOf[ScalaPsiElement]

    override def getIcon(flags: Int) = Icons.VAL

    [Nullable]
    override def names: Iterable[PsiElement] = {
      if (isManyDeclarations) return getDeclarations.childrenOfType[PsiElement](ScalaElementTypes.PATTERN2_BIT_SET)

      childrenOfType[PsiElement](ScalaElementTypes.PATTERN2_BIT_SET)
    }
  }

  case class ScPatternDefinition(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDefinition with ScValue with IfElseIndent {
    override def toString: String = "pattern" + " " + super.toString
  }


  case class ScFunctionDefinition(node: ASTNode) extends ScalaPsiElementImpl(node) with ScFunction with ScDefinition with IfElseIndent {
    override def toString: String = "function" + " " + super.toString

    //    override def getIcon(flags: Int) = Icons.METHOD
  }



  /************** supplementary constructor ***************/

  case class ScSelfInvocation(node: ASTNode) extends ScalaPsiElementImpl(node) {
    override def toString: String = "self invocation"
  }

  case class ScConstrExpr(node: ASTNode) extends ScalaPsiElementImpl (node) {
    override def toString: String = "constructor expression"
  }

  case class ScSupplementaryConstructor(node: ASTNode) extends ScalaPsiElementImpl(node) with ScFunction with ScDefinition with IfElseIndent {
    override def toString: String = "supplementary constructor"

    override def names: Iterable[PsiElement] = childrenOfType[PsiElement](TokenSet.create(Array(ScalaTokenTypes.kTHIS)))
  }

  case class ScTypeDefinition(node: ASTNode) extends ScalaPsiElementImpl(node) with ScDefinition with ScTemplateStatement with ScParametrized{
    override def toString: String = "type" + " " + super.toString

    def nameNode = {
      def isName = (elementType: IElementType) => (elementType == ScalaTokenTypes.tIDENTIFIER)
      childSatisfyPredicateForElementType(isName)
    }

    override def getName = nameNode.getText

  }

  /***************** declaration ***********************/

  trait Declaration extends ScTemplateStatement {
    override def toString: String = "declaration"
  }

  case class ScValueDeclaration(node: ASTNode) extends ScalaPsiElementImpl(node) with ScValue with Declaration {
    override def toString: String = "value" + " " + super.toString

    override def getIcon(flags: Int) = Icons.VAL
  }

  case class ScFunctionDeclaration(node: ASTNode) extends ScalaPsiElementImpl(node) with Declaration with ScFunction {
    override def toString: String = "function" + " " + super.toString

    override def getIcon(flags: Int) = Icons.FUNCTION
  }

  case class ScTypeDeclaration(node: ASTNode) extends ScalaPsiElementImpl(node) with Declaration with ScTemplateStatement {
    override def toString: String = "type" + " " + super.toString

    def nameNode = {
      def isName = (elementType: IElementType) => (elementType == ScalaTokenTypes.tIDENTIFIER)
      childSatisfyPredicateForElementType(isName)
    }

    override def getName = nameNode.getText
  }

  /************** function signature *********************/

  /* class ScFunctionSignature (node : ASTNode) extends ScalaPsiElementImpl (node) {
    override def toString: String = "function signature"
  }*/

  /****************** variable ************************/

  class ScIdentifierList(node: ASTNode) extends ScalaPsiElementImpl (node) {
    override def toString: String = "list of identifiers"
  }

  class ScReference(node: ASTNode) extends ScalaPsiElementImpl (node) {
    override def toString: String = "Reference identifier"
  }
  /****************** pattern2List  ************************/

  class ScPattern2List(node: ASTNode) extends ScalaPsiElementImpl (node) {
    override def toString: String = "list of pattern2"
  }
