package tivo.mfs.db.object;

import tivo.mfs.db.IdableObject;
import tivo.mfs.db.attribute.ObjectRef;

public class RecordingBehavior extends IdableObject {
	private int			diskBehavior;
	private int			presentationBehavior;
	private int			programGuideBehavior;
	private int			tunerBehavior;
	private ObjectRef	indexUsedBy;
	
	public int getDiskBehavior() {
		return diskBehavior;
	}
	public void setDiskBehavior(int diskBehavior) {
		this.diskBehavior = diskBehavior;
	}
	public int getPresentationBehavior() {
		return presentationBehavior;
	}
	public void setPresentationBehavior(int presentationBehavior) {
		this.presentationBehavior = presentationBehavior;
	}
	public int getProgramGuideBehavior() {
		return programGuideBehavior;
	}
	public void setProgramGuideBehavior(int programGuideBehavior) {
		this.programGuideBehavior = programGuideBehavior;
	}
	public int getTunerBehavior() {
		return tunerBehavior;
	}
	public void setTunerBehavior(int tunerBehavior) {
		this.tunerBehavior = tunerBehavior;
	}
	public ObjectRef getIndexUsedBy() {
		return indexUsedBy;
	}
	public void setIndexUsedBy(ObjectRef indexUsedBy) {
		this.indexUsedBy = indexUsedBy;
	}
}
