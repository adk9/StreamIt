/*
 * Flybit.java: an interesting piece of the Butterfly example
 * (to demonstrate split/joins)
 * $Id: Flybit.java,v 1.3 2001-10-10 19:58:55 dmaze Exp $
 */

import streamit.*;

class IntSource extends Filter
{
    int x;
    Channel output = new Channel(Integer.TYPE, 1);
    public void init()
    {
        this.x = 0;
    }
    public void work()
    {
        output.pushInt(x++);
    }
}

class IntSub extends Filter
{
    Channel input = new Channel(Integer.TYPE, 2);
    Channel output = new Channel(Integer.TYPE, 1);
    public void work()
    {
        output.pushInt(input.popInt() - input.popInt());
    }
}

class IntAdd extends Filter
{
    Channel input = new Channel(Integer.TYPE, 2);
    Channel output = new Channel(Integer.TYPE, 1);
    public void work()
    {
        output.pushInt(input.popInt() + input.popInt());
    }
}

class IntFly extends Pipeline // SplitJoin
{
    public void init() 
    {
        // setSplitter(DUPLICATE());
        add(new IntSub());
        add(new IntAdd());
        // setJoiner(WEIGHTED_ROUND_ROBIN(2, 2));
    }
}

class IntPrinter extends Filter
{
    Channel input = new Channel(Integer.TYPE, 1);
    public void work ()
    {
        System.out.println (input.popInt ());
    }
}

public class Flybit extends Pipeline
{
    static public void main(String[] t)
    {
        Flybit test = new Flybit();
        test.run();
    }
    
    public void init()
    {
        add(new IntSource());
        add(new IntFly());
        add(new IntPrinter());
    }
}

        
