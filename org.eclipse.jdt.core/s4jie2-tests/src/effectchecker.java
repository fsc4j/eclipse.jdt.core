class Main {
	public static void main(String[] args) throws Exception {
		System.out.println(Class.forName("fsc4j.EffectChecker").getDeclaredMethod("assertCanCreate", Object.class).toString());
		Object myArray = new int[4];
		fsc4j.EffectChecker.assertCanCreate(myArray);
	}
}