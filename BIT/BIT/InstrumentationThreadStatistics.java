package BIT;

import BIT.highBIT.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

public class InstrumentationThreadStatistics {
	private static final long DYNAMIC_BB_COUNT_MAX = Long.parseLong("3837633000");
	private static final long DYNAMIC_BB_COUNT_MIN = 85579620;
	private static final long DYNAMIC_METHOD_COUNT_MAX = 36783;
	private static final long DYNAMIC_METHOD_COUNT_MIN = 747;

	// General
	long start_time;
	long threadId;
	String[] requestParams;
	Date date;
	DateFormat dateFormat;

	// StatisticsTool
	int dyn_method_count;
	long dyn_bb_count;

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

	static float normalize_value(long value, long min, long max) {
		return (float) (value - min) / (max - min);
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
	
	public String getMetric() {
		return String.valueOf(
			(normalize_value(this.dyn_bb_count, DYNAMIC_BB_COUNT_MIN, DYNAMIC_BB_COUNT_MAX) + 
			normalize_value(this.dyn_method_count, DYNAMIC_METHOD_COUNT_MIN, DYNAMIC_METHOD_COUNT_MAX)) / 2			
		);
	}

	public String getMicroSecondsUsed() {
		return String.valueOf(((double) System.nanoTime() - this.start_time) / 1000000);
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
}


