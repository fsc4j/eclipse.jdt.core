class Foo {
	
	/**
	 * @throws ArithmeticException | false
	 */
	public void bar() {
		throw new IllegalArgumentException();
	}
}

class Main {
	public static void main(String[] args) {
		Foo foo = new Foo();
		foo.bar();
	}
}