package graph;

import java.util.Date;

public class MyLogger {

	static private int level=3;

	public static void debug(String message) {
		if (level<2)
			System.out.println(new Date() + " DEBUG: " + message);
	}

	public static void info(String message) {
		if (level<3)
			System.out.println(new Date() + " INFO: " + message);
	}
	public static void important(String message) {
		if (level<4)
			System.out.println(new Date() + " INFO: " + message);
	}

	public static void warn(String message) {
		if (level<5)
			System.out.println(new Date() + " WARN: " + message);
	}

	public static void error(String message) {
		if (level<6)
			System.out.println(new Date() + " ERROR: " + message);
	}

	public static String toStringMeaningfulVectorData(Double[] vector) {
		String s = "";
		//        for (int i = 0 ; i < vector.length ; ++i) {
		//            if(vector[i] != 0.0)
		//                s+="[" + i + "]={" + vector[i] + "}, ";
		//        }
		return s;
	}
}
