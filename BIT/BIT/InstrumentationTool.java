package BIT;

import BIT.highBIT.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import functions.Logger;


class InstrumentationThreadStatistics {
    long start_time;
    int i_count;
    int b_count;
    int m_count;

    InstrumentationThreadStatistics() {
        this.start_time = System.nanoTime();
        this.i_count = 0;
        this.b_count = 0;
        this.m_count = 0;
    }

    String resultToLog() {
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date();
        long timeUsed = System.nanoTime() - this.start_time;
        return "\n==============================================" +
            "\nLogged at: " + dateFormat.format(date) +
            "\nTime used: " + timeUsed +
            "\nInstructions: " + this.i_count +
            "\nBasic blocks: " + this.b_count +
            "\nMethods: " + this.m_count;
    }

    void count(int incr) {
        this.i_count += incr;
        this.b_count++;
    }

    void mcount() {
        this.m_count++;
    }
}

public class InstrumentationTool {
    private static PrintStream out = null;
    private static int i_count = 0, b_count = 0, m_count = 0;
    private static HashMap<Long, InstrumentationThreadStatistics> threadStore = new HashMap<Long, InstrumentationThreadStatistics>();
    
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
                ci.write(argv[1] + System.getProperty("file.separator") + infilename);
            }
        }
    }

    public static synchronized void count(int incr) {
        threadStore.get(Thread.currentThread().getId()).count(incr);
    }

    public static synchronized void mcount(int incr) {
        threadStore.get(Thread.currentThread().getId()).mcount();
    }

    public static synchronized void printToFile(long threadId) {
        Logger.logToFile(threadStore.get(threadId).resultToLog());
    }


    // This method is called before the solver starts
    public static synchronized Integer checkParams(String[] params) {
        System.out.println("This is the params from request in WebServer:");
        for (String param : params) {
            System.out.println(param);
        }
        threadStore.put(Thread.currentThread().getId(), new InstrumentationThreadStatistics());

        // TODO: return metrics based on these params
        return 333;
    }


    // This is called after solver is done
    // Logs statistics from solver
    public static synchronized Integer result() {
        long threadId = Thread.currentThread().getId();
        printToFile(threadId);
        threadStore.remove(threadId);
        return 123;
    }
}