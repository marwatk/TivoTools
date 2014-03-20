package tivo.mfs.db.object;

import java.util.List;

import tivo.mfs.db.IdableObject;

public class RecordingPartDrm extends IdableObject {
	private List<Integer>	mediaEncryptionKey;
	private int				mediaEncryptionKeyOffset;
	private int				mediaEncryptionKeyType;
	private List<Integer>	mediaSigningKey;
	private int				mediaSigningKeyType;
	private List<Integer>	signature;
	private int				signatureType;
	
	public List<Integer> getMediaEncryptionKey() {
		return mediaEncryptionKey;
	}
	public void setMediaEncryptionKey(List<Integer> mediaEncryptionKey) {
		this.mediaEncryptionKey = mediaEncryptionKey;
	}
	public int getMediaEncryptionKeyOffset() {
		return mediaEncryptionKeyOffset;
	}
	public void setMediaEncryptionKeyOffset(int mediaEncryptionKeyOffset) {
		this.mediaEncryptionKeyOffset = mediaEncryptionKeyOffset;
	}
	public int getMediaEncryptionKeyType() {
		return mediaEncryptionKeyType;
	}
	public void setMediaEncryptionKeyType(int mediaEncryptionKeyType) {
		this.mediaEncryptionKeyType = mediaEncryptionKeyType;
	}
	public List<Integer> getMediaSigningKey() {
		return mediaSigningKey;
	}
	public void setMediaSigningKey(List<Integer> mediaSigningKey) {
		this.mediaSigningKey = mediaSigningKey;
	}
	public int getMediaSigningKeyType() {
		return mediaSigningKeyType;
	}
	public void setMediaSigningKeyType(int mediaSigningKeyType) {
		this.mediaSigningKeyType = mediaSigningKeyType;
	}
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
