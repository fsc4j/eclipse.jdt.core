class Main {
	
	/**
	 * @throws IllegalArgumentException | bar == 0
	 * @throws ArithmeticException | bar == 5
	 */
	public static void foo(int bar) {
		throw new ArithmeticException();
	}
	
	public static void main(String[] args) {
		Main.foo(1);
	}
}