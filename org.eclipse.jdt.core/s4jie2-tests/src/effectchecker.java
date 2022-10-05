class Main {
	
	/**
	 * @inspects |
	 */
	public static void foo(String[] args) {
		fsc4j.EffectChecker.assertCanMutate(args); // This should throw.
	}
	
	/**
	 * @inspects |
	 */
	public static int bar() {
		return 42;
	}
	
	/**
	 * @inspects |
	 */
	public static void baz() {
		throw new IllegalArgumentException();
	}
	
	public static void main(String[] args) throws Exception {
		fsc4j.EffectChecker.assertCanMutate(args); // This should not throw; outside a spec frame, everything goes.
		try {
			foo(args);
			System.out.println("Should not happen!");
		} catch (AssertionError e) {
		}
		fsc4j.EffectChecker.assertCanMutate(args); // This should not throw; we should again be outside a spec frame.
		if (bar() != 42) throw new AssertionError();
		try {
			baz();
			throw new AssertionError();
		} catch (IllegalArgumentException e) {}
		foo(args);
	}
}