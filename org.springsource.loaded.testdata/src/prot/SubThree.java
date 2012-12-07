package prot;

public class SubThree extends Three {

	PeerThree peer = new PeerThree();

	public int getField() {
		return aField;
	}

	public void setField(int i) {
		this.aField = i;
	}

	public void setPeerField(int i) {
		peer.aField = i;
	}

	public int getPeerField() {
		return peer.aField;
	}

	public PeerThree getPeer() {
		return peer;
	}

}
