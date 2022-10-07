class Foo {
	
	Object bar() { return null; }
	
	Object baz(int x) { return null; }
	
	private Object x;
	
	/**
	 * @inspects | this, ...stuff, other, zazz, x
	 * @mutates | quux, bar(3), ...x, x
	 * @mutates_properties | bar(), baz(3), other, (...x).bar(), x, (...stuff).quux()
	 */
	Foo bar(Foo other, Iterable<Foo> stuff, Foo quux) {}
	
	/** @representationObject */
	public Object z1;
	
	/** @representationObjects */
	public Object z2;
	
	/** @peerObject */
	public Object z3;
	
	/** @peerObjects */
	public Object z4;
	
	/** @representationObject */
	private static Object z5;
	
	/** @representationObjects */
	private static Object z6;
	
	/** @peerObject */
	private static Object z7;
	
	/** @peerObjects */
	private static Object z8;
	
	/** @representationObjects */
	Object z9;
	
	/** @peerObjects */
	Object z10;
	
	/** @representationObject */
	public Object z1() { return null; }
	
	/** @representationObjects */
	public Object z2() { return null; }
	
	/** @peerObject */
	public Object z3() { return null; }
	
	/** @peerObjects */
	public Object z4() { return null; }
	
	/** @representationObject */
	static Object z5() { return null; }
	
	/** @representationObjects */
	static Object z6() { return null; }
	
	/** @peerObject */
	static Object z7() { return null; }
	
	/** @peerObjects */
	static Object z8() { return null; }
	
	/** @representationObject */
	Object z9(int i) { return null; }
	
	/** @representationObjects */
	Object z10(int i) { return null; }
	
	/** @peerObject */
	Object z11(int i) { return null; }
	
	/** @peerObjects */
	Object z12(int i) { return null; }
	
	/** @representationObject */
	void z13() {}
	
	/** @representationObjects */
	Object z14() { return null; }
	
	/** @peerObject */
	void z15() {}
	
	/** @peerObjects */
	Object z16() { return null; }
	
}