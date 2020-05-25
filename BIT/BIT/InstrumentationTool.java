package BIT;

import BIT.highBIT.*;
import java.io.*;
import java.util.*;

import BIT.InstrumentationThreadStatistics;
import awsclient.AmazonDynamoDBSample;

public class InstrumentationTool {

	private static PrintStream out = null;
	private static HashMap<Long, InstrumentationThreadStatistics> threadStore = new HashMap<Long, InstrumentationThreadStatistics>();

	/*
	 * main reads in all the files class files present in the input directory,
	 * instruments them, and outputs them to the specified output directory.
	 */
	public static void main(String argv[]) {
		File in_dir = new File(argv[0]);
		File out_dir = new File(argv[1]);

		String filelist[] = in_dir.list();

		for (int i = 0; i < filelist.length; i++) {
			String filename = filelist[i];
			if (filename.endsWith(".class")) {

				String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
				String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
				ClassInfo ci = new ClassInfo(in_filename);

				for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements();) {
					Routine routine = (Routine) e.nextElement();

					// DYNAMIC
					routine.addBefore("BIT/InstrumentationTool", "dynMethodCount", new Integer(1));

					for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements();) {
						BasicBlock bb = (BasicBlock) b.nextElement();
						bb.addBefore("BIT/InstrumentationTool", "dynInstrCount", new Integer(bb.size()));
					} 

				}

				ci.write(out_filename); // do this only once at end of all instrumenting
			}
		}
	}

	public static synchronized long getThreadId() {
		return Thread.currentThread().getId();
	}

	public static synchronized void dynInstrCount(int incr) {
		threadStore.get(getThreadId()).dynInstrCount(incr);
	}

	public static synchronized void dynMethodCount(int incr) {
		threadStore.get(getThreadId()).dynMethodCount(incr);
	}

	// This method is called before the solver starts
	public static synchronized void saveParams(String[] params) {
		long threadId = getThreadId();
		threadStore.put(threadId, new InstrumentationThreadStatistics(threadId, params));
		return;
	}

	// This is called after solver is done
	// Logs statistics from solver
	public static synchronized void finalizeMetricAndUpdateInDB() {
		long threadId = getThreadId();
		AmazonDynamoDBSample.updateSudokuDynamoDB("cnv_sudoku", threadStore.get(threadId));
		threadStore.remove(threadId);
		return;
	}
}
