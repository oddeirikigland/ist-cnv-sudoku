package BIT;

import BIT.highBIT.*;
import java.io.*;
import java.util.*;

import BIT.InstrumentationThreadStatistics;
import functions.Logger;
import awsclient.AmazonDynamoDBSample;


public class InstrumentationTool {
	
	//'dyn_instr_count', 'loadcount', 'fieldstorecount', 'newcount', 'newarraycount', 'fieldloadcount'

	private static PrintStream out = null;
	// private static int i_count = 0, b_count = 0, m_count = 0; // in use??
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
					} // DYNAMIC

					// ALLOC
					InstructionArray instructions = routine.getInstructionArray();

					for (Enumeration instrs = instructions.elements(); instrs.hasMoreElements();) {
						Instruction instr = (Instruction) instrs.nextElement();
						int opcode = instr.getOpcode();

						if ((opcode == InstructionTable.anewarray) || (opcode == InstructionTable.multianewarray)) {
							instr.addBefore("BIT/InstrumentationTool", "allocCount", new Integer(opcode));
						}
					} // ALLOC

					// LOAD STORE
					for (Enumeration instrs = (routine.getInstructionArray()).elements(); instrs.hasMoreElements();) {
						Instruction instr = (Instruction) instrs.nextElement();
						int opcode = instr.getOpcode();

//						if (opcode == InstructionTable.getfield)
//							instr.addBefore("BIT/InstrumentationTool", "LSFieldCount", new Integer(0));
//						else if (opcode == InstructionTable.putfield)
//							instr.addBefore("BIT/InstrumentationTool", "LSFieldCount", new Integer(1));
//						else {
						short instr_type = InstructionTable.InstructionTypeTable[opcode];
//							if (instr_type == InstructionTable.LOAD_INSTRUCTION) {
//								instr.addBefore("BIT/InstrumentationTool", "LSCount", new Integer(0));
//							} else
						if (instr_type == InstructionTable.STORE_INSTRUCTION) {
							instr.addBefore("BIT/InstrumentationTool", "LSCount", new Integer(1));
						}
//						}
					} // LOAD STORE
				}

				ci.write(out_filename); // do this only once at end of all instrumenting!
			}
		}
	}

	public static synchronized long getThreadId() {
		return Thread.currentThread().getId();
	}

//	public static synchronized void count(int incr) {
//		threadStore.get(getThreadId()).count(incr);
//	}
//
//	public static synchronized void mcount(int incr) {
//		threadStore.get(getThreadId()).mcount();
//	}

	public static synchronized void dynInstrCount(int incr) {
		threadStore.get(getThreadId()).dynInstrCount(incr);
	}

	public static synchronized void dynMethodCount(int incr) {
		threadStore.get(getThreadId()).dynMethodCount(incr);
	}

	public static synchronized void allocCount(int type) {
		threadStore.get(getThreadId()).allocCount(type);
	}

//	public static synchronized void LSFieldCount(int type) {
//		threadStore.get(getThreadId()).LSFieldCount(type);
//	}

	public static synchronized void LSCount(int type) {
		threadStore.get(getThreadId()).LSCount(type);
	}

	
	// Calls Logger to print results in log file
	public static synchronized void printToFile(long threadId) {
		Logger.logToFile(threadStore.get(threadId).resultToLog());
		AmazonDynamoDBSample.updateSudokuDynamoDB("cnv_sudoku", threadStore.get(threadId));
	}

	// This method is called before the solver starts
	public static synchronized Integer checkParams(String[] params) {
		long threadId = getThreadId();
		threadStore.put(threadId, new InstrumentationThreadStatistics(threadId, params));

		// TODO: return metrics based on these params
		return 333;
	}

	// This is called after solver is done
	// Logs statistics from solver
	public static synchronized Integer result() {
		long threadId = getThreadId();
		printToFile(threadId);
		threadStore.remove(threadId);
		return 123;
	}
}
