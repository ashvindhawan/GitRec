package gitlet;

import java.io.IOException;
import java.util.ArrayList;

/** Driver class for Gitlet, the tiny stupid version-control
 * system. Read strings from user and direct them to classes
 * based on which command is entered. Collaborated in design with
 * Henry Kasa (henrykasa@berkeley.edu).
 *  @author Ashvin Dhawan
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        ArrayList<String> arguments = new ArrayList<>();
        if (args == null || args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String operator = args[0];
        for (int i = 1; i < args.length; i++) {
            arguments.add(args[i]);
        }
        Gitlet gitlet = new Gitlet();
        gitlet.execute(operator, arguments);
    }

}
