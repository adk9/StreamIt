package streamit;

import java.util.*;
import java.lang.reflect.*;

import streamit.scheduler.*;

// creates a split/join
public class SplitJoin extends Stream
{
    Splitter splitter;
    Joiner joiner;

    SplitJoinType splitType, joinType;

    List childrenStreams;

    public SplitJoin()
    {
        super();
    }

    public SplitJoin(int n)
    {
        super(n);
    }

    // initializing IO will be handled by the add function
    public void initIO () { }

    // type of a split or a join:
    public static class SplitJoinType
    {
        // type:
        //  1 - round robin
        //  2 - weighted round robin
        //  3 - duplicate

        int type;
        List weights;

        SplitJoinType (int myType)
        {
            switch (myType)
            {
                case 1: // round robin
                case 3: // duplicate
                    break;
                case 2: // weighted round robin - need a weight list
                    weights = new LinkedList ();
                    break;
                default:
                    // passed an illegal parameter to the constructor!
                    ASSERT (false);
            }

            type = myType;
        }

        SplitJoinType addWeight (int weight)
        {
            ASSERT (weights != null);
            weights.add (new Integer (weight));
            return this;
        }

        Splitter getSplitter ()
        {
            switch (type)
            {
                case 1:
                    return new RoundRobinSplitter ();
                case 2:
                    WeightedRoundRobinSplitter splitter = new WeightedRoundRobinSplitter ();
                    while (!weights.isEmpty ())
                    {
                        splitter.addWeight ((Integer)weights.remove (0));
                    }
                    return splitter;
                case 3:
                    return new DuplicateSplitter ();
                default:
                    ASSERT (false);
            }
            return null;
        }

        Joiner getJoiner ()
        {
            switch (type)
            {
                case 1:
                    return new RoundRobinJoiner ();
                case 2:
                    WeightedRoundRobinJoiner joiner = new WeightedRoundRobinJoiner ();
                    while (!weights.isEmpty ())
                    {
                        joiner.addWeight ((Integer)weights.remove (0));
                    }
                    return joiner;
                case 3: // there are no duplicate joiners!
                default:
                    ASSERT (false);
            }
            return null;
        }
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1)
    {
        return new SplitJoinType (2).addWeight (w1);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2)
    {
        return new SplitJoinType (2).addWeight (w1).addWeight (w2);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3)
    {
        return new SplitJoinType (2).addWeight (w1).addWeight (w2).addWeight (w3);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3, int w4)
    {
        return new SplitJoinType (2).addWeight (w1).addWeight (w2).addWeight (w3).addWeight (w4);
    }

    public static SplitJoinType WEIGHTED_ROUND_ROBIN (int w1, int w2, int w3, int w4, int w5, int w6, int w7)
    {
        return new SplitJoinType (2).addWeight (w1).addWeight (w2).addWeight (w3).addWeight (w4).addWeight (w5).addWeight (w6).addWeight (w7);
    }

    public static SplitJoinType ROUND_ROBIN ()
    {
        return new SplitJoinType (1);
    }

    public static SplitJoinType DUPLICATE ()
    {
        return new SplitJoinType (3);
    }

    // specify the splitter
    public void setSplitter(SplitJoinType type)
    {
        ASSERT (splitter == null && type != null);
        splitter = type.getSplitter ();

        splitType = type;
    }

    // specify the joiner
    // must also add all the appropriate outputs to the joiner!
    public void setJoiner(SplitJoinType type)
    {
        ASSERT (joiner == null && type != null);
        joiner = type.getJoiner ();

        ListIterator iter;
        iter = childrenStreams.listIterator ();
        while (iter.hasNext ())
        {
            Stream s = (Stream) iter.next ();
            ASSERT (s != null);

            joiner.add (s);
        }

        joinType = type;
    }

    // add a stream to the parallel section between the splitter and the joiner
    public void add(Stream s)
    {
        ASSERT (joiner == null);

        // add the stream to the Split
        if (splitter != null)
        {
            splitter.add (s);
        }

        // save the stream to add to the Join
        if (childrenStreams == null)
        {
            childrenStreams = new LinkedList ();
        }
        childrenStreams.add (s);
    }

    public void connectGraph ()
    {
        // setup all children of this splitjoin
        {
            ListIterator iter;
            iter = childrenStreams.listIterator ();
            while (iter.hasNext ())
            {
                Stream s = (Stream) iter.next ();
                ASSERT (s != null);

                s.setupOperator ();
            }
        }
        // connect the SplitJoin with the Split and the Join
        if (splitter != null)
        {
            splitter.setupOperator ();
            streamInput = splitter.getIOField ("streamInput", 0);
            ASSERT (streamInput != null);
        }

        if (joiner != null)
        {
            joiner.setupOperator ();
            streamOutput = joiner.getIOField ("streamOutput", 0);
            ASSERT (streamOutput != null);
        }
    }

    // ----------------------------------------------------------------
    // This code constructs an independent graph for the scheduler
    // ----------------------------------------------------------------

    SchedStream constructSchedule ()
    {
        // create a new splitjoin
        SchedSplitJoin splitJoin = scheduler.newSchedSplitJoin (this);

        // setup the splitter
        if (splitter != null)
        {
            SchedSplitType splitType;
            splitType = splitter.getSchedType (scheduler);
            splitJoin.setSplitType (splitType);
        }

        // setup the joiner
        if (joiner != null)
        {
            SchedJoinType joinType;
            joinType = joiner.getSchedType (scheduler);
            splitJoin.setJoinType (joinType);
        }

        // add all the children:
        {
            ListIterator iter;
            iter = childrenStreams.listIterator ();

            while (iter.hasNext ())
            {
                Stream child = (Stream) iter.next ();
                ASSERT (child);

                SchedStream schedChild = child.constructSchedule ();
                splitJoin.addChild (schedChild);
            }
        }

        return splitJoin;
    }

}