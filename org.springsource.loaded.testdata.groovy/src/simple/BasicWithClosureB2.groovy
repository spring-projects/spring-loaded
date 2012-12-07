package simple

class BasicWithClosureB2 {
	
	def clos =  { println "hello!" }
	
	public String run() {
		print "Executing:"
		clos()            
	} 
	
	public static void main(String[] args) {
		new BasicWithClosureB2().run();
	}
	
}