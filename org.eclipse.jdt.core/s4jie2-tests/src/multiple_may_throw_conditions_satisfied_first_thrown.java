class Main {
	
	/**
	 * @may_throw IllegalArgumentException | true
	 * @may_throw ArithmeticException | true
	 */
	public static void foo() {
		throw new IllegalArgumentException();
	}
	
	public static void main(String[] args) {
		Main.foo();
	}
}