class Foo {
	
	/**
	 * @throws IllegalArgumentException | foobar == 1
	 */
	public void bar(int foobar) {
		if (foobar == 1)
			throw new IllegalArgumentException();
	}
}

class Main {
	public static void main(String[] args) {
		Foo foo = new Foo();
		foo.bar(1);
	}
}