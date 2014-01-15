package simple

class BasicWithClosure2 {
	
	def clos = null;
	
	public String run() {
	clos =  { println "hello!" }
		print "Executing:"
		clos()            
	} 
	
	public static void main(String[] args) {
		new BasicWithClosure2().run();
	}
	
}