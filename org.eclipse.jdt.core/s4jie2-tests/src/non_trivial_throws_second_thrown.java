class Main {
	
	/**
	 * @throws IllegalArgumentException | bar == 11
	 * @throws ArithmeticException | bar == 11
	 */
	public static void foo(int bar) {
		if (bar == 11)
			throw new ArithmeticException();
	}
	
	public static void main(String[] args) {
		Main.foo(11);
	}
}