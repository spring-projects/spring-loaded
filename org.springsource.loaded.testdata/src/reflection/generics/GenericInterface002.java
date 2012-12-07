package reflection.generics;

import java.util.Iterator;

public interface GenericInterface002<K> {

	public Iterator<K> iterator();
	
	public void processThem(String... strings);
		
    <E extends RuntimeException> void genericThrow() throws E;
    
    void checkMe() throws SecurityException, NoSuchFieldException;
	
	
}
