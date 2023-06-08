class Foo {
	
	/**
	 * @may_throw | false
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