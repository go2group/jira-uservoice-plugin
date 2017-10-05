package com.go2group.entity;

import net.java.ao.Entity;

public interface FeedbackMapping extends Entity{
	
	public Long getIssue();
	
	public void setIssue(Long issue);
	
	public String getFeedback();
	
	public void setFeedback(String feedback);
	
	public String getForum();
	
	public void setForum(String forum);
	
	public String getMapping();
	
	public void setMapping(String mapping);
}
