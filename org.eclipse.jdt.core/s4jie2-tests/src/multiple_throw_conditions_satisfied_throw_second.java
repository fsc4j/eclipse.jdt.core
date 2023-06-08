class Main {
	
	/**
	 * @throws IllegalArgumentException | true
	 * @throws ArithmeticException | true
	 */
	public static void foo() {
		throw new ArithmeticException();
	}
	
	public static void main(String[] args) {
		Main.foo();
	}
}