package at.dms.kjc.slir;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import at.dms.kjc.sir.SIRFileReader;
import at.dms.kjc.sir.SIRFileWriter;
import at.dms.kjc.backendSupport.*;

/**
 * 
 * @author mgordon
 *
 */
public abstract class Slicer {
    
    protected UnflatFilter[] topFilters;

    protected HashMap[] exeCounts;

    protected LinkedList<Filter> topSlices;

    public Filter[] io;

    /**
     * Create a Partitioner.
     * 
     * The number of partitions may be limited by <i>maxPartitions</i>, but
     * some implementations ignore <i>maxPartitions</i>.
     * 
     * @param topFilters  from {@link FlattenGraph}
     * @param exeCounts  a schedule
     * @param lfa  a linearAnalyzer to convert filters to linear form if appropriate.
     * @param work a work estimate, see {@link at.dms.kjc.sir.lowering.partition}, updeted if filters are added to a slice.
     * @param maxPartitions if non-zero, a maximum number of partitions to create
     */
    public Slicer(UnflatFilter[] topFilters, HashMap[] exeCounts) {
        this.topFilters = topFilters;
        this.exeCounts = exeCounts;
        if (topFilters != null)
            topSlices = new LinkedList<Filter>();
    }

    /**
     * Partition the stream graph into slices (slices) and return the slices.
     * @return The slices (slices) of the partitioned graph. 
     */
    public abstract Filter[] partition();

    /**
     * Check for I/O in slice
     * @param slice
     * @return Return true if this slice is an IO slice (file reader/writer).
     */
    public boolean isIO(Filter slice) {
        for (int i = 0; i < io.length; i++) {
            if (slice == io[i])
                return true;
        }
        return false;
    }
    
    /**
     * Add the slice to the list of top slices, roots of the forest.
     */
    public void addTopSlice(Filter slice) {
        topSlices.add(slice);
    }
    
    /**
     * remove this slice from the list of top slices, roots of the forest.
     */
    public void removeTopSlice(Filter slice) {
        assert topSlices.contains(slice);
        topSlices.remove(slice);
    }
    
    /**
     * Get all slices
     * @return All the slices of the slice graph. 
     */
    public Filter[] getSliceGraph() {
        //new slices may have been added so we need to reconstruct the graph each time
        LinkedList<Filter> sliceGraph = 
            DataFlowOrder.getTraversal(topSlices.toArray(new Filter[topSlices.size()]));
        
        return sliceGraph.toArray(new Filter[sliceGraph.size()]);
    }
    
    /**
     * Return true if the slice is a top (source) slice in the forrest
     */
    public boolean isTopSlice(Filter slice) {
        for (Filter cur : topSlices) {
            if (cur == slice)
                return true;
        }
        return false;
    }
    
    /**
     * Get just top level slices in the slice graph.
     * 
     * @return top level slices
     */
    public Filter[] getTopSlices() {
        assert topSlices != null;
        return topSlices.toArray(new Filter[topSlices.size()]);
    }

    /**
     * Does the the slice graph contain slice (perform a simple linear
     * search).
     * 
     * @param slice The slice to query.
     * 
     * @return True if the slice graph contains slice.
     */
    public boolean containsSlice(Filter slice) {
        Filter[] sliceGraph = getSliceGraph();
        for (int i = 0; i < sliceGraph.length; i++) 
            if (sliceGraph[i] == slice)
                return true;
        return false;
    }
  
    public void dumpGraph(String filename, Layout layout) {
        dumpGraph(filename, layout, true);
    }
    
