package fields;

public class Z {

	static int j = 5;

	public int getJ() {
		return j;
	}

	public void setJ(int newj) {
		j = newj;
	}
}

// see https://java.sun.com/docs/books/jvms/second_edition/html/Instructions2.doc5.html.
// TODO [rc1] funky field lookups only necessary for protected fields?