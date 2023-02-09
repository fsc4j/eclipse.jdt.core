class Account {
	private int balance;
	
	int getBalance() { return balance; }
	
	/**
	 * @mutates |
	 */
	Account() {
		balance = 0;
	}
	
	/**
	 * @mutates | this
	 */
	void deposit(int amount) {
		this.balance += amount;
	}
}

class AccountWrapper {
	/**
	 * @representationObject
	 */
	private Account account;
	
	/**
	 * @mutates |
	 */
	AccountWrapper() {
		account = new Account();
	}

	/**
	 * @mutates | this
	 */
	void deposit(int amount) {
		account.deposit(amount);
	}
	
	/**
	 * @mutates | this
	 */
	void badDeposit0(Account account) {
		this.account = account;
		deposit(10);
	}
	
	/**
	 * @mutates | this
	 */
	void badDeposit() {
		Account account = this.account;
		this.account = null;
		badDeposit0(account);
	}
}

class Main {
	public static void main(String[] args) {
		AccountWrapper wrapper = new AccountWrapper();
		wrapper.deposit(42);
		wrapper.badDeposit();
	}
}