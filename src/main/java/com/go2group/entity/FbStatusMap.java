package com.go2group.entity;

import net.java.ao.Entity;

/*
 * FeedbackStatusMap
 */
public interface FbStatusMap extends Entity {

	public String getMapId();
	
	public void setMapId(String mapId);
	
	public String getUserVoiceStatus();
	
	public String getUvStatusId();
	
	public String getJiraStatus();
	
	public String getJiraStatusId();

	public void setUvStatusId(String uvStatusId);
	
	public void setUserVoiceStatus(String userVoiceStatus);

	public void setJiraStatusId(String jiraStatusId);
	
	public void setJiraStatus(String jiraStatus);
	
}
