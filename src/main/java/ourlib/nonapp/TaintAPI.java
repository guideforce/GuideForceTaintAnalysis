package ourlib.nonapp;

public class TaintAPI {
	public static String getTaintedString() {
		return "tainted";
	}

	public static void emitA() {
		System.out.println("A");
	}

	public static void emitB() {
		System.out.println("B");
	}

	public static void emitC() {
		System.out.println("C");
	}

	public static void outputString(String s) {
		// s should not be tainted!
		System.out.println(s);
	}
}
