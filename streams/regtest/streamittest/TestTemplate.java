/**
 * Template framework for writing test cases using JUnit.
 * 1. Copy to new java file, and find/replace TestTemplate with new name.
 * 2. Add an entry in AllTests.java for this new suite
 * 3. Add test code in void methods like testSimple
 * 4. Add a line in suite() with the new test method name
 *
 * You can then use the CompilerInterface compiler to run compiler sessions.
 * $Id: TestTemplate.java,v 1.2 2002-06-21 20:03:54 aalamb Exp $
 **/
package streamittest;

import junit.framework.*;


public class TestTemplate extends StreamITTestCase {
    static String STREAM_ROOT = null;
    /**
     * Creates a new TestTemplate which will use the compiler options
     * specified by flags (defined in CompilerInterface.java).
     **/
    public TestTemplate(String name, int flags) {
	super (name,flags);
	if (STREAM_ROOT == null) {
	    STREAM_ROOT = getStreamITRoot();
	}
    }

    public static Test suite(int flags) {
	TestSuite suite = new TestSuite();
	suite.addTest(new TestTemplate("testSimple", flags));
	
	return suite;
    }
    
    public void testSimple() {
	assertTrue("was true", true);
    }

}
