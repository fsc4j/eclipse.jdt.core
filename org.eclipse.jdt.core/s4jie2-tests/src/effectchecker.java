class Account {
	
	int balance;
	
	/**
	 * @mutates |
	 */
	Account() {
		balance = 0;
	}
	
	/**
	 * @inspects | this
	 */
	private int getBalancePrivate() { return balance; }
	
	/**
	 * @inspects | this
	 */
	int getBalance() { return getBalancePrivate(); }

	/**
	 * @pre | 0 <= getBalancePrivate()
	 * @mutates | this
	 * @post | getBalancePrivate() == old(getBalancePrivate()) + amount
	 */
	private void depositPrivate(int amount) {
		balance += amount;
	}

	/**
	 * @pre | 0 <= getBalance()
	 * @mutates | this
	 * @post | getBalance() == old(getBalance()) + amount
	 */
	void deposit(int amount) {
		depositPrivate(amount);
	}

	/**
	 * @mutates |
	 */
	void depositBad(int amount) {
		deposit(amount);
	}

	/**
	 * @mutates |
	 */
	void depositBad2(int amount) {
		balance += amount;
	}

	/**
	 * @pre | 0 <= getBalance()
	 * @mutates_properties | this.getBalance()
	 * @post | getBalance() == old(getBalance()) + amount
	 */
	void depositOuter(int amount) {
		deposit(amount);
	}
}

class AccountWrapper {
	
	/**
	 * @representationObject
	 */
	private Account account;
	
	/**
	 * @inspects | this
	 */
	private Account getAccountPrivate() { return account; }
	
	/**
	 * @mutates |
	 */
	AccountWrapper() {
		account = new Account();
	}
	
	/**
	 * @inspects | this, account
	 */
	private int getBalancePrivate() { return getAccountPrivate().getBalance(); }
	
	/**
	 * @inspects | this
	 */
	int getBalance() { return getBalancePrivate(); }

	/**
	 * @pre | 0 <= getBalancePrivate()
	 * @mutates | this
	 * @mutates | account
	 * @post | getBalancePrivate() == old(getBalancePrivate()) + amount
	 */
	private void depositPrivate(int amount) {
		getAccountPrivate().deposit(amount);
	}

	/**
	 * @pre | 0 <= getBalance()
	 * @mutates | this
	 * @post | getBalance() == old(getBalance()) + amount
	 */
	void deposit(int amount) {
		depositPrivate(amount);
	}

	/**
	 * @pre | 0 <= getBalance()
	 * @mutates_properties | this.getBalance()
	 * @post | getBalance() == old(getBalance()) + amount
	 */
	void depositOuter(int amount) {
		deposit(amount);
	}
}

class BadAccountWrapper {
	
	Account account;
	
	/**
	 * @inspects | this
	 */
	private Account getAccountPrivate() { return account; }
	
	/**
	 * @mutates |
	 * @post | getBalance() == 0
	 */
	BadAccountWrapper() {
		account = new Account();
	}
	
	/**
	 * @inspects | this
	 * @inspects | account
	 */
	private int getBalancePrivate() { return getAccountPrivate().getBalance(); }
	
	/**
	 * @inspects | this
	 */
	int getBalance() { return getBalancePrivate(); }
}

class Node {
	/**
	 * @peerObject
	 */
	private Node next;
	/**
	 * @peerObject
	 */
	private Node previous;
	
	/**
	 * @inspects | this
	 */
	Node getNext() { return next; }
	
	/**
	 * @inspects | this
	 */
	Node getPrevious() { return previous; }
	
	/**
	 * @mutates | this
	 */
	void setNext(Node next) { this.next = next; }
	
	/**
	 * @mutates | this
	 */
	void setPrevious(Node previous) { this.previous = previous; }
}

class LinkedList {
	
	/**
	 * @representationObject
	 */
	private Node sentinel;
	
	/**
	 * @inspects | this
	 */
	int getNbNodes() {
		Node n = sentinel.getNext();
		int result = 0;
		while (n != sentinel) {
			n = n.getNext();
			result++;
		}
		return result;
	}
	
	/**
	 * @inspects | this
	 */
	void getNbNodesBad() {
		removeFirst();
	}
	
	/**
	 * @mutates |
	 * @post | getNbNodes() == 0
	 */
	LinkedList() {
		sentinel = new Node();
		sentinel.setNext(sentinel);
		sentinel.setPrevious(sentinel);
	}
	
