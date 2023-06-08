class Main {
	
	/**
	 * @throws IllegalArgumentException | false
	 * @throws ArithmeticException | false
	 */
	public static void foo() {
		throw new IllegalArgumentException();
	}
	
	public static void main(String[] args) {
		Main.foo();
	}
}