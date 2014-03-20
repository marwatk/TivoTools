package tivo.mfs.db.object;

import java.util.ArrayList;
import java.util.List;

public class DbRecording {
	private Recording				recording;
	private RecordingBehavior		recordingBehavior;
	private RecordingDrm			recordingDrm;
	private List<Showing>			showings;
	private List<RecordingPartDrm>	recordingPartDrms;
	private List<RecordingPart>		recordingParts;
	
	public Recording getRecording() {
		return recording;
	}
	public void setRecording(Recording recording) {
		this.recording = recording;
	}
	public RecordingBehavior getRecordingBehavior() {
		return recordingBehavior;
	}
	public void setRecordingBehavior(RecordingBehavior recordingBehavior) {
		this.recordingBehavior = recordingBehavior;
	}
	public RecordingDrm getRecordingDrm() {
		return recordingDrm;
	}
	public void setRecordingDrm(RecordingDrm recordingDrm) {
		this.recordingDrm = recordingDrm;
	}
	public List<Showing> getShowings() {
		return showings;
	}
	public void setShowings(List<Showing> showings) {
		this.showings = showings;
	}
	public void addShowing(Showing showing) {
		if( showings == null )
			showings = new ArrayList<Showing>();
		showings.add( showing );
	}
	public List<RecordingPartDrm> getRecordingPartDrms() {
		return recordingPartDrms;
	}
	public void setRecordingPartDrms(List<RecordingPartDrm> recordingPartDrms) {
		this.recordingPartDrms = recordingPartDrms;
	}
	public void addRecordingPartDrm(RecordingPartDrm recordingPartDrm) {
		if( recordingPartDrms == null )
			recordingPartDrms = new ArrayList<RecordingPartDrm>();
		recordingPartDrms.add( recordingPartDrm );
	}
	public List<RecordingPart> getRecordingParts() {
		return recordingParts;
	}
	public void setRecordingParts(List<RecordingPart> recordingParts) {
		this.recordingParts = recordingParts;
	}
	public void addRecordingPart(RecordingPart recordingPart) {
		if( recordingParts == null )
			recordingParts = new ArrayList<RecordingPart>();
		recordingParts.add( recordingPart );
	}
}
