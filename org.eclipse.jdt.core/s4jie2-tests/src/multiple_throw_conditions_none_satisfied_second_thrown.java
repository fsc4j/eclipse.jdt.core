class Main {
	
	/**
	 * @throws IllegalArgumentException | false
	 * @throws ArithmeticException | false
	 */
	public static void foo() {
		throw new ArithmeticException();
	}
	
	public static void main(String[] args) {
		Main.foo();
	}
}