package streamit;

import streamit.scheduler.SchedJoinType;
import streamit.scheduler.Scheduler;
import java.util.ArrayList;
import java.util.Iterator;

public class RoundRobinJoiner extends Joiner {
    public RoundRobinJoiner()
    {
    }

    public void work ()
    {
        int inputIndex;
        for (inputIndex = 0; inputIndex < srcs.size (); inputIndex++)
        {
            ASSERT (streamInput [inputIndex]);
            passOneData (streamInput [inputIndex], streamOutput);
        }
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedJoinType getSchedType (Scheduler scheduler)
    {
        ArrayList weights = new ArrayList (srcs.size ());

        Integer one = new Integer (1);
        int index;
        for (index = 0; index < srcs.size (); index++)
        {
            weights.add (one);
        }

        return scheduler.newSchedJoinType (SchedJoinType.ROUND_ROBIN, weights, this);
    }
}
