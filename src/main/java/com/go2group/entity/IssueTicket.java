package com.go2group.entity;

import net.java.ao.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 12:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IssueTicket extends Entity {

    //Stores mapping between JIRA issue and UserVoice Ticket

    public String getProject();

    public void setProject(String projectId);

    public Long getIssue();

    public void setIssue(Long issue);

    public String getTicket();

    public void setTicket(String ticket);
    
	public String getMapping();
	
	public void setMapping(String mapping);
}
