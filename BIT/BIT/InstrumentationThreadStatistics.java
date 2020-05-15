package BIT;

import BIT.highBIT.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

public class InstrumentationThreadStatistics {
	// General
	long start_time;
	long threadId;
	String[] requestParams;
	Date date;
	DateFormat dateFormat;

	// StatisticsTool
	int dyn_method_count;
	long dyn_bb_count;

	int anewarraycount;
	int multianewarraycount;

	long storecount;

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

		this.dyn_bb_count = 0;
		this.dyn_method_count = 0;

		this.anewarraycount = 0;
		this.multianewarraycount = 0;

		this.storecount = 0;

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
	
	public String get_dyn_bb_count() {
		return String.valueOf(this.dyn_bb_count);
	}
	public String get_dyn_method_count() {
		return String.valueOf(this.dyn_method_count);
	}
	public String get_anewarraycount() {
		return String.valueOf(this.anewarraycount);
	}
	public String get_multianewarraycount() {
		return String.valueOf(this.multianewarraycount);
	}
	public String get_storecount() {
		return String.valueOf(this.storecount);
	}

	public String getMetric() {
		return String.valueOf(
			this.dyn_bb_count +
			this.dyn_method_count +
			this.anewarraycount +
			this.multianewarraycount +
			this.storecount 
		);
	}

	public String getMicroSecondsUsed() {
		return String.valueOf(((double) System.nanoTime() - this.start_time) / 1000000);
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
				"\nDynamic Methods: " + this.dyn_method_count + 
				"\nAllocations--------------------------------" + 
				"\nA new array: " + this.anewarraycount + 
				"\nMulti a new array: "	+ this.multianewarraycount + 
				"\nLoad/Store---------------------------------" + 
				"\nStore count: " + this.storecount  
		;
	}

	public String logParams() {
		String out = "";
		for (String param : this.requestParams) {
			out += param + " ";
		}
		return out;
	}

	void dynInstrCount(int incr) {
		this.dyn_bb_count++;
	}

	void dynMethodCount(int incr) {
		this.dyn_method_count++;
	}

	void allocCount(int type) {

		switch (type) {

		case InstructionTable.anewarray:
			this.anewarraycount++;
			break;
		case InstructionTable.multianewarray:
			this.multianewarraycount++;
			break;
		}
	}

	void LSCount(int type) {
		this.storecount++;
	}

	
}

