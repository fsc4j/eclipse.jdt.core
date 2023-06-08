class Main {
	
	/**
	 * @throws IllegalArgumentException | bar == 0
	 * @throws ArithmeticException | bar == 33
	 */
	public static void foo(int bar) {
		return;
	}
	
	public static void main(String[] args) {
		Main.foo(33);
	}
}