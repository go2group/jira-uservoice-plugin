package com.go2group.entity;

import net.java.ao.Entity;

public interface CommentMapping extends Entity {

	public Long getIssueComment();

	public void setIssueComment(Long issueComment);

	public String getFeedbackComment();

	public void setFeedbackComment(String feedbackComment);

	public String getFeedback();

	public void setFeedback(String feedback);

	public String getForum();

	public void setForum(String forum);

}
