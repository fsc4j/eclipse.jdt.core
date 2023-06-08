class Main {
	
	/**
	 * @throws IllegalArgumentException | bar == 0
	 * @throws ArithmeticException | bar == 1
	 */
	public static void foo(int bar) {
		throw new IllegalArgumentException();
	}
	
	public static void main(String[] args) {
		Main.foo(0);
	}
}