package simple

class BasicH {
	
	public String run() {
		// warmup
		for (int i=0;i<100000;i++) {
			m();
		}
		// measure
		long l = System.currentTimeMillis()
		for (int i=0;i<1000000;i++) {
			m();
		}
		return Long.toString(System.currentTimeMillis()-l);
	} 
	
	public void m() {
		
	}
	
	public static void main(String[] args) {
		println new BasicH().run();
	}
}