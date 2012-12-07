package simple

class BasicWithClosureB {
	
	def clos =  { println "hello!" }
	
	public String run() {
		print "Executing:"
		clos()            
	} 
	
	public static void main(String[] args) {
		new BasicWithClosureB().run();
	}
	
}