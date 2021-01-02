package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.io.IOException;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Ashvin Dhawan
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }


    /** a dummy test to avoid compaint. */
    @Test
    public void initialTest() throws IOException {
    }
}


