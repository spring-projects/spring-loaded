package simple

class BasicWithClosure {
	
	def clos =null;
	
	public String run() {
		clos =  { println "hello!" }
		print "Executing:"
		clos()            
	} 
	
	public static void main(String[] args) {
		new BasicWithClosure().run();
	}
	
}