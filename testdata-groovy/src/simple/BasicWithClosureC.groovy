package simple

class BasicWithClosureC {
	
	public static void main(String[] args) {
		new BasicWithClosureC().run();
	}
	
	public String run() {
		foo()
		doit {
			doitInner {
				println 'in closure'
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