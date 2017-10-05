package com.go2group.entity;

import net.java.ao.Entity;

public interface Mapping extends Entity {

	public String getJiraProject();

	public void setJiraProject(String jiraProject);

	public String getJiraIssueType();

	public void setJiraIssueType(String issueType);

	public String getUvForum();

	public void setUvForum(String uvForum);

	public String getUvType();

	public void setUvType(String uvType);

}
