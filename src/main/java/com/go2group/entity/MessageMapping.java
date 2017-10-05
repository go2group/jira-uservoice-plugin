package com.go2group.entity;

import net.java.ao.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public interface MessageMapping extends Entity {

    public Long getIssueComment();

    public void setIssueComment(Long issueComment);

    public String getTicketMessage();

    public void setTicketMessage(String ticketId);

    public String getTicket();

    public void setTicket(String ticket);

}
