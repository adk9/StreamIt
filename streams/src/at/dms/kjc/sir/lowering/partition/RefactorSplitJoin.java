package at.dms.kjc.sir.lowering.partition;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.Lifter;

public class RefactorSplitJoin {
    /**
     * Given a splitjoin <sj> and a partitioning <partition> of its
     * children, returns a new splitjoin in which all the elements of
     * each partition are factored into their own splitjoin.  The
     * returned element is also replaced in the parent (that is, the
     * parent is mutated.)
     */
    public static SIRSplitJoin addHierarchicalChildren(SIRSplitJoin sj, PartitionGroup partition) {
	// the new and old weights for the splitter and joiner
	int[] oldSplit=sj.getSplitter().getWeights();
	int[] oldJoin=sj.getJoiner().getWeights();
	JExpression[] newSplit=new JExpression[partition.size()];
	JExpression[] newJoin=new JExpression[partition.size()];

	// new splitjoin
	SIRSplitJoin newSplitJoin=new SIRSplitJoin(sj.getParent(), 
						   sj.getIdent(),
						   sj.getFields(), 
						   sj.getMethods());
	newSplitJoin.setInit(SIRStream.makeEmptyInit());

	// for all the partitions...
	for(int i=0;i<partition.size();i++) {
	    int partSize=partition.get(i);
	    if (partSize==1) {
		// if there is only one stream in the partition, then
		// we don't need to do anything; just add the children
		int pos = partition.getFirst(i);
		newSplitJoin.add(sj.get(pos), sj.getParams(pos));
		newSplit[i]=new JIntLiteral(oldSplit[pos]);
		newJoin[i]=new JIntLiteral(oldJoin[pos]);
	    } else {
		int sumSplit=0;
		int sumJoin=0;
		// child split and join weights
		JExpression[] childSplit=new JExpression[partSize];
		JExpression[] childJoin=new JExpression[partSize];
		// the child splitjoin
		SIRSplitJoin childSplitJoin=new SIRSplitJoin(newSplitJoin,
							     newSplitJoin.getIdent() + "_child" + i,
							     JFieldDeclaration.EMPTY(),
							     JMethodDeclaration.EMPTY());
		childSplitJoin.setInit(SIRStream.makeEmptyInit());

		// move children into hierarchical splitjoin
		for(int k=0,l=partition.getFirst(i);k<partSize;k++,l++) {
		    sumSplit+=oldSplit[l];
		    sumJoin+=oldJoin[l];
		    childSplit[k]=new JIntLiteral(oldSplit[l]);
		    childJoin[k]=new JIntLiteral(oldJoin[l]);
		    childSplitJoin.add(sj.get(l), sj.getParams(l));
		}
		// in the case of a duplicate splitter, <create>
		// disregards the weights array and makes them all 1
		childSplitJoin.setSplitter(SIRSplitter.create(childSplitJoin,sj.getSplitter().getType(),childSplit));
		childSplitJoin.setJoiner(SIRJoiner.create(childSplitJoin,sj.getJoiner().getType(),childJoin));

		// update new toplevel splitjoin
		newSplit[i]=new JIntLiteral(sumSplit);
		newJoin[i]=new JIntLiteral(sumJoin);
		newSplitJoin.add(childSplitJoin);
	    }
	}
	// set the splitter and joiner types according to the new weights
	newSplitJoin.setSplitter(SIRSplitter.create(newSplitJoin,sj.getSplitter().getType(),newSplit));
	newSplitJoin.setJoiner(SIRJoiner.create(newSplitJoin,sj.getJoiner().getType(),newJoin));
	// replace in parent
	sj.getParent().replace(sj,newSplitJoin);

	// return new sj
	return newSplitJoin;
    }

