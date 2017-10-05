package com.go2group.entity;

import java.util.Date;

import net.java.ao.Entity;

public interface UserVoiceConfig extends Entity {

	public String getJiraAdmin();

	public void setJiraAdmin(String jiraAdmin);

	public String getUserVoiceAdmin();

	public void setUserVoiceAdmin(String userVoiceAdmin);

	public String getConsumerKey();

	public void setConsumerKey(String consumerKey);

	public String getSharedSecret();

	public void setSharedSecret(String sharedSecret);
	
	public String getUserVoiceUrl();

	public void setUserVoiceUrl(String userVoiceUrl);
	
	public String getOAuthToken();

	public void setOAuthToken(String oAuthToken);
	
	public String getOAuthSecret();

	public void setOAuthSecret(String oAuthSecret);
	
	public Date getLastRun();
	
	public void setLastRun(Date lastRun);

}
