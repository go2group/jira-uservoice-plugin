package com.go2group.uservoice.manager.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import org.apache.commons.lang.StringUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.go2group.entity.CommentMapping;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.uservoice.bean.Comment;
import com.go2group.uservoice.bean.Feedback;
import com.go2group.uservoice.bean.FeedbackStatus;
import com.go2group.uservoice.bean.Forum;
import com.go2group.uservoice.bean.Message;
import com.go2group.uservoice.bean.Ticket;
import com.go2group.uservoice.bean.User;
import com.go2group.uservoice.bean.UserVoiceCustomField;
import com.go2group.uservoice.bean.UserVoiceCustomFieldValue;
import com.go2group.uservoice.manager.UserVoiceManager;
import com.go2group.util.PropertyUtil;
import com.go2group.util.UserVoiceUtil;

public class UserVoiceManagerImpl implements UserVoiceManager {

    private final ActiveObjects ao;
    private final SimpleDateFormat simpleDateFormat;

    public UserVoiceManagerImpl(ActiveObjects ao) {
        super();
        this.ao = ao;
        this.simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        this.simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    public List<Forum> getForums() {
        List<Forum> forumList = new ArrayList<Forum>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        String url = config.getUserVoiceUrl() + "/api/v1/forums.json?client=" + config.getConsumerKey() + "&per_page=100";
        try {
            String forumString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            JSONObject jsonObject = new JSONObject(forumString);
            JSONArray forums = jsonObject.getJSONArray("forums");
            for (int i = 0; i < forums.length(); i++) {
                Forum forum = new Forum();

                JSONObject forumObj = forums.getJSONObject(i);
                forum.setId(forumObj.getString("id"));
                forum.setName(forumObj.getString("name"));
                forum.setUrl(forumObj.getString("url"));
                forum.setDescription(forumObj.getString("name"));
                forumList.add(forum);
            }
            return forumList;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Feedback> getFeedbacks(String forumId) {
        List<Feedback> feedbacks = new ArrayList<Feedback>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        int nextPage = getFeedbackPerPage(forumId, feedbacks, 1, config, consumer);
        while (nextPage != -1) {
            nextPage = getFeedbackPerPage(forumId, feedbacks, nextPage, config, consumer);
        }
        return feedbacks;
    }
    
    /* Changes for JUVP-28 - start */
    @Override
    public List<Feedback> getFeedbacks(String forumId, Date lastRun) {
        List<Feedback> feedbacks = new ArrayList<Feedback>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        
        int nextPage = getUpdatedFeedbackPerPage(forumId, feedbacks, 1, config, consumer, lastRun);
        while (nextPage != -1) {
            nextPage = getUpdatedFeedbackPerPage(forumId, feedbacks, nextPage, config, consumer, lastRun);
        }
        return feedbacks;
    }
    /* Changes for JUVP-28 - end */
    
    @Override
    public List<Ticket> getTickets() {
        List<Ticket> tickets = new ArrayList<Ticket>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets.json?client=").append(config.getConsumerKey()).toString();
        try {
            String ticketString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            if(StringUtils.isNotBlank(ticketString)){
	            JSONObject response = new JSONObject(ticketString);
	            int ticketCount = Integer.parseInt(response.getJSONObject("response_data").getString("total_records"));
	            if (ticketCount > 0) {
	                JSONArray feedbackArray = response.getJSONArray("tickets");
	                for (int i = 0; i < feedbackArray.length(); i++) {
	                    JSONObject feedbackObj = feedbackArray.getJSONObject(i);
	                    tickets.add(getTicketFromJson(feedbackObj));
	                }
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return tickets;
    }

    @Override
    public Feedback getFeedback(String forumId, String feedbackId) {
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/").append(forumId)
                .append("/suggestions/").append(feedbackId).append(".json?client=").append(config.getConsumerKey())
                .toString();
        try {
            String feedbackString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            JSONObject feedbackObj = new JSONObject(feedbackString).getJSONObject("suggestion");
            return getFeedbackFromJson(feedbackObj);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Ticket getTicket(String ticketNumber) {
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets/").append(ticketNumber)
                .append(".json?client=").append(config.getConsumerKey())
                .toString();
        try {
            String ticketString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            JSONObject feedbackObj = new JSONObject(ticketString).getJSONObject("ticket");
            return getTicketFromJson(feedbackObj);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    //Used by FeedbackScannerUtil
    public List<Object> getEvents(String forumId, Date lastRun) {
        List<Object> events = new ArrayList<Object>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        try {
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/").append(forumId)
                    .append("/stream/private.json?client=").append(config.getConsumerKey()).append("&since=")
                    .append(URLEncoder.encode(this.simpleDateFormat.format(lastRun), "UTF-8"))
                    .append("&filter=suggestion,suggestion_status,comment").toString();
            String eventString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            if(eventString != null){
	            JSONObject response = new JSONObject(eventString);
	            int eventCnt = Integer.parseInt(response.getJSONObject("response_data").getString("total_records"));
	            if (eventCnt > 0) {
	                JSONArray eventArray = response.getJSONArray("events");
	                for (int i = 0; i < eventArray.length(); i++) {
	                    JSONObject eventObj = eventArray.getJSONObject(i);
	                    switch (Event.valueOf(eventObj.getString("type"))) {
	                        case suggestion:
	                            JSONObject feedbackObj = eventObj.getJSONObject("object");
	                            events.add(getFeedbackFromJson(feedbackObj));
	                            break;
	                        case suggestion_status:
	                            JSONObject updatedFeedbackObj = eventObj.getJSONObject("object").getJSONObject("suggestion");
	                            events.add(getFeedbackFromJson(updatedFeedbackObj));
	                            break;
	                        case comment:
	                            JSONObject commentObj = eventObj.getJSONObject("object");
	                            events.add(getCommentFromJson(commentObj));
	                            break;
	                        default:
	                            // Do Nothing
	                            break;
	                    }
	                }
	            }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return events;
    }

    @Override
    //Used by TicketScannerUtil
    public List<Object> getEvents(Date lastRun) {   	
        List<Object> events = new ArrayList<Object>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        try {
        	String uvDateString = simpleDateFormat.format(lastRun);
        	
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/stream/private.json?client=").append(config.getConsumerKey()).append("&since=")
                    .append(URLEncoder.encode(uvDateString, "UTF-8"))
                    .append("&filter=ticket,ticket_message").toString();
            String eventString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            if(eventString != null){
	            JSONObject response = new JSONObject(eventString);
	            int eventCnt = Integer.parseInt(response.getJSONObject("response_data").getString("total_records"));
	            if (eventCnt > 0) {
	                JSONArray eventArray = response.getJSONArray("events");
	                for (int i = 0; i < eventArray.length(); i++) {
	                    JSONObject eventObj = eventArray.getJSONObject(i);
	                    switch (Event.valueOf(eventObj.getString("type"))) {
	                        case ticket:
	                            JSONObject ticketObj = eventObj.getJSONObject("object");
	                            events.add(getTicketFromJson(ticketObj));
	                            break;
	                        case ticket_message:
	                            int messageCount = eventObj.getJSONObject("object").getJSONArray("messages").length();
	                            if (messageCount > 0) {
	                                JSONArray messageArray = eventObj.getJSONObject("object").getJSONArray("messages");
	                                for (int j = 0; j < messageArray.length(); j++) {
	                                    JSONObject messageObj = messageArray.getJSONObject(j);
	                                    //Check if message already exists, if not add
	                                    Message message = getMessageFromJson(messageObj, eventObj.getJSONObject("object").getString("id"));
	                                    if(PropertyUtil.getMessageMappingByTicketMessageId(message.getId(), ao) == null){
	                                        events.add(message);
	                                    }
	                                }
	                            }
	                            break;
	                        default:
	                            // Do Nothing
	                            break;
	                    }
	                }
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return events;
    }

    @Override
    public List<Comment> getComments(String forumId, String feedbackId, String state) {
        List<Comment> comments = new ArrayList<Comment>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        try {
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/").append(forumId)
                    .append("/suggestions/").append(feedbackId).append("/comments.json?client=")
                    .append(config.getConsumerKey()).append("&per_page=100&filter=").append(state).toString();
            String commentString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
	        if(commentString != null){
	            JSONObject response = new JSONObject(commentString);
	            int commentCnt = Integer.parseInt(response.getJSONObject("response_data").getString("total_records"));
	            if (commentCnt > 0) {
	                JSONArray commentArray = response.getJSONArray("comments");
	                for (int i = 0; i < commentArray.length(); i++) {
	                    JSONObject commentObj = commentArray.getJSONObject(i);
	                    comments.add(getCommentFromJson(commentObj));
	                }
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return comments;
    }

    @Override
    public List<Message> getMessages(String ticketId) {
        List<Message> messages = new ArrayList<Message>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        try {
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets/").append(ticketId)
                    .append("/ticket_messages.json?client=")
                    .append(config.getConsumerKey()).toString();
            String messageString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            if(messageString != null){
	            JSONObject response = new JSONObject(messageString);
	            int messageCount = Integer.parseInt(response.getJSONObject("response_data").getString("total_records"));
	            if (messageCount > 0) {
	                JSONArray messageArray = response.getJSONArray("messages");
	                for (int i = 0; i < messageArray.length(); i++) {
	                    JSONObject messageObj = messageArray.getJSONObject(i);
	                    messages.add(getMessageFromJson(messageObj, ticketId));
	                }
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return messages;
    }

    @Override
    public boolean createNote(String forumId, String feedbackId, String note) {
        boolean created = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/").append(forumId)
                    .append("/suggestions/").append(feedbackId).append("/notes.json").toString();
            String body = "{ \"note\" : { \"text\" : \"" + note + "\" } }";
            String noteString = UserVoiceUtil.postUserVoiceData(consumer, url, "POST", "application/json", body);
            if(StringUtils.isNotBlank(noteString)){
	            JSONObject response = new JSONObject(noteString);
	            if (response.getJSONObject("note") != null) {
	                return true;
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return created;
    }

    @Override
    public boolean createNoteForTicket(String ticketId, String note) {
        ///api/v1/tickets/ticket_id/notes.json
        boolean created = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets/").append(ticketId)
                    .append("/notes.json").toString();
            String body = "{ \"note\" : { \"text\" : \"" + note + "\" } }";
            /* Changes for JUVP-36 - start */
            String noteString = UserVoiceUtil.postUserVoiceData(consumer, url, "POST", "application/json", body);
            if(StringUtils.isNotBlank(noteString)){
            /* Changes for JUVP-36 - end */
	            JSONObject response = new JSONObject(noteString);
	            if (response.getJSONObject("note") != null) {
	                return true;
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return created;
    }

    @Override
    public boolean createTicketNote(String ticketId, String note) {
        boolean created = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets/").append(ticketId).append("/notes.json").toString();
            String body = "{ \"note\" : { \"text\" : \"" + note + "\" } }";
            String noteString = UserVoiceUtil.postUserVoiceData(consumer, url, "POST", "application/json", body);
	        if(StringUtils.isNotBlank(noteString)){
	            JSONObject response = new JSONObject(noteString);
	            if (response.getJSONObject("note") != null) {
	                return true;
	            }
	        }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return created;
    }

    public boolean createComment(Long jiraIssueCommentId, String forumId, String feedbackId, String comment) {
        boolean created = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/").append(forumId)
                    .append("/suggestions/").append(feedbackId).append("/comments.json").toString();
            String body = "{ \"comment\" : { \"text\" : \"" + "Posted from JIRA on behalf of " + ComponentAccessor.getJiraAuthenticationContext().getUser().getDisplayName() + ".\\n" + comment + "\" } }";
            String noteString = UserVoiceUtil.postUserVoiceData(consumer, url, "POST", "application/json", body);
            if(StringUtils.isNotBlank(noteString)){
	            JSONObject response = new JSONObject(noteString);
	            if (response.getJSONObject("comment") != null) {
	                PropertyUtil.saveCommentMapping(jiraIssueCommentId, response.getJSONObject("comment").getString("id"), feedbackId, forumId, ao);
	                return true;
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return created;
    }

    public boolean createMessage(com.atlassian.jira.issue.comments.Comment issueComment, String ticketId, String comment) {
        boolean created = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
            ///api/v1/tickets/ticket_id/ticket_messages.json
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets/").append(ticketId)
                    .append("/ticket_messages.json").toString();
            //String body = "{ \"ticket_message\" : { \"text\" : \"" + comment + "\"}, \"email\" : \"" + issueComment.getAuthorApplicationUser().getEmailAddress() + "\"}";
            String body = "{ \"ticket_message\" : { \"text\" : \"" + comment + "\"}}";
            String messageString = UserVoiceUtil.postUserVoiceData(consumer, url, "POST", "application/json", body);
            if(messageString != null){
	            JSONObject response = new JSONObject(messageString);
	            if (response.getJSONObject("ticket") != null) {
	                int messageCount = response.getJSONObject("ticket").getJSONArray("messages").length();
	                if (messageCount > 0) {
	                    JSONArray messageArray = response.getJSONObject("ticket").getJSONArray("messages");
	                    for (int i = 0; i < messageArray.length(); i++) {
	                        JSONObject messageObj = messageArray.getJSONObject(i);
	                        //Check if message already exists, if not add
	                        Message message = getMessageFromJson(messageObj, ticketId);
	                        if(PropertyUtil.getMessageMappingByTicketMessageId(message.getId(), ao) == null){
	                             PropertyUtil.saveMessageMapping(issueComment.getId(), message.getId(), ticketId, ao);
	                        }
	                    }
	                }
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return created;
    }

    public boolean deleteComment(CommentMapping commentMapping){
        boolean deleted = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/").append(commentMapping.getForum())
                    .append("/suggestions/").append(commentMapping.getFeedback()).append("/comments").append("/").append(commentMapping.getFeedbackComment()).append(".json").toString();
            String body = "";
            UserVoiceUtil.postUserVoiceData(consumer, url, "DELETE", "application/json", body);
            CommentMapping[] mappings = new CommentMapping[] { commentMapping };
            ao.delete(mappings);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return deleted;
    }

    /* Changes for JUVP-28 - start */
    @Override
    public List<Ticket> getTickets(Date lastRun) {
    	SimpleDateFormat uvDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        uvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    	
        List<Ticket> tickets = new ArrayList<Ticket>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        
        try {
        	String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets.json?client=")
        			.append(config.getConsumerKey())
        			.append("&filter=updated_after&updated_after_date="+ URLEncoder.encode(uvDateFormatter.format(lastRun),"UTF-8")).toString();
        	
            String ticketString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            if(StringUtils.isNotBlank(ticketString)){
	            JSONObject response = new JSONObject(ticketString);
	            int ticketCount = Integer.parseInt(response.getJSONObject("response_data").getString("total_records"));
	            if (ticketCount > 0) {
	                JSONArray feedbackArray = response.getJSONArray("tickets");
	                for (int i = 0; i < feedbackArray.length(); i++) {
	                    JSONObject feedbackObj = feedbackArray.getJSONObject(i);
	                    tickets.add(getTicketFromJson(feedbackObj));
	                }
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return tickets;
    }
    
    @Override
    public boolean updateUVState(String ticketId, String state) {
    	boolean updated = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
        	
        	if(StringUtils.isNotBlank(state)){
	            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets/").append(ticketId).append(".json").toString();
	            String body = "{ \"ticket\" : { \"state\" : \"" + state + "\" } }";
	            String updatedString = UserVoiceUtil.postUserVoiceData(consumer, url, "PUT", "application/json", body);
	            
	            if(StringUtils.isNotBlank(updatedString)){
		            JSONObject response = new JSONObject(updatedString);
		            if (response.getJSONObject("ticket") != null) {
		            	updated =  true;
		            }
	            }
	        }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return updated;
    }
    
    @Override
    public boolean updateFeedbackStatus(String forumId, String feedbackId, String statusId, String statusName) {
    	boolean updated = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
        	
        	if(StringUtils.isNotBlank(statusName)){
	            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/")
	            		.append(forumId)
	            		.append("/suggestions/")
	            		.append(feedbackId).append("/respond.json").toString();
	            String body = "{ \"response\" : { \"status\" : \"" + statusName + "\" ," +
	            							  "   \"status_id\" : \"" + statusId + "\" } }";
	            String updatedString = UserVoiceUtil.postUserVoiceData(consumer, url, "PUT", "application/json", body);
	            
	            if(StringUtils.isNotBlank(updatedString)){
		            JSONObject response = new JSONObject(updatedString);
		            if (response.getJSONObject("suggestion") != null) {
		            	updated =  true;
		            }
	            }
	        }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return updated;
    }
    /* Changes for JUVP-28 - end */
    
    /* Changes for JUVP-37 - start */
    @Override
    public boolean updateFeedbackField(String forumId, String feedbackId, String fieldName, String modifiedText){
    	boolean updated = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
        	if(StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(modifiedText)){
	            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/")
	            		.append(forumId)
	            		.append("/suggestions/")
	            		.append(feedbackId).append(".json").toString();
	            
	            StringBuffer body=new StringBuffer("");
	            if("title".equalsIgnoreCase(fieldName)){
	            	body.append("{ \"suggestion\" : { ");
	            	body.append("\"title\" : \"" + modifiedText + "\"");
	            }else if("text".equalsIgnoreCase(fieldName)) {
	            	body.append("{ \"suggestion\" : { ");
	            	body.append("\"text\" : \"" + modifiedText + "\"");
	            }
	            if(StringUtils.isNotBlank(body.toString())) {
		            body.append(" } }");
		            
		            String updatedString = UserVoiceUtil.postUserVoiceData(consumer, url, "PUT", "application/json", body.toString());
		            
		            if(StringUtils.isNotBlank(updatedString)){
			            JSONObject response = new JSONObject(updatedString);
			            if (response.getJSONObject("suggestion") != null) {
			            	updated =  true;
			            }
		            }
	            }
	        }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return updated;
    }
    
    @Override
    public boolean updateTicketField(String ticketId, String fieldName, String modifiedText){
    	boolean updated = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        consumer.setTokenWithSecret(config.getOAuthToken(), config.getOAuthSecret());
        try {
        	if(StringUtils.isNotBlank(fieldName) && StringUtils.isNotBlank(modifiedText)){
	            String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets/")
	            		.append(ticketId).append(".json").toString();
	            StringBuffer body=new StringBuffer("");
	            if("title".equalsIgnoreCase(fieldName)){
	            	body.append("{ \"ticket\" : { ");
	            	body.append("\"subject\" : \"" + modifiedText + "\"");
	            }
	            if("custom_field".equalsIgnoreCase(fieldName)){
//	            	ticket[custom_field_values][_field_name_]
	            	body.append("{ \"ticket\" : { ");
	            	body.append("\"custom_field_values\" : ");
	            	body.append(modifiedText);
	            	body.append("");
	            }
	            if(StringUtils.isNotBlank(body.toString())) {
		            body.append(" } }");
		            
		            String updatedString = UserVoiceUtil.postUserVoiceData(consumer, url, "PUT", "application/json", body.toString());
		            if(StringUtils.isNotBlank(updatedString)){
			            JSONObject response = new JSONObject(updatedString);
			            if (response.getJSONObject("ticket") != null) {
			            	updated =  true;
			            }
		            }
	            }
	        }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return updated;
    }
    /* Changes for JUVP-37 - end */
    
    private Comment getCommentFromJson(JSONObject commentObj) throws JSONException {
        Comment comment = new Comment();
        comment.setId(commentObj.getString("id"));
        comment.setFeedbackId(commentObj.getString("suggestion_id"));
        comment.setForumId(commentObj.getString("forum_id"));
        comment.setText(commentObj.getString("text"));
        comment.setCreator(getUserFromJSON(commentObj.getJSONObject("creator")));
        return comment;
    }
    
    private Message getMessageFromJson(JSONObject messageObject, String ticketId) throws JSONException, IOException {
        Message message = new Message();
        message.setId(messageObject.getString("id"));
        message.setTicketId(ticketId);
        /* Changes for JUVP-36 - start */
        message.setBody(new String(messageObject.getString("body").getBytes(), "UTF-8"));
        message.setPlaintextBody(new String(messageObject.getString("plaintext_body").getBytes(), "UTF-8"));
        /* Changes for JUVP-36 - end */
        if(messageObject.has("recipient"))
        {
            message.setRecipient(getUserFromJSON(messageObject.getJSONObject("recipient")));
        }
        if(messageObject.has("sender"))
        {
            message.setSender(getUserFromJSON(messageObject.getJSONObject("sender")));
        }
        message.setCreatedAt(getTime(messageObject.getString("created_at")));
        message.setUpdatedAt(getTime(messageObject.getString("updated_at")));
        return message;
    }

    private int getFeedbackPerPage(String forumId, List<Feedback> feedbacks, int page, UserVoiceConfig config,
                                   OAuthConsumer consumer) {
        String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/forums/").append(forumId)
                .append("/suggestions.json?filter=published").append("&page=").append(page)
                .append("&per_page=100&client=").append(config.getConsumerKey()).toString();
        try {
            String feedbackString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
            JSONObject response = new JSONObject(feedbackString);
            int feedbackCnt = Integer.parseInt(response.getJSONObject("response_data").getString("total_records"));
            if (feedbackCnt > 0) {
                JSONArray feedbackArray = response.getJSONArray("suggestions");
                for (int i = 0; i < feedbackArray.length(); i++) {
                    JSONObject feedbackObj = feedbackArray.getJSONObject(i);
                    feedbacks.add(getFeedbackFromJson(feedbackObj));
                }
                int noOfPages = feedbackCnt / 100;
                if (feedbackCnt % 100 > 0) {
                    noOfPages++;
                }
                if (page < noOfPages) {
                    return page + 1;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    /* Changes for JUVP-28 - start */
	private int getUpdatedFeedbackPerPage(String forumId, List<Feedback> feedbacks,
			int page, UserVoiceConfig config, OAuthConsumer consumer, Date lastRun) {
		
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		try {
			String url = new StringBuffer().append(config.getUserVoiceUrl())
					.append("/api/v1/forums/").append(forumId)
					.append("/suggestions.json?filter=updated_after")
					.append("&updated_after_date=" + URLEncoder.encode(sdf.format(lastRun), "UTF-8"))
					.append("&page=")
					.append(page).append("&per_page=100&client=")
					.append(config.getConsumerKey()).toString();
			
			String feedbackString = UserVoiceUtil.getUserVoiceData(consumer,
					url, "GET");
			if(StringUtils.isNotBlank(feedbackString)){
				JSONObject response = new JSONObject(feedbackString);
				int feedbackCnt = Integer.parseInt(response.getJSONObject(
						"response_data").getString("total_records"));
				if (feedbackCnt > 0) {
					JSONArray feedbackArray = response.getJSONArray("suggestions");
					for (int i = 0; i < feedbackArray.length(); i++) {
						JSONObject feedbackObj = feedbackArray.getJSONObject(i);
						feedbacks.add(getFeedbackFromJson(feedbackObj));
					}
					int noOfPages = feedbackCnt / 100;
					if (feedbackCnt % 100 > 0) {
						noOfPages++;
					}
					if (page < noOfPages) {
						return page + 1;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	@Override
	public List<FeedbackStatus> getFeedbackStatuses(UserVoiceConfig config, OAuthConsumer consumer){
		List<FeedbackStatus> statuses=new ArrayList<FeedbackStatus>();
		try {
			String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/statuses.json?client=")
					.append(config.getConsumerKey())
					.toString();
			
			String feedbackString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
			if(StringUtils.isNotBlank(feedbackString)) {
				JSONObject response = new JSONObject(feedbackString);
				int feedbackCnt = Integer.parseInt(response.getJSONObject(
						"response_data").getString("total_records"));
				if (feedbackCnt > 0) {
					FeedbackStatus uvStatus=null;
					JSONArray feedbackArray = response.getJSONArray("statuses");
					for (int i = 0; i < feedbackArray.length(); i++) {
						JSONObject feedbackObj = feedbackArray.getJSONObject(i);
						uvStatus = new FeedbackStatus();
						uvStatus.setId(feedbackObj.getString("id"));
						uvStatus.setEvent(feedbackObj.getString("event"));
						uvStatus.setName(feedbackObj.getString("name"));
						
						statuses.add(uvStatus);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}  catch (JSONException e) {
			e.printStackTrace();
		}
		
		return statuses;
	}
	/* Changes for JUVP-28 - end */
    
	/* Changes for JUVP-22 - start */
	@Override
	public List<UserVoiceCustomField> getUVCustomFields(UserVoiceConfig config, OAuthConsumer consumer){
		List<UserVoiceCustomField> uvCustomFields=new ArrayList<UserVoiceCustomField>();
		try {
			String url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/custom_fields.json?client=")
					.append(config.getConsumerKey())
					.toString();
			
			String customFieldString = UserVoiceUtil.getUserVoiceData(consumer, url, "GET");
			if(StringUtils.isNotBlank(customFieldString)) {
				JSONObject response = new JSONObject(customFieldString);
				int customFieldsCount = Integer.parseInt(response.getJSONObject(
						"response_data").getString("total_records"));
				if (customFieldsCount > 0) {
					UserVoiceCustomField uvCustomField=null;
					JSONArray customFields = response.getJSONArray("custom_fields");
					for (int i = 0; i < customFields.length(); i++) {
						JSONObject customField = customFields.getJSONObject(i);
						uvCustomField = new UserVoiceCustomField();
						uvCustomField.setId(customField.getString("id"));
						uvCustomField.setName(customField.getString("name"));
						uvCustomField.setDescription(customField.getString("description"));
						
						uvCustomFields.add(uvCustomField);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}  catch (JSONException e) {
			e.printStackTrace();
		}
		
		return uvCustomFields;
	}
	/* Changes for JUVP-22 - end */
	
    private Feedback getFeedbackFromJson(JSONObject feedbackObj) throws JSONException, IOException {
        Feedback feedback = new Feedback();
        feedback.setId(feedbackObj.getString("id"));
        feedback.setTitle(new String(feedbackObj.getString("title").getBytes(), "UTF-8"));
        feedback.setText(new String(feedbackObj.getString("text").getBytes(), "UTF-8"));
        feedback.setUrl(feedbackObj.getString("url"));
        feedback.setState(feedbackObj.getString("state"));
        feedback.setVotes(Integer.parseInt(feedbackObj.getString("vote_count")));
        feedback.setComments(Integer.parseInt(feedbackObj.getString("comments_count")));
        feedback.setSupporters(Integer.parseInt(feedbackObj.getString("supporters_count")));
        if (feedbackObj.get("status") instanceof JSONObject) {
            feedback.setStatus(feedbackObj.getJSONObject("status").getString("name"));
        }
        feedback.setUrl(feedbackObj.getString("url"));

        User creator = getUserFromJSON(feedbackObj.getJSONObject("creator"));
        feedback.setCreatedBy(creator);
        if (feedbackObj.has("status_changed_by")) {
            User statusChangedBy = getUserFromJSON(feedbackObj.getJSONObject("status_changed_by"));
            feedback.setStatusChangedBy(statusChangedBy);
        }

        feedback.setCreatedDate(getTime(feedbackObj.getString("created_at")));
        feedback.setUpdatedDate(getTime(feedbackObj.getString("updated_at")));
        feedback.setClosedDate(getTime(feedbackObj.getString("closed_at")));
        return feedback;
    }

    private Ticket getTicketFromJson(JSONObject feedbackObj) throws JSONException, IOException {
        Ticket ticket = new Ticket();
        ticket.setId(feedbackObj.getString("id"));
        ticket.setTicketNumber(feedbackObj.getString("ticket_number"));
        ticket.setUrl(feedbackObj.getString("url"));
        ticket.setSubject(new String(feedbackObj.getString("subject").getBytes(), "UTF-8"));
        ticket.setState(feedbackObj.getString("state"));
        
        /* Changes for JUVP-22 - start */
        UserVoiceCustomFieldValue uvCustomField = null;
        JSONArray customFields = feedbackObj.getJSONArray("custom_fields");
		for (int i = 0; i < customFields.length(); i++) {
			JSONObject customField = customFields.getJSONObject(i);
			uvCustomField = new UserVoiceCustomFieldValue();
			uvCustomField.setCustomFieldKey(customField.getString("key"));
			uvCustomField.setValue(customField.getString("value"));
			
			ticket.addCustomField(uvCustomField);
		}
		/* Changes for JUVP-22 - end */
		/* Satisfaction rating - start */
		JSONArray messages = feedbackObj.getJSONArray("messages");
		if(messages != null){
			for (int i = 0; i < messages.length(); i++) {
				JSONObject message = messages.getJSONObject(i);
				if(message != null){
					JSONObject sender = message.getJSONObject("sender"); 
					if(sender != null){
						JSONObject rating = sender.getJSONObject("satisfaction_rating");
						if(rating != null){
							uvCustomField = new UserVoiceCustomFieldValue();
							uvCustomField.setCustomFieldKey("User Satisfaction Rating");
							uvCustomField.setValue(rating.getString("score"));
							ticket.addCustomField(uvCustomField);
							break;
						}
					}
				}
			}
		}
		/* Satisfaction rating - end */
		
        User createdBy = getUserFromJSON(feedbackObj.getJSONObject("created_by"));
        ticket.setCreatedBy(createdBy);
        ticket.setCreatedAt(getTime(feedbackObj.getString("created_at")));
        ticket.setUpdatedAt(getTime(feedbackObj.getString("updated_at")));
        return ticket;
    }

    private User getUserFromJSON(JSONObject userObj) throws JSONException {
        User user = new User();
        user.setId(userObj.getString("id"));
        user.setName(userObj.getString("name"));
        user.setEmail(userObj.getString("email"));
        user.setTitle(userObj.getString("title"));
        user.setUrl(userObj.getString("url"));
        return user;
    }

    private Date getTime(String time) {
        if (time != null && !time.equals("null")) {
            try {
                return this.simpleDateFormat.parse(time);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public enum Event {
        suggestion, comment, vote, suggestion_status, suggestion_merge, note, ticket, ticket_message
    }

	@Override
	public List<Ticket> searchTickets(Date lastRun, String filter, boolean firstSearch) {
		int page=1;
		int recordsPerPage=10;
		SimpleDateFormat uvDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        uvDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    	
        List<Ticket> tickets = new ArrayList<Ticket>();
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
        
        try {
        	StringBuffer url = new StringBuffer().append(config.getUserVoiceUrl()).append("/api/v1/tickets/search.json?client=").append(config.getConsumerKey());
			url.append("&page="+page);
			url.append("&per_page=" + recordsPerPage);
			if(lastRun == null){
				url.append("&query=" + URLEncoder.encode(filter,"UTF-8"));
			}else{
				StringBuffer filterText=new StringBuffer(filter);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				String startDate="";
				if(firstSearch){
					startDate="created_start";
				}else{
					startDate="updated_start";
				}
				if(filter.indexOf(startDate) >= 0){
					int startIndex=filter.indexOf(startDate) + startDate.length() + 1;
					int endIndex=filter.indexOf(" ", startIndex);
					
					filterText.replace(startIndex, endIndex, sdf.format(lastRun));
				}else{
					filterText.append(" " + startDate + ":"+ sdf.format(lastRun));
				}
				
				url.append("&query=" + URLEncoder.encode(filterText.toString(),"UTF-8"));
			}
			
        	while(true){
        		int startIndex=url.indexOf("page");
        		int endIndex=url.indexOf("&", startIndex);
        		
        		url.replace(startIndex, endIndex, "page=" + page);
	            String ticketString = UserVoiceUtil.getUserVoiceData(consumer, url.toString(), "GET");
	            if(StringUtils.isNotBlank(ticketString)){
		            JSONObject response = new JSONObject(ticketString);
		            int ticketCount = Integer.parseInt(response.getJSONObject("response_data").getString("total_records"));
		            if (ticketCount > 0) {
		                JSONArray feedbackArray = response.getJSONArray("tickets");
		                for (int i = 0; i < feedbackArray.length(); i++) {
		                    JSONObject feedbackObj = feedbackArray.getJSONObject(i);
		                    tickets.add(getTicketFromJson(feedbackObj));
		                }
		                int noOfPages = (int)Math.ceil((1.0 * ticketCount) / recordsPerPage);
		            	if(page < noOfPages ){
		            		page++;
		            	}else{
		            		break;
		            	}
		            }else{
		            	break;
		            }
	            }else{
	            	break;
	            }
        	}
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return tickets;
	}
}
