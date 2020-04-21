package BIT;

import BIT.highBIT.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import functions.Logger;

class StatisticsBranch  // needed here because i could not import it properly
{
	String class_name_;
	String method_name_;
	int pc_;
	int taken_;
	int not_taken_;

	public StatisticsBranch(String class_name, String method_name, int pc) 
		{
			class_name_ = class_name;
			method_name_ = method_name;
			pc_ = pc;
			taken_ = 0;
			not_taken_ = 0;
		}
	
	public void incrTaken()
		{
			taken_++;
		}

	public void incrNotTaken() 
		{
			not_taken_++;
		}
}


class InstrumentationThreadStatistics {
	// General
    long start_time;
    
    //ICount
    int i_count; // Instructions
    int b_count; // Basic Blocks
    int m_count; // methods
    
    // StatisticsTool
	int dyn_method_count;
	int dyn_bb_count;
	int dyn_instr_count;
	
	int newcount;
	int newarraycount;
	int anewarraycount;
	int multianewarraycount;
	
	int loadcount;
	int storecount;
	int fieldloadcount;
	int fieldstorecount;
	
	StatisticsBranch[] branch_info;
	int branch_number;
	int branch_pc;
	String branch_class_name;
	String branch_method_name;
    

    InstrumentationThreadStatistics() {
        this.start_time = System.nanoTime();
        
        this.i_count = 0;
        this.b_count = 0;
        this.m_count = 0;
        
        this.dyn_bb_count = 0;
        this.dyn_instr_count = 0;
        this.dyn_method_count = 0;
        
        this.newcount = 0;
        this.newarraycount = 0;
        this.anewarraycount = 0;
        this.multianewarraycount = 0;
        
        this.loadcount = 0;
        this.storecount = 0;
        this.fieldloadcount = 0;
        this.fieldstorecount = 0;
    }

    String resultToLog() {
    	
//    	String branch_info_log = "";
//    	for (int i = 0; i < branch_info.length; i++) {
//			if (branch_info[i] != null) {
//				//branch_info[i].print();
//				branch_info_log += branch_info[i].class_name_ + '\t' + branch_info[i].method_name_ + '\t' + branch_info[i].pc_ + '\t' + branch_info[i].taken_ + '\t' + branch_info[i].not_taken_ + "\n";
//			}
//		}
    	
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date();
        long timeUsed = System.nanoTime() - this.start_time;
        double timeUsedSeconds = (double) timeUsed / 1000000000;
        return "\n==============================================" +
            "\nLogged at: " + dateFormat.format(date) +
            "\nSeconds used: " + timeUsedSeconds +
            //"\nInstructions: " + this.i_count +
            //"\nBasic blocks: " + this.b_count +
            //"\nMethods: " + this.m_count +
            "\nBasic Counts-------------------------------" +
            "\nDynamic basic blocks: " + this.dyn_bb_count + 
            "\nDynamic instructions: " + this.dyn_instr_count + 
            "\nDynamic Methods: " + this.dyn_method_count +
            "\nAllocations--------------------------------" +
            "\nNew: " + this.newcount +
            "\nNew array: " + this.newarraycount +
            "\nA new array: " + this.anewarraycount +
            "\nMulti a new array: " + this.multianewarraycount +
            "\nLoad/Store---------------------------------" +
            "\nLoad count: " + this.loadcount +
            "\nStore count: " + this.storecount +
            "\nField load count: " + this.fieldloadcount +
            "\nField store count: " + this.fieldstorecount +
            "\nBranch summary-------------------------------"
//            "\nCLASS NAME" + '\t' + "METHOD" + '\t' + "PC" + '\t' + "TAKEN" + '\t' + "NOT_TAKEN" +
//            branch_info_log            // currently looks like shit but at least works
            ;
    }

    void count(int incr) {
        this.i_count += incr;
        this.b_count++;
    }

    void mcount() {
        this.m_count++;
    }
    
    void dynInstrCount(int incr) {
		this.dyn_instr_count += incr;
		this.dyn_bb_count++;
	}
    
	void dynMethodCount(int incr) {
		this.dyn_method_count++;
	}
	
	void allocCount(int type) {
		
		switch (type) {
		
		case InstructionTable.NEW:
			this.newcount++;
			break;
		case InstructionTable.newarray:
			this.newarraycount++;
			break;
		case InstructionTable.anewarray:
			this.anewarraycount++;
			break;
		case InstructionTable.multianewarray:
			this.multianewarraycount++;
			break;
		}
	}

	void LSFieldCount(int type) {
		
		if (type == 0)
			this.fieldloadcount++;
		else
			this.fieldstorecount++;
	}
	
	void LSCount(int type) {
		
		if (type == 0)
			this.loadcount++;
		else
			this.storecount++;
	}

	void setBranchMethodName(String name) {
		this.branch_method_name = name;
	}
	
	void setBranchClassName(String name) {
		this.branch_class_name = name;
	}
	
	void setBranchPC(int pc) {
		this.branch_pc = pc;
	}
	
