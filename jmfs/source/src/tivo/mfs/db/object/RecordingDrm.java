package tivo.mfs.db.object;

import java.util.List;

import tivo.mfs.db.IdableObject;

public class RecordingDrm extends IdableObject {
	private List<Integer>	signature;
	private int				signatureType;
	
	public List<Integer> getSignature() {
		return signature;
	}
	public void setSignature(List<Integer> signature) {
		this.signature = signature;
	}
	public int getSignatureType() {
		return signatureType;
	}
	public void setSignatureType(int signatureType) {
		this.signatureType = signatureType;
	}
}
