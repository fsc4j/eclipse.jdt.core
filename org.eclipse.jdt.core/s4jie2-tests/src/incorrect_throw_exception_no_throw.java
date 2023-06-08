class Foo {
	
	/**
	 * @throws IllegalArgumentException | false
	 */
	public void bar() {
		return;
	}
}

class Main {
	public static void main(String[] args) {
		Foo foo = new Foo();
		foo.bar();
	}
}