	void branchInit(int n) {
		if (this.branch_info == null) {
			this.branch_info = new StatisticsBranch[n];
		}
	}
	
	void updateBranchNumber(int n) {
		this.branch_number = n;
		if (this.branch_info[branch_number] == null) {
			this.branch_info[this.branch_number] = new StatisticsBranch(this.branch_class_name, this.branch_method_name, this.branch_pc);
		}
	}
	
	void updateBranchOutcome(int br_outcome) {
		if (br_outcome == 0) {
			this.branch_info[this.branch_number].incrNotTaken();
		} else {
			this.branch_info[this.branch_number].incrTaken();
		}
	}
}

public class InstrumentationTool {
	
    private static PrintStream out = null;
    //private static int i_count = 0, b_count = 0, m_count = 0; // in use??
    private static HashMap<Long, InstrumentationThreadStatistics> threadStore = new HashMap<Long, InstrumentationThreadStatistics>();
    
    /* main reads in all the files class files present in the input directory,
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
					
					//DYNAMIC
					routine.addBefore("BIT/InstrumentationTool", "dynMethodCount", new Integer(1));
					
					for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements();) {
						BasicBlock bb = (BasicBlock) b.nextElement();
						bb.addBefore("BIT/InstrumentationTool", "dynInstrCount", new Integer(bb.size()));
					} //DYNAMIC
					
					// ALLOC
					InstructionArray instructions = routine.getInstructionArray();
					
					for (Enumeration instrs = instructions.elements(); instrs.hasMoreElements();) {
						Instruction instr = (Instruction) instrs.nextElement();
						int opcode = instr.getOpcode();
						
						if ((opcode == InstructionTable.NEW) || (opcode == InstructionTable.newarray)
								|| (opcode == InstructionTable.anewarray)
								|| (opcode == InstructionTable.multianewarray)) {
							instr.addBefore("BIT/InstrumentationTool", "allocCount", new Integer(opcode));
						}
					} // ALLOC
					
					//LOAD STORE
					for (Enumeration instrs = (routine.getInstructionArray()).elements(); instrs.hasMoreElements();) {
						Instruction instr = (Instruction) instrs.nextElement();
						int opcode = instr.getOpcode();
						
						if (opcode == InstructionTable.getfield)
							instr.addBefore("BIT/InstrumentationTool", "LSFieldCount", new Integer(0));
						else if (opcode == InstructionTable.putfield)
							instr.addBefore("BIT/InstrumentationTool", "LSFieldCount", new Integer(1));
						else {
							short instr_type = InstructionTable.InstructionTypeTable[opcode];
							if (instr_type == InstructionTable.LOAD_INSTRUCTION) {
								instr.addBefore("BIT/InstrumentationTool", "LSCount", new Integer(0));
							} else if (instr_type == InstructionTable.STORE_INSTRUCTION) {
								instr.addBefore("BIT/InstrumentationTool", "LSCount", new Integer(1));
							}
						}
					} //LOAD STORE
				}
								
				ci.write(out_filename); // do this only once at end of all instrumenting!
			}
		} 
    }
    
   

    public static synchronized void count(int incr) {
        threadStore.get(Thread.currentThread().getId()).count(incr);
    }

    public static synchronized void mcount(int incr) {
       	threadStore.get(Thread.currentThread().getId()).mcount();
    }
    
    public static synchronized void dynInstrCount(int incr) {
    	threadStore.get(Thread.currentThread().getId()).dynInstrCount(incr);
	}
    
	public static synchronized void dynMethodCount(int incr) {
    	threadStore.get(Thread.currentThread().getId()).dynMethodCount(incr);
	}
		
	public static synchronized void allocCount(int type) {
    	threadStore.get(Thread.currentThread().getId()).allocCount(type);
	}
    
	public static synchronized void LSFieldCount(int type) {
    	threadStore.get(Thread.currentThread().getId()).LSFieldCount(type);
	}
	
	public static synchronized void LSCount(int type) {
    	threadStore.get(Thread.currentThread().getId()).LSCount(type);
	}
	
	public static synchronized void setBranchClassName(String name) {
    	threadStore.get(Thread.currentThread().getId()).setBranchClassName(name);
	}
	
	public static synchronized void setBranchMethodName(String name) {
    	threadStore.get(Thread.currentThread().getId()).setBranchMethodName(name);
	}
	
	public static synchronized void setBranchPC(int pc) {
    	threadStore.get(Thread.currentThread().getId()).setBranchPC(pc);
	}
	
	public static synchronized void branchInit(int n) {
    	threadStore.get(Thread.currentThread().getId()).branchInit(n);
	}
	
	public static synchronized void updateBranchNumber(int n) {
    	threadStore.get(Thread.currentThread().getId()).updateBranchNumber(n);
	}
	
	public static synchronized void updateBranchOutcome(int br_outcome) {
    	threadStore.get(Thread.currentThread().getId()).updateBranchOutcome(br_outcome);
	}
	
	// Calls Logger to print results in log file
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