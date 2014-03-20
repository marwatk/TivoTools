package tivo.mfs.db.object;

import tivo.mfs.db.IdableObject;
import tivo.mfs.db.attribute.ObjectRef;

public class RecordingPart extends IdableObject {
	private ObjectRef	drm;
	private int			file;
	private int			begin;
	private int			end;
	
	public ObjectRef getDrm() {
		return drm;
	}
	public void setDrm(ObjectRef drm) {
		this.drm = drm;
	}
	public int getFile() {
		return file;
	}
	public void setFile(int file) {
		this.file = file;
	}
	public int getBegin() {
		return begin;
	}
	public void setBegin(int begin) {
		this.begin = begin;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
}
