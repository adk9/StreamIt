package at.dms.kjc.smp;

import at.dms.kjc.sir.*;
import at.dms.kjc.backendSupport.*;
import java.util.HashMap;

import at.dms.kjc.slir.*;
import at.dms.kjc.KjcOptions;

/**
 * This class is the super class of all partitioners that act on the SIR graph to
 * data-parallelize the application.  Currently we support space-multiplexed and
 * time-multiplexed data parallel partitioners.  
 * 
 * @author mgordon
 *
 */
public abstract class Scheduler implements Layout<Core> {
    
    protected BasicSpaceTimeSchedule graphSchedule;
    protected HashMap<InternalFilterNode, Core> layoutMap;
    
    public Scheduler() {
        graphSchedule = null;
        layoutMap = new HashMap<InternalFilterNode, Core>();
    }

    public boolean isSMD() {
        return (this instanceof SMD);
    }
    
    public boolean isTMD() {
        return (this instanceof TMD);
    }
    
    public void setGraphSchedule(BasicSpaceTimeSchedule graphSchedule) {
        this.graphSchedule = graphSchedule;
    }
    
    public abstract void run(int tiles);

    public SIRStream SIRFusion(SIRStream str, int tiles) {return str;};
    
    public BasicSpaceTimeSchedule getGraphSchedule() {
        return graphSchedule;
    }
}
