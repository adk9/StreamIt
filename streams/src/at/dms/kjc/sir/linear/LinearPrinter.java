package at.dms.kjc.sir.linear;

/**
 * Control point for printing messages generated by the LinearAnalysis pass.
 * The purpose of this class is to have an easy way to leave in statements that
 * generate verbose output but be able to disable the output from appearing if
 * not requested (via the --debug compiler flag). **/
public class LinearPrinter {
    /** flag to control output generation. **/
    private static boolean outputEnabled = false;
    /** Return flag status. **/
    public static boolean getOutput() {
	return outputEnabled;
    }
    /** Set output flag status. **/
    public static void setOutput(boolean outFlag) {
	outputEnabled = outFlag;
    }
    /** Prints message to stderr if flag is set to true. **/
    public static void println(String message) {
	if (outputEnabled) {
	    System.err.println(message);
	}
    }
    /** Prints message to stderr if flag is set to true. **/
    public static void print(String message) {
	if (outputEnabled) {
	    System.err.print(message);
	}
    }
    /** Prints message with WARNING: prefix to stderr if flag is set to true. **/
    public static void warn(String message) {
	System.err.println("WARNING: " + message);
    }
}
