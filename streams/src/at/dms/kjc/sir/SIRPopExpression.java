package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a pop expression.
 */
public class SIRPopExpression extends JExpression {

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   */
  public SIRPopExpression(TokenReference where)
  {
    super(where);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return the type of this expression
   */
  public CType getType() {
    return CStdType.Void;
  }

  /**
   * Returns true iff this expression can be used as a statement (JLS 14.8)
   */
  public boolean isStatementExpression() {
    return true;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Throws an exception (NOT SUPPORTED YET)
   */
  public JExpression analyse(CExpressionContext context) throws PositionedError {
      at.dms.util.Utils.fail("Analysis of custom nodes not supported yet.");
      return this;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor.  NOT SUPPORTED YET.
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
      at.dms.util.Utils.fail("Visitors to custom nodes not supported yet.");
      //    p.visitMethodCallExpression(this, prefix, ident, args);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.  NOT SUPPORTED YET.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
      at.dms.util.Utils.fail("Visitors to custom nodes not supported yet.");
  }
}








