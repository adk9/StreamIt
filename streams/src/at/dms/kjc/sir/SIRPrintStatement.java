/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: SIRPrintStatement.java,v 1.1 2001-09-25 14:19:25 thies Exp $
 */

package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;
import at.dms.util.MessageDescription;
import at.dms.util.InconsistencyException;

/**
 * This represents a print statement, originally formulated with
 * System.out.println in StreaMIT.
 */
public class SIRPrintStatement extends JStatement {

    /**
     * The argument to the print statement.
     */
    protected JExpression arg;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     * @param where the line of this node in the source code
     */
    public SIRPrintStatement(TokenReference where, JExpression arg, JavaStyleComment[] comments) {
	super(where, comments);
	this.arg = arg;
    }

    // ----------------------------------------------------------------------
    // SEMANTIC ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Analyses the statement (semantically) - NOT IMPLEMENTED YET.
     * @param	context		the analysis context
     * @exception	PositionedError	the analysis detected an error
     */
    public void analyse(CBodyContext context) throws PositionedError {
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Generates a sequence of bytescodes - NOT IMPLEMENTED YET.
     * @param	code		the code list
     */
    public void genCode(CodeSequence code) {}
}
