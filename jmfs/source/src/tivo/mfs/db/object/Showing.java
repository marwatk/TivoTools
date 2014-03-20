package tivo.mfs.db.object;

import tivo.mfs.db.IdableObject;
import tivo.mfs.db.attribute.ObjectRef;

public class Showing extends IdableObject {
	private int			bits;
	private int			_36;
	private int			_35;
	private int			date;
	private int			dolby;
	private int			dontIndex;
	private int			duration;
	private int			partCount;
	private int			partIndex;
	private ObjectRef	program;
	private int			reason;
	private ObjectRef	station;
	private int			time;
	private int			tvRating;
	private ObjectRef	indexUsedBy;
	
	public int getBits() {
		return bits;
	}
	public void setBits(int bits) {
		this.bits = bits;
	}
	public int get36() {
		return _36;
	}
	public void set36(int _36) {
		this._36 = _36;
	}
	public int get35() {
		return _35;
	}
	public void set35(int _35) {
		this._35 = 35;
	}
	public int getDate() {
		return date;
	}
	public void setDate(int date) {
		this.date = date;
	}
	public int getDolby() {
		return dolby;
	}
	public void setDolby(int dolby) {
		this.dolby = dolby;
	}
	public int getDontIndex() {
		return dontIndex;
	}
	public void setDontIndex(int dontIndex) {
		this.dontIndex = dontIndex;
	}
	public int getDuration() {
		return duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}
	public int getPartCount() {
		return partCount;
	}
	public void setPartCount(int partCount) {
		this.partCount = partCount;
	}
	public int getPartIndex() {
		return partIndex;
	}
	public void setPartIndex(int partIndex) {
		this.partIndex = partIndex;
	}
	public ObjectRef getProgram() {
		return program;
	}
	public void setProgram(ObjectRef program) {
		this.program = program;
	}
	public int getReason() {
		return reason;
	}
	public void setReason(int reason) {
		this.reason = reason;
	}
	public ObjectRef getStation() {
		return station;
	}
	public void setStation(ObjectRef station) {
		this.station = station;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
	public int getTvRating() {
		return tvRating;
	}
	public void setTvRating(int tvRating) {
		this.tvRating = tvRating;
	}
	public ObjectRef getIndexUsedBy() {
		return indexUsedBy;
	}
	public void setIndexUsedBy(ObjectRef indexUsedBy) {
		this.indexUsedBy = indexUsedBy;
	}
}
