package BIT;

import BIT.highBIT.*;
import java.io.*;
import java.util.*;

import functions.Logger;


public class InstrumentationTool {
    private static PrintStream out = null;
    private static int i_count = 0, b_count = 0, m_count = 0;
    
    /* main reads in all the files class files present in the input directory,
     * instruments them, and outputs them to the specified output directory.
     */
    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        String infilenames[] = file_in.list();
        
        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            if (infilename.endsWith(".class")) {
				// create class info object
				ClassInfo ci = new ClassInfo(argv[0] + System.getProperty("file.separator") + infilename);
				
                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
					routine.addBefore("BIT/InstrumentationTool", "mcount", new Integer(1));
                    
                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("BIT/InstrumentationTool", "count", new Integer(bb.size()));
                    }
                }
                ci.addAfter("BIT/InstrumentationTool", "printToFile", ci.getClassName());
                ci.write(argv[1] + System.getProperty("file.separator") + infilename);
            }
        }
    }

    public static synchronized void count(int incr) {
        i_count += incr;
        b_count++;
    }

    public static synchronized void mcount(int incr) {
		m_count++;
    }

    public static synchronized void printToFile(String foo) {
        String line = i_count + " instructions in " + b_count + " basic blocks were executed in " + m_count + " methods.";
        Logger.logToFile(line);
    }

    // To test connection with WebServer
    // This is called after solver is done
    public static Integer result() {
        printToFile("foo");
        return i_count;
    }

    public static synchronized Integer checkParams(String[] params, long threadId) {
        System.out.println("This is the params from request in WebServer:");
        for (String param : params) {
            System.out.println(param);
        }
        System.out.println("Thread # " + threadId + " is doing this task");

        // TODO: return metrics based on these params
        return 333;
    }
}