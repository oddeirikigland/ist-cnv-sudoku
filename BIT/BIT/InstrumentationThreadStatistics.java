package BIT;

import BIT.highBIT.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

class StatisticsBranch { // needed here because i could not import it properly
	String class_name_;
	String method_name_;
	int pc_;
	int taken_;
	int not_taken_;

	public StatisticsBranch(String class_name, String method_name, int pc) {
		class_name_ = class_name;
		method_name_ = method_name;
		pc_ = pc;
		taken_ = 0;
		not_taken_ = 0;
	}

	public void incrTaken() {
		taken_++;
	}

	public void incrNotTaken() {
		not_taken_++;
	}
}

public class InstrumentationThreadStatistics {
	// General
	long start_time;
	long threadId;
	String[] requestParams;

	// ICount
	int i_count; // Instructions
	int b_count; // Basic Blocks
	int m_count; // methods

	// StatisticsTool
	int dyn_method_count;
	long dyn_bb_count;
	long dyn_instr_count;

	int newcount;
	int newarraycount;
	int anewarraycount;
	int multianewarraycount;

	long loadcount;
	long storecount;
	int fieldloadcount;
	int fieldstorecount;

	StatisticsBranch[] branch_info;
	int branch_number;
	int branch_pc;
	String branch_class_name;
	String branch_method_name;

	public InstrumentationThreadStatistics(long threadId, String[] requestParams) {
		this.threadId = threadId;
		this.requestParams = requestParams;
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
    
    public String getThreadId() {
        return String.valueOf(this.threadId);
    }

	public String resultToLog() {

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
		return "\n==============================================" 	+	 
				"\nLogged at: " + dateFormat.format(date)			+ 
				"\nSeconds used: " + timeUsedSeconds +
				"\nThread ID: " + this.threadId +
				"\nRequest params: " + this.logParams(this.requestParams) +
				// "\nInstructions: " + this.i_count +
				// "\nBasic blocks: " + this.b_count +
				// "\nMethods: " + this.m_count +
				"\nBasic Counts-------------------------------" + 
				"\nDynamic basic blocks: " + this.dyn_bb_count
				+ "\nDynamic instructions: " + this.dyn_instr_count + 
				"\nDynamic Methods: " + this.dyn_method_count + 
				"\nAllocations--------------------------------" + 
				"\nNew: " + this.newcount + 
				"\nNew array: "	+ this.newarraycount + 
				"\nA new array: " + this.anewarraycount + 
				"\nMulti a new array: "	+ this.multianewarraycount + 
				"\nLoad/Store---------------------------------" + 
				"\nLoad count: " + this.loadcount + 
				"\nStore count: " + this.storecount + 
				"\nField load count: " + this.fieldloadcount + 
				"\nField store count: " + this.fieldstorecount  
//				"\nBranch summary-------------------------------"
//            "\nCLASS NAME" + '\t' + "METHOD" + '\t' + "PC" + '\t' + "TAKEN" + '\t' + "NOT_TAKEN" +
//            branch_info_log            // currently looks like shit but at least works
		;
	}

	String logParams(String[] params) {
		String out = "";
		for (String param : params) {
			out += param + " ";
		}
		return out;
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
			this.branch_info[this.branch_number] = new StatisticsBranch(this.branch_class_name, this.branch_method_name,
					this.branch_pc);
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