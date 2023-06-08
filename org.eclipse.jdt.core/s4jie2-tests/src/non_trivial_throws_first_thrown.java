class Main {
	
	/**
	 * @throws IllegalArgumentException | bar == 15
	 * @throws ArithmeticException | bar == 15
	 */
	public static void foo(int bar) {
		throw new IllegalArgumentException();
	}
	
	public static void main(String[] args) {
		Main.foo(15);
	}
}