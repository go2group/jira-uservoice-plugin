package com.go2group.entity;

import net.java.ao.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 30/10/13
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TicketMapping  extends Entity {

    public String getJiraProject();

    public void setJiraProject(String jiraProject);

    public String getJiraIssueType();

    public void setJiraIssueType(String issueType);
}
