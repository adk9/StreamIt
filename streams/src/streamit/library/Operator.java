package streamit;

import java.lang.reflect.*;
import java.util.*;

// an operator takes N inputs and produces N outputs.
// Never explicitly instantiated
class Operator extends DestroyedClass
{
    ParameterContainer initParams;
    boolean initialized = false;

    public Operator()
    {
        OperatorInit ();
        initParams = new ParameterContainer ("");
    }

    public Operator(int n)
    {
        OperatorInit ();
        initParams = new ParameterContainer ("int").Add ("n", n);
    }

    public Operator(float f)
    {
        OperatorInit ();
        initParams = new ParameterContainer ("float").Add ("f", f);
    }

    public Operator(String str)
    {
        OperatorInit ();
        initParams = new ParameterContainer ("String").Add ("str", str);
    }

    public Operator(ParameterContainer params)
    {
        OperatorInit ();
        initParams = new ParameterContainer ("ParameterContainer").Add ("params", params);
    }

    // INIT FUNCTIONS ---------------------------------------------------------------------

    // initializatoin functions, to be over-ridden
    public void Init() { ASSERT (false); }

    // initializatoin functions, to be over-ridden
    public void Init(int n) { ASSERT (false); }

    // initializatoin functions, to be over-ridden
    public void Init(float f) { ASSERT (false); }

    // initializatoin functions, to be over-ridden
    public void Init(String str) {ASSERT (false); }

    // initializatoin functions, to be over-ridden
    public void Init(ParameterContainer params) {ASSERT (false); }

    // general initialization function for Stream class only

    private void OperatorInit ()
    {
        InitIO ();
    }

    // InitIO initializes all input/output channels
    // as required
    public void InitIO ()
    {
        // You must provide an InitIO function
        // to initialize
        ASSERT (false);
    }

    public static MessageStub MESSAGE_STUB;

    // initialize the MESSAGE_STUB
    static {
        MESSAGE_STUB = MessageStub.STUB;
    }

    // allSinks is used for scheduling the streaming graph
    public static LinkedList allSinks;
    public static LinkedList allFilters;
    public static HashSet fullChannels;

    static {
        allSinks = new LinkedList ();
        allFilters = new LinkedList ();
        fullChannels = new HashSet ();
    }

    void AddSink ()
    {
        allSinks.add (this);
    }

    void AddFilter ()
    {
        allFilters.add (this);
    }

    void RunSinks ()
    {
        ListIterator iter;

        if (!allSinks.isEmpty ())
            iter = allSinks.listIterator ();
        else
            iter = allFilters.listIterator ();

        // go over all the sinks
        while (iter.hasNext ())
        {
            Operator sink;
            sink = (Operator) iter.next ();
            ASSERT (sink != null);

            // do bunch of work
            int i;
            for (i = 0; i < 10; i++)
            {
                sink.Work ();
            }
        }
    }

    void DrainChannels ()
    {
        while (!fullChannels.isEmpty ())
        {
                // empty any full channels:
            Iterator fullChannel;
                fullChannel = fullChannels.iterator ();

            Channel ch = (Channel) fullChannel.next ();
            ASSERT (ch != null);

            ch.GetSink ().Work ();
             }
    }


    void AddFullChannel (Channel channel)
    {
        fullChannels.add (channel);
    }

    void RemoveFullChannel (Channel channel)
        {
                fullChannels.remove (channel);
        }

    public static void PassOneData (Channel from, Channel to)
    {
        Class type = from.GetType ();
        ASSERT (type == to.GetType ());

        if (type == Integer.TYPE)
        {
            to.PushInt (from.PopInt ());
        } else
        if (type == Character.TYPE)
        {
            to.PushChar (from.PopChar ());
        } else
        if (type == Float.TYPE)
        {
            to.PushFloat (from.PopFloat ());
        } else
        if (type == Double.TYPE)
        {
            to.PushDouble (from.PopDouble ());
        } else {
            to.Push (from.Pop ());
        }
    }

