package simple

class BasicWithClosure3 {
	
	def clos = null;
	
	public String run() {
	clos =  { println "goodbye!" }
		print "Executing:"
		clos()            
	} 
	
	public static void main(String[] args) {
		new BasicWithClosure3().run();
	}
	
}