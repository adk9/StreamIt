package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.*;
import java.util.*;

/**
 * Provides a means for estimating the amount of work in a stream graph.
 */
class WorkEstimate {
    
    /**
     * Maps stream constructs to a WorkInfo node.
     */
    private HashMap workMap;

    /**
     * The total amount of work contained in the stream.
     */
    private int toplevelWork;

    private WorkEstimate() {
	this.workMap = new HashMap();
	this.toplevelWork = 0;
    }

    /**
     * Returns a work estimate of <str>
     */
    public static WorkEstimate getWorkEstimate(SIRStream str) {
	WorkEstimate result = new WorkEstimate();
	result.doit(str);
	return result;
    }

    /**
     * Returns the work estimate for filter <obj>.  Requires that
     * <obj> was present in the original graph used to construct this.
     */
    public int getWork(SIRFilter obj) {
	return ((WorkInfo)workMap.get(obj)).totalWork;
    }

    /**
     * Returns the percent of the total stream's work that is consumed
     * by filter <obj>.  Returns this in the range of 0-100.
     */
    public float getPercentageWork(SIRFilter obj) {
	return 100 * 
	    ((float)((WorkInfo)workMap.get(obj)).totalWork) / toplevelWork;
    }

    /**
     * prints work of all functions to system.err.
     */
    public void printWork() {
	System.err.println("\nWORK ESTIMATES:");
	for (Iterator it = workMap.keySet().iterator(); it.hasNext(); ) {
	    SIRFilter obj = (SIRFilter)it.next();
	    String objName = obj.toString();
	    System.err.print(objName);
	    for (int i=objName.length();  i<70; i++) {
		System.err.print(" ");
	    }
	    System.err.println("\t" + getWork(obj) + "\t" + "(" +
			       ((int)getPercentageWork(obj)) + "%)");
	}
	
    }

    /**
     * Estimates the work in <str>
     */
    private void doit(SIRStream str) {
	// get execution counts for filters in <str>
	HashMap executionCounts = SIRScheduler.getExecutionCounts(str)[1];

	// for each filter, build a work count
	for (Iterator it = executionCounts.keySet().iterator();
	     it.hasNext(); ){
	    SIROperator obj = (SIROperator)it.next();
	    if (obj instanceof SIRFilter) {
		int count = ((int[])executionCounts.get(obj))[0];
		int work = WorkVisitor.getWork((SIRFilter)obj);
		workMap.put(obj, new WorkInfo(count*work));
		toplevelWork += count*work;
	    }
	}
    }
}

class WorkInfo {

    /**
     * Returns total amount of work enclosed in this node.
     */
    public final int totalWork;

    public WorkInfo(int totalWork) {
	this.totalWork = totalWork;
    }

    public String toString() {
	return "totalWork = " + totalWork;
    }
}

class WorkVisitor extends SLIREmptyVisitor implements WorkConstants {
    /**
     * An estimate of the amount of work found by this filter.
     */
    private int work;

    private WorkVisitor() {
	this.work = 0;
    }

    /**
     * Returns estimate of work in <filter>
     */
    public static int getWork(SIRFilter filter) {
        if (!filter.needsWork ())
		return 0;
	WorkVisitor visitor = new WorkVisitor();
	if (filter.getWork()==null) {
	    System.err.println("this filter has null work function: " + filter);
	    return 0;
	}
	filter.getWork().accept(visitor);
	return visitor.work;
    }

    /* still need to handle:
    public void visitCastExpression(JCastExpression self,
				    JExpression expr,
				    CType type) {}

    public void visitUnaryPromoteExpression(JUnaryPromote self,
					    JExpression expr,
					    CType type) {}
    */

    /**
     * SIR NODES.
     */

