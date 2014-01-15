package simple

class BasicWithClosureD2 {
	
	public static void main(String[] args) {
		new BasicWithClosureD2().run();
	}
	
	public String run() {
		foo()
		String sone = "abc"
		String stwo = "def"
//		doit {
			doitInner {
				println 'string is '+stwo
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