    public static void DuplicateOneData (Channel from, Channel [] to)
    {
        Class type = from.GetType ();
        ASSERT (to != null && type == to[0].GetType ());

        if (type == Integer.TYPE)
        {
            int data = from.PopInt ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .PushInt (data);
            }
        } else
        if (type == Character.TYPE)
        {
            char data = from.PopChar ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .PushChar (data);
            }
        } else
        if (type == Float.TYPE)
        {
            float data = from.PopFloat ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .PushFloat (data);
            }
        } else
        if (type == Double.TYPE)
        {
            double data = from.PopDouble ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .PushDouble (data);
            }
        } else {
            Object data = from.Pop ();

            int indx;
            for (indx = to.length - 1; indx >= 0; indx--)
            {
                to [indx] .Push (data);
            }
        }
    }

    // send a message to a handler that returns <stub> within <delay>
    // units of my input/output (to be specified more clearly...)
    public void SendMessage(MessageStub stub, int delay) {}

    // send a message to a handler that returns <stub> at the earliest
    // convenient time.
    public void SendMessage(MessageStub stub) {}

    protected static class MessageStub
    {
        private static MessageStub STUB = new MessageStub();
        private MessageStub() {}
    }

    // a prototype work function
    void Work () { }

    // this function will take care of all appropriate calls to init:
    void SetupOperator ()
    {
        // don't re-initialize
        if (initialized) return;

        ASSERT (initParams != null);

        if (initParams.GetParamName ().equals("")) Init ();
        if (initParams.GetParamName ().equals("int")) Init (initParams.GetIntParam ("n"));
        if (initParams.GetParamName ().equals("float")) Init (initParams.GetFloatParam ("f"));
        if (initParams.GetParamName ().equals("String")) Init (initParams.GetStringParam ("str"));
        if (initParams.GetParamName ().equals("ParameterContainer")) Init ((ParameterContainer) initParams.GetObjParam ("params"));

        ConnectGraph ();
        initialized = true;
    }

    public void ConnectGraph ()
    {
        ASSERT (false);
    }

    // ------------------------------------------------------------------
    // ------------------ all graph related functions -------------------
    // ------------------------------------------------------------------

    // get an IO field (input or output)
    // returns null if none present
    Channel[] GetIOFields (String fieldName)
    {
        ASSERT (fieldName == "input" || fieldName == "output");

        Channel fieldsInstance [] = null;

        try
        {
            Class thisClass = this.getClass ();
            ASSERT (thisClass != null);

            Field ioField;
            ioField  = thisClass.getField (fieldName);

            Object fieldValue = ioField.get (this);

            if (ioField.getType ().isArray ())
            {
                fieldsInstance = (Channel []) fieldValue;
            } else {
                fieldsInstance = new Channel [1];
                fieldsInstance [0] = (Channel) fieldValue;

                if (fieldsInstance [0] == null) fieldsInstance = null;
            }
        }
        catch (NoSuchFieldException noError)
        {
            // do not do anything here, this is NOT an error!
        }
        catch (Throwable error)
        {
            // this is all the other errors:
            error.getClass ();
            ASSERT (false);
        }

        return fieldsInstance;
    }

    Channel GetIOField (String fieldName, int fieldIndex)
    {
        Channel field = null;

        {
            Channel fieldInstance[];
            fieldInstance = GetIOFields (fieldName);

            if (fieldInstance != null)
            {
                ASSERT (fieldInstance.length > fieldIndex);
                field = fieldInstance [fieldIndex];
            }
        }

        return field;
    }

    void SetIOField (String fieldName, int fieldIndex, Channel newChannel)
    {
        ASSERT (fieldName == "input" || fieldName == "output");

        Channel fieldsInstance [];

        try
        {
            Class thisClass = this.getClass ();
            ASSERT (thisClass != null);

            Field ioField;
            ioField  = thisClass.getField (fieldName);

            if (ioField.getType () == newChannel.getClass ())
            {
                ASSERT (fieldIndex == 0);
                ioField.set (this, newChannel);
            } else {
                fieldsInstance = (Channel []) ioField.get (this);
                ASSERT (fieldsInstance != null);
                ASSERT (fieldsInstance.length > fieldIndex);

                fieldsInstance [fieldIndex] = newChannel;
            }

        }
        catch (Throwable error)
        {
            // this is all the other errors:
            ASSERT (false);
        }
    }
}
