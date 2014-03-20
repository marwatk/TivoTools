package tivo.mfs.db.object;

import java.util.List;

import tivo.mfs.db.attribute.ObjectRef;

public class Program {
	private int				serverVersion;
	private String			tmsId;
	private String			title;
	private ObjectRef		series;
	private String			description;
	private String			descLanguage;
	private int				showType;
	private int				sourceType;
	private String			episodeTitle;
	private List<String>	actor;
	private List<String>	guestStar;
	private List<String>	director;
	private List<String>	execProducer;
	private List<String>	writer;
	private int				genre;
	private int				colorCode;
	private int				isEpisode;
	private int				originalAirDate;
	private String			_60;
	private String			_61;
	private String			serverId;
	private int				version;
	private List<String>	indexPath;
	
	public int getServerVersion() {
		return serverVersion;
	}
	public void setServerVersion(int serverVersion) {
		this.serverVersion = serverVersion;
	}
	public String getTmsId() {
		return tmsId;
	}
	public void setTmsId(String tmsId) {
		this.tmsId = tmsId;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public ObjectRef getSeries() {
		return series;
	}
	public void setSeries(ObjectRef series) {
		this.series = series;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescLanguage() {
		return descLanguage;
	}
	public void setDescLanguage(String descLanguage) {
		this.descLanguage = descLanguage;
	}
	public int getShowType() {
		return showType;
	}
	public void setShowType(int showType) {
		this.showType = showType;
	}
	public int getSourceType() {
		return sourceType;
	}
	public void setSourceType(int sourceType) {
		this.sourceType = sourceType;
	}
	public String getEpisodeTitle() {
		return episodeTitle;
	}
	public void setEpisodeTitle(String episodeTitle) {
		this.episodeTitle = episodeTitle;
	}
	public List<String> getActor() {
		return actor;
	}
	public void setActor(List<String> actor) {
		this.actor = actor;
	}
	public List<String> getGuestStar() {
		return guestStar;
	}
	public void setGuestStar(List<String> guestStar) {
		this.guestStar = guestStar;
	}
	public List<String> getDirector() {
		return director;
	}
	public void setDirector(List<String> director) {
		this.director = director;
	}
	public List<String> getExecProducer() {
		return execProducer;
	}
	public void setExecProducer(List<String> execProducer) {
		this.execProducer = execProducer;
	}
	public List<String> getWriter() {
		return writer;
	}
	public void setWriter(List<String> writer) {
		this.writer = writer;
	}
	public int getGenre() {
		return genre;
	}
	public void setGenre(int genre) {
		this.genre = genre;
	}
	public int getColorCode() {
		return colorCode;
	}
	public void setColorCode(int colorCode) {
		this.colorCode = colorCode;
	}
	public int getIsEpisode() {
		return isEpisode;
	}
	public void setIsEpisode(int isEpisode) {
		this.isEpisode = isEpisode;
	}
	public int getOriginalAirDate() {
		return originalAirDate;
	}
	public void setOriginalAirDate(int originalAirDate) {
		this.originalAirDate = originalAirDate;
	}
	public String get60() {
		return _60;
	}
	public void set60(String _60) {
		this._60 = _60;
	}
	public String get61() {
		return _61;
	}
	public void set61(String _61) {
		this._61 = _61;
	}
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public List<String> getIndexPath() {
		return indexPath;
	}
	public void setIndexPath(List<String> indexPath) {
		this.indexPath = indexPath;
	}
}
