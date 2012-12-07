package simple

class BasicWithClosureC2 {
	
	public static void main(String[] args) {
		new BasicWithClosureC2().run();
	}
	
	public String run() {
		doit {
			doitInner {
				println 'in closure'
				foo() // moved from top of run() method to here
			}
		}         
	} 
	
	public void doit(Closure c) {
		c.call()
	}
	
	public void doitInner(Closure c) {
		c.call()
	}
	
	
	public void foo() {
		println 'foo() running'
	}
	
}