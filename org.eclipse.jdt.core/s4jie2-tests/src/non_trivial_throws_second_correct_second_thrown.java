class Main {
	
	/**
	 * @throws IllegalArgumentException | bar == 7
	 * @throws ArithmeticException | bar == 15
	 */
	public static void foo(int bar) {
		throw new ArithmeticException();
	}
	
	public static void main(String[] args) {
		Main.foo(15);
	}
}