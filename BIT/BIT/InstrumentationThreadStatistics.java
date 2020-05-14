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
//'dyn_instr_count', 'loadcount', 'fieldstorecount', 'newcount', 'newarraycount', 'fieldloadcount'

public class InstrumentationThreadStatistics {
	// General
	long start_time;
	long threadId;
	String[] requestParams;
	Date date;
	DateFormat dateFormat;

	// ICount
	//int i_count; // Instructions
//	int b_count; // Basic Blocks
//	int m_count; // methods

	// StatisticsTool
	int dyn_method_count;
	long dyn_bb_count;
//	long dyn_instr_count;

//	int newcount;
//	int newarraycount;
	int anewarraycount;
	int multianewarraycount;

//	long loadcount;
	long storecount;
//	int fieldloadcount;
//	int fieldstorecount;

	StatisticsBranch[] branch_info;
	int branch_number;
	int branch_pc;
	String branch_class_name;
	String branch_method_name;

	String s;
	String un;
	String n1;
	String n2;
	String i;

	public InstrumentationThreadStatistics(long threadId, String[] requestParams) {
		this.threadId = threadId;
		this.requestParams = requestParams;
		this.start_time = System.nanoTime();
		this.date = new Date();
		this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

//		this.i_count = 0;
//		this.b_count = 0;
//		this.m_count = 0;

		this.dyn_bb_count = 0;
//		this.dyn_instr_count = 0;
		this.dyn_method_count = 0;

//		this.newcount = 0;
//		this.newarraycount = 0;
		this.anewarraycount = 0;
		this.multianewarraycount = 0;

//		this.loadcount = 0;
		this.storecount = 0;
//		this.fieldloadcount = 0;
//		this.fieldstorecount = 0;

		this.s = "";
		this.un = "";
		this.n1 = "";
		this.n2 = "";
		this.i = "";
		this.updateRequestParams();
	}

	void updateRequestParams() {
		for (String param : this.requestParams) {
			String[] arrOfStr = param.split("=", 2);
			if (arrOfStr[0].equals("s")) this.s = String.valueOf(arrOfStr[1]);
			else if (arrOfStr[0].equals("un")) this.un = String.valueOf(arrOfStr[1]);
			else if (arrOfStr[0].equals("n1")) this.n1 = String.valueOf(arrOfStr[1]);
			else if (arrOfStr[0].equals("n2")) this.n2 = String.valueOf(arrOfStr[1]);
			else if (arrOfStr[0].equals("i")) this.i = String.valueOf(arrOfStr[1]);
			else System.out.println("Parameter with unvalid name");
		}
	}
	
	// Getters for DynamoDB, needs to be String as return type    
    public String getThreadId() {
        return String.valueOf(this.threadId);
	}
	
	public String getRequestDate() {
		return String.valueOf(this.date);
	}

	public String getS() {
		return this.s;
	}

	public String getUn() {
		return this.un;
	}

	public String getN1() {
		return this.n1;
	}

	public String getN2() {
		return this.n2;
	}

	public String getI() {
		return this.i;
	}
	
	//'dyn_instr_count', 'loadcount', 'fieldstorecount', 'newcount', 'newarraycount', 'fieldloadcount'


//	public String get_i_count() {
//		return String.valueOf(this.i_count);
//	}
//	public String get_b_count() {
//		return String.valueOf(this.b_count);
//	}
//	public String get_m_count() {
//		return String.valueOf(this.m_count);
//	}
	public String get_dyn_bb_count() {
		return String.valueOf(this.dyn_bb_count);
	}
//	public String get_dyn_instr_count() {
//		return String.valueOf(this.dyn_instr_count);
//	}
	public String get_dyn_method_count() {
		return String.valueOf(this.dyn_method_count);
	}
//	public String get_newcount() {
//		return String.valueOf(this.newcount);
//	}
//	public String get_newarraycount() {
//		return String.valueOf(this.newarraycount);
//	}
	public String get_anewarraycount() {
		return String.valueOf(this.anewarraycount);
	}
	public String get_multianewarraycount() {
		return String.valueOf(this.multianewarraycount);
	}
//	public String get_loadcount() {
//		return String.valueOf(this.loadcount);
//	}
	public String get_storecount() {
		return String.valueOf(this.storecount);
	}
//	public String get_fieldloadcount() {
//		return String.valueOf(this.fieldloadcount);
//	}
//	public String get_fieldstorecount() {
//		return String.valueOf(this.fieldstorecount);
//	}

	public String getMetric() {
		return String.valueOf(
//			this.i_count +
//			this.b_count +
//			this.m_count +
			this.dyn_bb_count +
//			this.dyn_instr_count +
			this.dyn_method_count +
//			this.newcount +
//			this.newarraycount +
			this.anewarraycount +
			this.multianewarraycount +
//			this.loadcount +
			this.storecount 
//			this.fieldloadcount +
//			this.fieldstorecount
		);
	}


	public String resultToLog() {

		long timeUsed = System.nanoTime() - this.start_time;
		double timeUsedSeconds = (double) timeUsed / 1000000000;
		return "\n==============================================" 	+	 
				"\nLogged at: " + this.dateFormat.format(this.date)			+ 
				"\nSeconds used: " + timeUsedSeconds +
				"\nThread ID: " + this.threadId +
				"\nRequest params: " + this.logParams() +
				"\nBasic Counts-------------------------------" + 
				"\nDynamic basic blocks: " + this.dyn_bb_count +
//				+ "\nDynamic instructions: " + this.dyn_instr_count + 
				"\nDynamic Methods: " + this.dyn_method_count + 
				"\nAllocations--------------------------------" + 
//				"\nNew: " + this.newcount + 
//				"\nNew array: "	+ this.newarraycount + 
				"\nA new array: " + this.anewarraycount + 
				"\nMulti a new array: "	+ this.multianewarraycount + 
				"\nLoad/Store---------------------------------" + 
//				"\nLoad count: " + this.loadcount + 
				"\nStore count: " + this.storecount  
//				"\nField load count: " + this.fieldloadcount + 
//				"\nField store count: " + this.fieldstorecount  
//				"\nBranch summary-------------------------------"
//            "\nCLASS NAME" + '\t' + "METHOD" + '\t' + "PC" + '\t' + "TAKEN" + '\t' + "NOT_TAKEN" +
//            branch_info_log            // currently looks like shit but at least works
		;
	}

	public String logParams() {
		String out = "";
		for (String param : this.requestParams) {
			out += param + " ";
		}
		return out;
	}

//	void count(int incr) {
//		this.i_count += incr;
//		this.b_count++;
//	}
//
//	void mcount() {
//		this.m_count++;
//	}

	void dynInstrCount(int incr) {
//		this.dyn_instr_count += incr;
		this.dyn_bb_count++;
	}

	void dynMethodCount(int incr) {
		this.dyn_method_count++;
	}

	void allocCount(int type) {

		switch (type) {

//		case InstructionTable.NEW:
//			this.newcount++;
//			break;
//		case InstructionTable.newarray:
//			this.newarraycount++;
//			break;
		case InstructionTable.anewarray:
			this.anewarraycount++;
			break;
		case InstructionTable.multianewarray:
			this.multianewarraycount++;
			break;
		}
	}
//
//	void LSFieldCount(int type) {
//
//		if (type == 0)
//			this.fieldloadcount++;
//		else
//			this.fieldstorecount++;
//	}

	void LSCount(int type) {
//
//		if (type == 0)
//			this.loadcount++;
//		else
		this.storecount++;
	}

	
}
