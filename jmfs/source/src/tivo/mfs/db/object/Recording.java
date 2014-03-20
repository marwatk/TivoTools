package tivo.mfs.db.object;

import java.util.List;

import tivo.mfs.db.IdableObject;
import tivo.mfs.db.attribute.ObjectRef;

public class Recording extends IdableObject {
	private int				version;
	private int				bitRate;
	private int				cancelDate;
	private int				cancelReason;
	private int				cancelTime;
	private int				createdBy;
	private int				deletionDate;
	private int				deletionTime;
	private int				diskPartitionId;
	private int				_81;
	private int				nSecondsWatched;
	private ObjectRef		primaryProgramSource;
	private int				_77;
	private int				recordQuality;
	private int				recordingBehavior;
	private int				saveToTapeStatus;
	private int				score;
	private int				selectionType;
	private int				serviceRecordingPriority;
	private ObjectRef		showing;
	private int				startPadding;
	private int				subPriority;
	private String			serverId;
	private int				nVisit;
	private int				usedBy;
	private ObjectRef		drm;
	private int				startTime;
	private ObjectRef		actualShowing;
	private List<Integer>	npkChannelDefinition;
	private List<ObjectRef>	part;
	private List<Integer>	bitstreamFormat;
	private int				endPadding;
	private int				expirationDate;
	private ObjectRef		programSource;
	private int				state;
	private int				stopDate;
	private int				stopTime;
	private int				_76;
	private int				_80;
	private int				expirationTime;
	private int				streamFileSize;
	private List<Integer>	_75;
	private int				startDate;
	private List<ObjectRef>	indexUsed;
	private List<String>	indexPath;
	
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public int getBitRate() {
		return bitRate;
	}
	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}
	public int getCancelDate() {
		return cancelDate;
	}
	public void setCancelDate(int cancelDate) {
		this.cancelDate = cancelDate;
	}
	public int getCancelReason() {
		return cancelReason;
	}
	public void setCancelReason(int cancelReason) {
		this.cancelReason = cancelReason;
	}
	public int getCancelTime() {
		return cancelTime;
	}
	public void setCancelTime(int cancelTime) {
		this.cancelTime = cancelTime;
	}
	public int getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}
	public int getDeletionDate() {
		return deletionDate;
	}
	public void setDeletionDate(int deletionDate) {
		this.deletionDate = deletionDate;
	}
	public int getDeletionTime() {
		return deletionTime;
	}
	public void setDeletionTime(int deletionTime) {
		this.deletionTime = deletionTime;
	}
	public int getDiskPartitionId() {
		return diskPartitionId;
	}
	public void setDiskPartitionId(int diskPartitionId) {
		this.diskPartitionId = diskPartitionId;
	}
	public int get81() {
		return _81;
	}
	public void set81(int _81) {
		this._81 = _81;
	}
	public int getNSecondsWatched() {
		return nSecondsWatched;
	}
	public void setNSecondsWatched(int secondsWatched) {
		nSecondsWatched = secondsWatched;
	}
	public ObjectRef getPrimaryProgramSource() {
		return primaryProgramSource;
	}
	public void setPrimaryProgramSource(ObjectRef primaryProgramSource) {
		this.primaryProgramSource = primaryProgramSource;
	}
	public int get77() {
		return _77;
	}
	public void set77(int _77) {
		this._77 = _77;
	}
	public int getRecordQuality() {
		return recordQuality;
	}
	public void setRecordQuality(int recordQuality) {
		this.recordQuality = recordQuality;
	}
	public int getRecordingBehavior() {
		return recordingBehavior;
	}
	public void setRecordingBehavior(int recordingBehavior) {
		this.recordingBehavior = recordingBehavior;
	}
	public int getSaveToTapeStatus() {
		return saveToTapeStatus;
	}
	public void setSaveToTapeStatus(int saveToTapeStatus) {
		this.saveToTapeStatus = saveToTapeStatus;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public int getSelectionType() {
		return selectionType;
	}
	public void setSelectionType(int selectionType) {
		this.selectionType = selectionType;
	}
	public int getServiceRecordingPriority() {
		return serviceRecordingPriority;
	}
	public void setServiceRecordingPriority(int serviceRecordingPriority) {
		this.serviceRecordingPriority = serviceRecordingPriority;
	}
	public ObjectRef getShowing() {
		return showing;
	}
	public void setShowing(ObjectRef showing) {
		this.showing = showing;
	}
	public int getStartPadding() {
		return startPadding;
	}
	public void setStartPadding(int startPadding) {
		this.startPadding = startPadding;
	}
	public int getSubPriority() {
		return subPriority;
	}
	public void setSubPriority(int subPriority) {
		this.subPriority = subPriority;
	}
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public int getNVisit() {
		return nVisit;
	}
	public void setNVisit(int visit) {
		nVisit = visit;
	}
	public int getUsedBy() {
		return usedBy;
	}
	public void setUsedBy(int usedBy) {
		this.usedBy = usedBy;
	}
	public ObjectRef getDrm() {
		return drm;
	}
	public void setDrm(ObjectRef drm) {
		this.drm = drm;
	}
	public int getStartTime() {
		return startTime;
	}
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}
	public ObjectRef getActualShowing() {
		return actualShowing;
	}
	public void setActualShowing(ObjectRef actualShowing) {
		this.actualShowing = actualShowing;
	}
	public List<Integer> getNpkChannelDefinition() {
		return npkChannelDefinition;
	}
	public void setNpkChannelDefinition(List<Integer> npkChannelDefinition) {
		this.npkChannelDefinition = npkChannelDefinition;
	}
	public List<ObjectRef> getPart() {
		return part;
	}
	public void setPart(List<ObjectRef> part) {
		this.part = part;
	}
	public List<Integer> getBitstreamFormat() {
		return bitstreamFormat;
	}
	public void setBitstreamFormat(List<Integer> bitstreamFormat) {
		this.bitstreamFormat = bitstreamFormat;
	}
	public int getEndPadding() {
		return endPadding;
	}
	public void setEndPadding(int endPadding) {
		this.endPadding = endPadding;
	}
	public int getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(int expirationDate) {
		this.expirationDate = expirationDate;
	}
	public ObjectRef getProgramSource() {
		return programSource;
	}
	public void setProgramSource(ObjectRef programSource) {
		this.programSource = programSource;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	public int getStopDate() {
		return stopDate;
	}
	public void setStopDate(int stopDate) {
		this.stopDate = stopDate;
	}
	public int getStopTime() {
		return stopTime;
	}
	public void setStopTime(int stopTime) {
		this.stopTime = stopTime;
	}
	public int get76() {
		return _76;
	}
	public void set76(int _76) {
		this._76 = _76;
	}
	public int get80() {
		return _80;
	}
	public void set80(int _80) {
		this._80 = _80;
	}
	public int getExpirationTime() {
		return expirationTime;
	}
	public void setExpirationTime(int expirationTime) {
		this.expirationTime = expirationTime;
	}
	public int getStreamFileSize() {
		return streamFileSize;
	}
	public void setStreamFileSize(int streamFileSize) {
		this.streamFileSize = streamFileSize;
	}
	public List<Integer> get75() {
		return _75;
	}
	public void set75(List<Integer> _75) {
		this._75 = _75;
	}
	public int getStartDate() {
		return startDate;
	}
	public void setStartDate(int startDate) {
		this.startDate = startDate;
	}
	public List<ObjectRef> getIndexUsed() {
		return indexUsed;
	}
	public void setIndexUsed(List<ObjectRef> indexUsed) {
		this.indexUsed = indexUsed;
	}
	public List<String> getIndexPath() {
		return indexPath;
	}
	public void setIndexPath(List<String> indexPath) {
		this.indexPath = indexPath;
	}
}
