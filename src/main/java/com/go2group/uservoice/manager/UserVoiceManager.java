package com.go2group.uservoice.manager;

import java.util.Date;
import java.util.List;

import oauth.signpost.OAuthConsumer;

import com.go2group.entity.CommentMapping;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.uservoice.bean.Comment;
import com.go2group.uservoice.bean.Feedback;
import com.go2group.uservoice.bean.Forum;
import com.go2group.uservoice.bean.Message;
import com.go2group.uservoice.bean.Ticket;
import com.go2group.uservoice.bean.FeedbackStatus;
import com.go2group.uservoice.bean.UserVoiceCustomField;

public interface UserVoiceManager {
	
	public List<Forum> getForums();
	
	public List<Feedback> getFeedbacks(String forumId);

    public List<Ticket> getTickets();

    public List<Message> getMessages(String ticketId);
	
	public Feedback getFeedback(String forumId, String feedbackId);

    public Ticket getTicket(String ticketNumber);
	
	public List<Comment> getComments(String forumId, String feedbackId, String state);
	
	public List<Object> getEvents(String forumId, Date lastRun);

    public List<Object> getEvents(Date lastRun);
	
	public boolean createNote(String forumId, String feedbackId, String note);

    public boolean createNoteForTicket(String ticketId, String note);

    public boolean createTicketNote(String ticketId, String note);

    public boolean createComment(Long jiraIssueCommentId, String forumId, String feedbackId, String comment);

    public boolean createMessage(com.atlassian.jira.issue.comments.Comment issueComment, String ticketId, String comment);

    public boolean deleteComment(CommentMapping commentMapping);
    
    /* Changes for JUVP-28 - start */
    public List<Ticket> getTickets(Date lastRun);
    public boolean updateUVState(String ticketId, String state);
    
    public List<Feedback> getFeedbacks(String forumId, Date lastRun);
    public boolean updateFeedbackStatus(String forumId, String feedbackId, String statusId, String statusName);
    
    public List<FeedbackStatus> getFeedbackStatuses(UserVoiceConfig config, OAuthConsumer consumer);
    /* Changes for JUVP-28 - end */
    
    public List<UserVoiceCustomField> getUVCustomFields(UserVoiceConfig config, OAuthConsumer consumer); /* Changes for JUVP-22 */
    
    /* Changes for JUVP-37 - start */
    public boolean updateFeedbackField(String forumId, String feedbackId, String fieldName, String modifiedText);
    public boolean updateTicketField(String ticketId, String fieldName, String modifiedText);
    /* Changes for JUVP-37 - end */
    public List<Ticket> searchTickets(Date lastRun, String filter, boolean firstSearch);
}