    // dump the the completed partition to a dot file
    public void dumpGraph(String filename, Layout layout, boolean fullInfo) {
        Filter[] sliceGraph = getSliceGraph();
        StringBuffer buf = new StringBuffer();
        buf.append("digraph Flattend {\n");
        buf.append("size = \"8, 10.5\";\n");

        for (int i = 0; i < sliceGraph.length; i++) {
            Filter slice = sliceGraph[i];
            assert slice != null;
            buf.append(slice.hashCode() + " [ " + 
                    sliceName(slice, layout, fullInfo) + 
                    "\" ];\n");
            Filter[] next = getNext(slice/* ,parent */, SchedulingPhase.STEADY);
            for (int j = 0; j < next.length; j++) {
                assert next[j] != null;
                buf.append(slice.hashCode() + " -> " + next[j].hashCode()
                           + ";\n");
            }
            next = getNext(slice, SchedulingPhase.INIT);
            for (int j = 0; j < next.length; j++) {
                assert next[j] != null;
                buf.append(slice.hashCode() + " -> " + next[j].hashCode()
                           + "[style=dashed,color=red];\n");
            }
        }

        buf.append("}\n");
        // write the file
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(buf.toString());
            fw.close();
        } catch (Exception e) {
            System.err.println("Could not print extracted slices");
        }
    }
    
    // get the downstream slices we cannot use the edge[] of slice
    // because it is for execution order and this is not determined yet.
    protected Filter[] getNext(Filter slice, SchedulingPhase phase) {
        SliceNode node = slice.getInputNode();
        if (node instanceof InputNode)
            node = node.getNext();
        while (node != null && node instanceof WorkNode) {
            node = node.getNext();
        }
        if (node instanceof OutputNode) {
            Edge[][] dests = ((OutputNode) node).getDests(phase);
            ArrayList<Object> output = new ArrayList<Object>();
            for (int i = 0; i < dests.length; i++) {
                Edge[] inner = dests[i];
                for (int j = 0; j < inner.length; j++) {
                    // Object next=parent.get(inner[j]);
                    Object next = inner[j].getDest().getParent();
                    if (!output.contains(next))
                        output.add(next);
                }
            }
            Filter[] out = new Filter[output.size()];
            output.toArray(out);
            return out;
        }
        return new Filter[0];
    }

    protected WorkNodeContent getFilterContent(UnflatFilter f) {
        WorkNodeContent content;

        if (f.filter instanceof SIRFileReader)
            content = new FileInputContent(f);
        else if (f.filter instanceof SIRFileWriter)
            content = new FileOutputContent(f);
        else {
            assert f.filter != null;
            content = new WorkNodeContent(f);
        }
        
        return content;
    }
   
    
    //return a string with all of the names of the filterslicenodes
    // and blue if linear
    protected  String sliceName(Filter slice, Layout layout, boolean fullInfo) {
        SliceNode node = slice.getInputNode();

        StringBuffer out = new StringBuffer();

        //do something fancy for linear slices!!!
        if (((WorkNode)node.getNext()).getFilter().getArray() != null)
            out.append("color=cornflowerblue, style=filled, ");
        
        out.append("label=\"" + slice.hashCode() + "\\n");
        if (fullInfo)
            out.append(node.getAsInput().debugString(true, SchedulingPhase.INIT) + "\\n" +
                        node.getAsInput().debugString(true, SchedulingPhase.STEADY));//toString());
        
        node = node.getNext();
        while (node != null ) {
            if (node.isFilterSlice()) {
                WorkNodeContent f = node.getAsFilter().getFilter();
                out.append("\\n" + node.toString() + "{"
                        + "}");
                if (f.isTwoStage())
                    out.append("\\npre:(peek, pop, push): (" + 
                            f.getPreworkPeek() + ", " + f.getPreworkPop() + "," + f.getPreworkPush());
                out.append(")\\n(peek, pop, push: (" + 
                        f.getPeekInt() + ", " + f.getPopInt() + ", " + f.getPushInt() + ")");
                out.append("\\nMult: init " + f.getInitMult() + ", steady " + f.getSteadyMult());
                if (layout != null) 
                    out.append("\\nTile: " + layout.getComputeNode(slice.getWorkNode()).getUniqueId());
                out.append("\\n *** ");
            }
            else {
                if (fullInfo)
                    out.append("\\n" + node.getAsOutput().debugString(true, SchedulingPhase.INIT) + "\\n" +
                            node.getAsOutput().debugString(true, SchedulingPhase.STEADY));
               
            }
            /*else {
                //out.append("\\n" + node.toString());
            }*/
            node = node.getNext();
        }
        return out.toString();
    }

    /**
     * Force creation of kopi methods and fields for predefined filters.
     */
    public void createPredefinedContent() {
        for (Filter s : getSliceGraph()) {
        	if (s.getWorkNode().getFilter() instanceof PredefinedContent) {
        		((PredefinedContent)s.getWorkNode().getFilter()).createContent();
            }
        }

    }
    
    /**
     * Update all the necessary state to add node to slice.
     * 
     * @param node The node to add.
     * @param slice The slice to add the node to.
     */
    public void addFilterToSlice(WorkNode node, 
            Filter slice) {
    }
    

}
