package simple

class BasicWithClosureE3 {
	
	public static void main(String[] args) {
		new BasicWithClosureE3().run();
	}
	
	public void xxx() {
		
	}
	
	public String run() {
		foo()
		String sone = "xyz"
		String stwo = "def"
//		doit {
			doitInner {
				println 'string is '+sone
				print "owner is "+(getOwner()==null?"null":"not null")
			}
//		}         
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