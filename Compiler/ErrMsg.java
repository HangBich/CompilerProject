
class ErrMsg {
	private static boolean err = false;
    static void fatal(int lineNum, int charNum, String msg) {
		err = true;
        System.err.println(lineNum + ":" + charNum + " ***ERROR*** " + msg);
    }
    static void warn(int lineNum, int charNum, String msg) {
        System.err.println(lineNum + ":" + charNum + " ***WARNING*** " + msg);
    }
	static boolean getErr() {
		return err;
	}
}
