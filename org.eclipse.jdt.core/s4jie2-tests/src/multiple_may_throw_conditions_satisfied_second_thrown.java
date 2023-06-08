class Main {
	
	/**
	 * @may_throw IllegalArgumentException | true
	 * @may_throw ArithmeticException | true
	 */
	public static void foo() {
		throw new ArithmeticException();
	}
	
	public static void main(String[] args) {
		Main.foo();
	}
}