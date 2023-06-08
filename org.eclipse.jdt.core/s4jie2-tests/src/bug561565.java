// https://bugs.eclipse.org/bugs/show_bug.cgi?id=561565

class Foo {
	
	int x;
	
	static void bar(int x) {}
	
	static void foo(Foo foo) {
		try {
			bar(foo.x);
		} catch (Throwable t) {}
		if (foo == null)
			throw new IllegalArgumentException();
	}
	
}