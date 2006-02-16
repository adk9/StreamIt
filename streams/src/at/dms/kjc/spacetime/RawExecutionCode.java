package at.dms.kjc.spacetime;

import java.util.Vector;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;
import at.dms.kjc.flatgraph2.FilterContent;

/**
 * This abstract class defines an interface for filter code generators.  
 * These generator add code necessary to execute the filter on raw, generating
 * SIR code.
 * 
 * @author mgordon
 *
 */
public abstract class RawExecutionCode 
{
    /** the prefix for the c method to receive structs over a network */
    public static String structReceivePrefix = "__popPointer";
    
    /** the c method that is used to receive structs over the static net */
    public static String structReceivePrefixStatic = structReceivePrefix
        + "Static";
    /** the c method that is used to receive structs over the gdn */
    public static String structReceivePrefixDynamic = structReceivePrefix
        + "Dynamic";

    //if true, inline the work function in the init and steady-state
    protected static final boolean INLINE_WORK = true;
    
    /*** fields for the var names we introduce ***/
    public static String recvBuffer = "__RECVBUFFER__";
    public static String recvBufferSize = "__RECVBUFFERSIZE__";
    public static String recvBufferBits = "__RECVBUFFERBITS__";

    //the output buffer for ratematching
    public static String sendBuffer = "__SENDBUFFER__";
    public static String sendBufferIndex = "__SENDBUFFERINDEX__";
    public static String rateMatchSendMethod = "__RATEMATCHSEND__";
    
    //recvBufferIndex points to the beginning of the tape
    public static String recvBufferIndex = "__RECVBUFFERINDEX__";
    //recvIndex points to the end of the tape
    public static String recvIndex = "_RECVINDEX__";

    public static String simpleIndex = "__SIMPLEINDEX__";
    
    public static String exeIndex = "__EXEINDEX__";
    public static String exeIndex1 = "__EXEINDEX__1__";

    public static String ARRAY_INDEX = "__ARRAY_INDEX__";
    public static String ARRAY_COPY = "__ARRAY_COPY__";

    public static String initSchedFunction = "__RAWINITSCHED__";
    public static String steadySchedFunction = "__RAWSTEADYSCHED__";
    
    public static String staticReceiveMethod = "static_receive_to_mem";
    public static String gdnReceiveMethod = "gdn_receive_to_mem";
    public static String staticSendMethod = "static_send";
    public static String gdnSendMethod = "gdn_send";
    public static String structReceiveMethodPrefix = "__popPointer";
    public static String arrayReceiveMethod = "__array_receive__";

    public static String initStage = "__INITSTAGE__";
    public static String steadyStage = "__STEADYSTAGE__";
    public static String primePumpStage = "__PRIMEPUMP__";
    public static String workCounter = "__WORKCOUNTER__";
 
    //keep a unique integer for each filter in each trace
    //so var names do not clash
    private static int globalID = 0;
    protected int uniqueID;

    protected GeneratedVariables generatedVariables;
    protected FilterInfo filterInfo;
    /** the method that executes each stage of the prime pump schedule */
    protected JMethodDeclaration primePumpMethod;
    /** true if this filter has dynamic network input, this will only 
     * happen when the filters is connected to a inputtracenode and on the dynamic 
     * net
     */
    protected boolean gdnInput;
    /** true if this filter has dynamic network output, this will only happen
     * if hte filter is connected to an outputtrcenode allocated to the gdn
     */
    protected boolean gdnOutput;
    
    protected RawTile tile;
    
    public RawExecutionCode(RawTile tile, FilterInfo filterInfo) 
    {
        this.tile = tile;
        this.filterInfo = filterInfo;
        generatedVariables = new GeneratedVariables();
        uniqueID = getUniqueID();
        primePumpMethod = null;
        //see if we have gdn input
        gdnInput = false;
        if (filterInfo.traceNode.getPrevious().isInputTrace()) {
            if (!IntraTraceBuffer.getBuffer(
                    (InputTraceNode)filterInfo.traceNode.getPrevious(),
                    filterInfo.traceNode).isStaticNet())
                gdnInput = true;
        }
        //see if we have gdn output
        gdnOutput = false;
        if (filterInfo.traceNode.getNext().isOutputTrace()) {
            if (!IntraTraceBuffer.getBuffer(filterInfo.traceNode,
                    (OutputTraceNode)filterInfo.traceNode.getNext()).isStaticNet())
                gdnOutput = true;
        }
    }
    

