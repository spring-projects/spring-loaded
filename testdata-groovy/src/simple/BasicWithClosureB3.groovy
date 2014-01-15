package simple

class BasicWithClosureB3 {
	
	def clos =  { println "goodbye!" }
	
	public String run() {
		print "Executing:"
		clos()            
	} 
	
	public static void main(String[] args) {
		new BasicWithClosureB3().run();
	}
	
}