	/**
	 * @mutates | this
	 * @post | getNbNodes() == old(getNbNodes()) + 1
	 */
	void addLast() {
		Node node = new Node();
		node.setNext(sentinel);
		node.setPrevious(sentinel.getPrevious());
		node.getNext().setPrevious(node);
		node.getPrevious().setNext(node);
	}
	
	/**
	 * @pre | getNbNodes() > 0
	 * @mutates | this
	 * @post | getNbNodes() == old(getNbNodes()) - 1
	 */
	void removeFirst() {
		Node first = sentinel.getNext();
		first.getNext().setPrevious(first.getPrevious());
		first.getPrevious().setNext(first.getNext());
	}
}

class Main {
	
	/**
	 * @inspects |
	 */
	public static void foo0(String[] args) {
		fsc4j.EffectChecker.assertCanMutate(args); // This should throw.
	}
	
	/**
	 * @inspects |
	 */
	public static void foo(String[] args) {
		fsc4j.EffectChecker.assertCanMutate(args); // This will pass the first time, because 'args' is not yet claimed. This will claim 'args'.
		foo0(args);
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
	
	/**
	 * @inspects | args
	 */
	public static void inspectArray(String[] args) {
		
	}

	/**
	 * @inspects | args
	 */
	public static void inspectArrayOuter(String[] args) {
		inspectArray(args);
	}

	/**
	 * @inspects |
	 */
	public static void inspectArrayBadOuter(String[] args) {
		inspectArray(args);
	}
	
	/**
	 * @mutates | args
	 */
	public static void mutateArray(String[] args) {
		
	}
	
	/**
	 * @mutates | args
	 */
	public static void mutateArrayOuter(String[] args) {
		mutateArray(args);
	}
	
	/**
	 * @mutates |
	 */
	public static void mutateArrayBadOuter(String[] args) {
		mutateArray(args);
	}

	public static void main(String[] args) throws Exception {
		System.err.println("Checking that a toplevel assertCanMutate passes:");
		fsc4j.EffectChecker.assertCanMutate(args); // This should not throw; outside a spec frame, everything goes.
		System.err.println("Checking a basic failure case:");
		try {
			foo(args);
			System.err.println("Should not happen!");
		} catch (AssertionError e) {
			e.printStackTrace();
		}
		System.err.println("Some sanity checks:");
		fsc4j.EffectChecker.assertCanMutate(new int[2]); // This should not throw; we should again be outside a spec frame.
		if (bar() != 42) throw new AssertionError();
		try {
			baz();
			throw new AssertionError();
		} catch (IllegalArgumentException e) {}
		inspectArrayOuter(args);
		mutateArrayOuter(args);
		Account account = new Account();
		account.deposit(42);
		account.depositOuter(23);
		if (account.getBalance() != 65)
			throw new AssertionError();
		AccountWrapper wrapper = new AccountWrapper();
		wrapper.deposit(42);
		wrapper.depositOuter(23);
		if (wrapper.getBalance() != 65)
			throw new AssertionError();
		LinkedList linkedList = new LinkedList();
		linkedList.addLast();
		linkedList.addLast();
		linkedList.removeFirst();
		linkedList.removeFirst();
		linkedList.addLast();
		System.err.println("This should fail:");
		try {
			inspectArrayBadOuter(args);
			System.err.println("Should not happen!");
		} catch (AssertionError e) {
			e.printStackTrace();
		}
		System.err.println("This should also fail:");
		try {
			mutateArrayBadOuter(args);
			System.err.println("Should not happen!");
		} catch (AssertionError e) {
			e.printStackTrace();
		}
		System.err.println("This should fail as well:");
		try {
			account.depositBad(10);
			System.err.println("Should not happen!");
		} catch (AssertionError e) {
			e.printStackTrace();
		}
		try {
			account.depositBad2(10);
			System.err.println("Should not happen!");
		} catch (AssertionError e) {
			e.printStackTrace();
		}
		System.err.println("This should fail too:");
		try {
			new BadAccountWrapper();
			System.err.println("Should not happen!");
		} catch (AssertionError e) {
			e.printStackTrace();
		}
		System.err.println("This should fail also:");
		try {
			linkedList.getNbNodesBad();
			System.err.println("Should not happen!");
		} catch (AssertionError e) {
			e.printStackTrace();
		}
	}
}