    public static int getUniqueID() 
    {
        return globalID++;
    }
    
    public abstract JFieldDeclaration[] getVarDecls();
    public abstract JMethodDeclaration[] getHelperMethods();
    public abstract JMethodDeclaration getInitStageMethod();
    public abstract JBlock getSteadyBlock();
    public abstract JMethodDeclaration getPrimePumpMethod();
    
    /**
     * Returns a for loop that uses field <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns empty (!not legal in the general case)
     */
    public static JStatement makeForLoop(JStatement body,
                                         JVariableDefinition var,
                                         JExpression count) {
        if (body == null)
            return new JEmptyStatement(null, null);
    
        // make init statement - assign zero to <var>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JExpression initExpr[] = {
            new JAssignmentExpression(null,
                                      new JFieldAccessExpression(null, 
                                                                 new JThisExpression(null),
                                                                 var.getIdent()),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(null, initExpr, null);
        // if count==0, just return init statement
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // return assignment statement
                return new JEmptyStatement(null, null);
            }
        }
        // make conditional - test if <var> less than <count>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JFieldAccessExpression(null, 
                                                                 new JThisExpression(null),
                                                                 var.getIdent()),
                                      count);
        JExpression incrExpr = 
            new JPostfixExpression(null, 
                                   Constants.OPE_POSTINC, 
                                   new JFieldAccessExpression(null, new JThisExpression(null),
                                                              var.getIdent()));
        JStatement incr = 
            new JExpressionStatement(null, incrExpr, null);

        return new JForStatement(null, init, cond, incr, body, null);
    }

    public static JStatement constToSwitchStmt(int constant) 
    {
        //alt code gen is always enabled!
        JAssignmentExpression send = 
            new JAssignmentExpression(null,
                                      new JFieldAccessExpression(null, new JThisExpression(null),
                                                                 Util.CSTOINTVAR),
                                      new JIntLiteral(constant));
    
        return new JExpressionStatement(null, send, null);
    }
    
    public static JStatement boundToSwitchStmt(int constant)  
    {
        return constToSwitchStmt(constant - 1);
    }
 
    /** 
     * @return The SIR code necessary to set the dynamic message header used 
     * when we send data over the gdn.
     */
    public JStatement setDynMsgHeader() {
       
        assert filterInfo.traceNode.getNext().isOutputTrace();
        //get the buffer
        IntraTraceBuffer buf = IntraTraceBuffer.getBuffer(filterInfo.traceNode,
                (OutputTraceNode)filterInfo.traceNode.getNext());
        assert !buf.isStaticNet();
        //get the type size
        int size = Util.getTypeSize(filterInfo.filter.getOutputType());
        
        //add one to the size because we need to send over the opcode 13 
        //with each pkt to inform the dram that this is a data payload
        size += 1;
        
        //make sure that each element can fit in a gdn pkt         
        assert size <= RawChip.MAX_GDN_PKT_SIZE : "Type size too large to fit in single dynamic network packet";
        
        JBlock block = new JBlock(null, new JStatement[0], null);
        //construct the args for the dyn header construction function
        
        //get the neigboring tile for the dram we are sending to...
        RawTile neighboringTile = buf.getDRAM().getNeighboringTile();
        
        //now calculated the final route, once the packet gets to the destination (neighboring) tile
        //it will be routed off the chip by 
        // 2 = west, 3 = south, 4 = east, 5 = north
        int finalRoute = buf.getDRAM().getDirectionFromTile();
        
        
        JExpression[] args = {new JIntLiteral(finalRoute),
                new JIntLiteral(size), 
                new JIntLiteral(0) /* user */, 
                new JIntLiteral(tile.getY()), new JIntLiteral(tile.getX()),
                new JIntLiteral(neighboringTile.getY()),
                new JIntLiteral(neighboringTile.getX())
        };
        
        JMethodCallExpression conDynHdr = 
            new JMethodCallExpression(RawChip.ConstructDynHdr, args);
        
        JAssignmentExpression ass = 
            new JAssignmentExpression(new JFieldAccessExpression(TraceIRtoC.DYNMSGHEADER),
                    conDynHdr);
        block.addStatement(new JExpressionStatement(ass));
        
        return block;
    }
    
}
