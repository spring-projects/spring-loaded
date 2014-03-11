package remote

class Code {
  // Create a serialized closure
  static void main(String[] args) {
	def a = { 1 + 1 }
	def out = new ObjectOutputStream(new FileOutputStream('ser.obj'))
	out.writeObject(a)
	out.close()
  }
}