    /**
     * Visits a peek expression.
     */
    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	super.visitPeekExpression(self, tapeType, arg);
	work += PEEK;
    }

    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	super.visitPopExpression(self, tapeType);
	work += POP;
    }

    /**
     * Visits a print statement.
     */
    public void visitPrintStatement(SIRPrintStatement self,
				    JExpression arg) {
	super.visitPrintStatement(self, arg);
	work += PRINT;
    }

    /**
     * Visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	super.visitPushExpression(self, tapeType, arg);
	work += PUSH;
    }

    /*

    /**
     * KJC NODES.
     */

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
				    JExpression cond,
				    JStatement body) {
	System.err.println("WARNING:  Estimating work in loop, assume N=" +
			   LOOP_COUNT);
	int oldWork = work;
	super.visitWhileStatement(self, cond, body);
	int newWork = work;
	work = oldWork + LOOP_COUNT * (newWork - oldWork);
    }

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
				     JExpression expr,
				     JSwitchGroup[] body) {
	super.visitSwitchStatement(self, expr, body);
	work += SWITCH;
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
				     JExpression expr) {
	super.visitReturnStatement(self, expr);
	work += RETURN;
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
				 JExpression cond,
				 JStatement thenClause,
				 JStatement elseClause) {
	super.visitIfStatement(self, cond, thenClause, elseClause);
	work += IF;
    }

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
				  JStatement init,
				  JExpression cond,
				  JStatement incr,
				  JStatement body) {
	if (init != null) {
	    init.accept(this);
	}
	// try to determine how many times the loop executes
	int loopCount = Unroller.getNumExecutions(init, cond, incr, body);
	if (loopCount==-1) {
	    System.err.println("WARNING:  Estimating work in loop, assume N=" +
			       LOOP_COUNT);
	    loopCount = LOOP_COUNT;
	}
	int oldWork = work;
	if (cond != null) {
	    cond.accept(this);
	}
	if (incr != null) {
	    incr.accept(this);
	}
	body.accept(this);
	int newWork = work;
	work = oldWork + loopCount * (newWork - oldWork);
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
				 JExpression cond,
				 JStatement body) {
	System.err.println("WARNING:  Estimating work in loop, assume N=" +
			   LOOP_COUNT);
	int oldWork = work;
	super.visitDoStatement(self, cond, body);
	int newWork = work;
	work = oldWork + LOOP_COUNT * (newWork - oldWork);
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
				       String label) {
	super.visitContinueStatement(self, label);
	work += CONTINUE;
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
				    String label) {
	super.visitBreakStatement(self, label);
	work += BREAK;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * Adds to work estimate an amount for an arithmetic op of type
     * expr.  Assumes <expr> is integral unless the type is explicitly
     * float or double.
     */
    private void countArithOp(JExpression expr) {
	if (expr.getType()==CStdType.Float ||
	    expr.getType()==CStdType.Double) {
	    work += FLOAT_ARITH_OP;
	} else {
	    work += INT_ARITH_OP;
	}
    }

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
					 JExpression expr) {
	super.visitUnaryPlusExpression(self, expr);
	countArithOp(self);
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
					  JExpression expr) {
	super.visitUnaryMinusExpression(self, expr);
	countArithOp(self);
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	super.visitBitwiseComplementExpression(self, expr);
	countArithOp(self);
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	super.visitLogicalComplementExpression(self, expr);
	countArithOp(self);
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
				     int oper,
				     JExpression left,
				     JExpression right) {
	super.visitShiftExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
					  int oper,
					  JExpression left,
					  JExpression right) {
	super.visitRelationalExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
				      int oper,
				      JExpression expr) {
	super.visitPrefixExpression(self, oper, expr);
	countArithOp(self);
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
				       int oper,
				       JExpression expr) {
	super.visitPostfixExpression(self, oper, expr);
	countArithOp(self);
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
				    JExpression prefix,
				    String ident) {
	super.visitNameExpression(self, prefix, ident);
	work += MEMORY_OP;
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
				      String oper,
				      JExpression left,
				      JExpression right) {
	super.visitBinaryExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
					  JExpression prefix,
					  String ident,
					  JExpression[] args) {
	super.visitMethodCallExpression(self, prefix, ident, args);
	work += METHOD_CALL;
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
					boolean equal,
					JExpression left,
					JExpression right) {
	super.visitEqualityExpression(self, equal, left, right);
	countArithOp(self);
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
					   JExpression cond,
					   JExpression left,
					   JExpression right) {
	super.visitConditionalExpression(self, cond, left, right);
	work += IF;
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						  int oper,
						  JExpression left,
						  JExpression right) {
	super.visitCompoundAssignmentExpression(self, oper, left, right);
	work += ASSIGN;
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
				     JExpression left,
				     String ident) {
	super.visitFieldExpression(self, left, ident);
	work += MEMORY_OP;
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
				       int oper,
				       JExpression left,
				       JExpression right) {
	super.visitBitwiseExpression(self, oper, left, right);
	countArithOp(self);
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
					  JExpression left,
					  JExpression right) {
	super.visitAssignmentExpression(self, left, right);
	work += ASSIGN;
    }
}
