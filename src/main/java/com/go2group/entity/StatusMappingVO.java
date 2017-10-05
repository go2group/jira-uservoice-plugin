package com.go2group.entity;

public class StatusMappingVO implements Comparable<StatusMappingVO> {
	private String mapId;
	private String userVoiceStatus;
	private String jiraStatus;
	private String uvStatusId;
	private String jiraStatusId;

	public String getMapId() {
		return mapId;
	}

	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	public String getUserVoiceStatus() {
		return userVoiceStatus;
	}

	public void setUserVoiceStatus(String userVoiceStatus) {
		this.userVoiceStatus = userVoiceStatus;
	}

	public String getJiraStatus() {
		return jiraStatus;
	}

	public void setJiraStatus(String jiraStatus) {
		this.jiraStatus = jiraStatus;
	}

	@Override
	public int compareTo(StatusMappingVO fbsm1) {
		if (fbsm1 == null) {
			return 1;
		}

		return fbsm1.getUserVoiceStatus().compareTo(fbsm1.getUserVoiceStatus());
	}

	@Override
	public boolean equals(Object obj1) {
		if (obj1 == null) {
			return false;
		}
		StatusMappingVO newObject = obj1 instanceof StatusMappingVO ? (StatusMappingVO) obj1
				: null;
		if (newObject == null) {
			return false;
		}
		boolean equality = false;
		if ((this.getUserVoiceStatus().equals(newObject.getUserVoiceStatus()))
				& this.getJiraStatus().equals(newObject.getJiraStatus())) {
			equality = true;
		}

		return equality;
	}

	@Override
	public int hashCode() {
		return ((int) (this.getUserVoiceStatus().hashCode() + this
				.getJiraStatus().hashCode()) / 10);
	}

	public String getUvStatusId() {
		return uvStatusId;
	}

	public void setUvStatusId(String uvStatusId) {
		this.uvStatusId = uvStatusId;
	}

	public String getJiraStatusId() {
		return jiraStatusId;
	}

	public void setJiraStatusId(String jiraStatusId) {
		this.jiraStatusId = jiraStatusId;
	}

}
