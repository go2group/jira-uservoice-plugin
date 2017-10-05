package com.go2group.uservoice.bean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class Ticket {
    private String id;
    private String ticketNumber;
    private String subject;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private String url;
    private String state;
    private User assignee;
    private User createdBy;
    private User updatedBy;
    private Date createdAt;
    private Date updatedAt;
    private List<UserVoiceCustomFieldValue> uvCustomFields=new ArrayList<UserVoiceCustomFieldValue>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

	public List<UserVoiceCustomFieldValue> getUvCustomFields() {
		return uvCustomFields;
	}

	public void setUvCustomFields(List<UserVoiceCustomFieldValue> uvCustomFields) {
		this.uvCustomFields = uvCustomFields;
	}

	public void addCustomField(UserVoiceCustomFieldValue uvCustomField){
		uvCustomFields.add(uvCustomField);
	}
}