    /**
     * Given that all of the children of <sj> are pipelines and that
     * <partition> describes a partitioning for such a pipeline,
     * re-arrange <sj> into a pipeline of several splitjoins, each of
     * which has children corresponding to a segment of <partition>:
     *
     *      |                          |
     *      .                          .
     *    / | \                      / | \ 
     *   |  |  |                     | | |
     *   |  |  |         ===>        \ | /
     *   |  |  |                       .
     *    \ | /                      / | \
     *      .                        | | |
     *      |                        \ | /
     *      |                          .
     *
     * The returned element is also replaced in the parent (that is,
     * the parent is mutated.)
     */
    public static SIRPipeline addSyncPoints(SIRSplitJoin sj, PartitionGroup partition) {
	// check what we're getting
	checkSymmetry(sj);
	Utils.assert(partition.size()==((SIRPipeline)sj.get(0)).size());

	// make result pipeline
	SIRPipeline result = new SIRPipeline(sj.getParent(), 
					     sj.getIdent()+"_par",
					     JFieldDeclaration.EMPTY(),
					     JMethodDeclaration.EMPTY());
	result.setInit(SIRStream.makeEmptyInit());

	for (int i=0; i<partition.size(); i++) {
	    // new i'th splitjoin.  Replace init function in <sj>
	    // before we clone the methods
	    sj.setInit(SIRStream.makeEmptyInit());
	    SIRSplitJoin newSJ = new SIRSplitJoin(result, 
						  sj.getIdent()+"_"+i,
						  (JFieldDeclaration[])ObjectDeepCloner.deepCopy(sj.getFields()),
						  (JMethodDeclaration[])ObjectDeepCloner.deepCopy(sj.getMethods()));
	    // new pipe's for the i'th splitjoin
	    for (int j=0; j<sj.size(); j++) {
		SIRPipeline origPipe = (SIRPipeline)sj.get(j);
		// reset init function in origPipe before we clone it
		origPipe.setInit(SIRStream.makeEmptyInit());
		SIRPipeline pipe = new SIRPipeline(newSJ,
						   origPipe.getIdent()+"_"+i+"_"+j,
						   (JFieldDeclaration[])ObjectDeepCloner.deepCopy(origPipe.getFields()),
						   (JMethodDeclaration[])ObjectDeepCloner.deepCopy(origPipe.getMethods()));
		// add requisite streams to pipe
		for (int k=partition.getFirst(i); k<=partition.getLast(i); k++) {
		    pipe.add(origPipe.get(k), origPipe.getParams(k));
		}
		// add pipe to the sj for the current partition
		newSJ.add(pipe, sj.getParams(j));
		// try lifting in case we can
		Lifter.lift(pipe);
	    }

	    // make the splitter and joiner for <newSJ>.  In the end
	    // cases, this is the same as for <sj>; otherwise it's
	    // the template RR splits and joins
	    SIRSplitter split = (i==0 ? 
				 sj.getSplitter() : 
				 SIRSplitter.createUniformRR(newSJ, new JIntLiteral(1)));
	    SIRJoiner join = (i==partition.size()-1 ? 
			      sj.getJoiner() : 
			      SIRJoiner.createUniformRR(newSJ, new JIntLiteral(1)));
	    newSJ.setSplitter(split);
	    newSJ.setJoiner(join);

	    // add sj for this partition to the overall pipe
	    StreamItDot.printGraph(newSJ, "newsj" + i + ".dot");
	    result.add(newSJ);
	}

	// replace in parent
	sj.getParent().replace(sj,result);

	return result;
    }

    /**
     * Checks that <sj> has symmetrical pipeline children.
     */
    private static void checkSymmetry(SIRSplitJoin sj) {
	SIRStream child_0 = sj.get(0);
	Utils.assert(child_0 instanceof SIRPipeline);
	
	int size_0 = ((SIRPipeline)child_0).size();
	for (int i=1; i<sj.size(); i++) {
	    SIRStream child_i = sj.get(i);
	    Utils.assert(child_i instanceof SIRPipeline &&
			 ((SIRPipeline)child_i).size() == size_0);
	}
    }

}
