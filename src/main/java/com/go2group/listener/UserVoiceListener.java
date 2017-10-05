package com.go2group.listener;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.customfields.MultipleSettableCustomFieldType;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.resolution.Resolution;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.MapBuilder;
import com.go2group.entity.CommentMapping;
import com.go2group.entity.FbStatusMap;
import com.go2group.entity.FeedbackMapping;
import com.go2group.entity.IssueTicket;
import com.go2group.entity.Mapping;
import com.go2group.entity.TicketMapping;
import com.go2group.entity.TicketStatusMap;
import com.go2group.entity.TktCustomFieldMap;
import com.go2group.uservoice.manager.UserVoiceManager;
import com.go2group.util.ApplicationUtil;
import com.go2group.util.PropertyUtil;

public class UserVoiceListener implements InitializingBean, DisposableBean {

    private final Logger log = Logger.getLogger(UserVoiceListener.class);

    private final EventPublisher eventPublisher;
    private final UserVoiceManager userVoiceManager;
    private final ActiveObjects ao;
    private final OfBizDelegator delegator;

    public UserVoiceListener(EventPublisher eventPublisher, UserVoiceManager userVoiceManager, ActiveObjects ao,
                             OfBizDelegator delegator) {
        super();
        this.eventPublisher = eventPublisher;
        this.userVoiceManager = userVoiceManager;
        this.ao = ao;
        this.delegator = delegator;
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Issue issue = issueEvent.getIssue();
        FeedbackMapping mapping = PropertyUtil.getFeedbackMappingByIssue(issue.getId(), ao);
        if (mapping != null) {
            String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
            ApplicationUser user = UserUtils.getUser(PropertyUtil.getUserVoiceConfig(ao).getJiraAdmin());
            I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(user);

            if (issueEvent.getEventTypeId() == EventType.ISSUE_DELETED_ID) {
                FeedbackMapping[] mappings = new FeedbackMapping[] { mapping };
                ao.delete(mappings);
            } else if(issueEvent.getEventTypeId() == EventType.ISSUE_COMMENT_DELETED_ID){
                CommentMapping[] commentMappings = PropertyUtil.getCommentMappingsByFeedback(mapping.getFeedback(), ao);
                for(int i = 0; i < commentMappings.length; i++){
                    CommentMapping commentMapping = commentMappings[i];
                    if(ComponentAccessor.getCommentManager().getCommentById(commentMapping.getIssueComment()) == null){
                        this.userVoiceManager.deleteComment(commentMapping);
                    }
                }
            }
             else if(issueEvent.getEventTypeId() == EventType.ISSUE_COMMENTED_ID){
                createComment(issueEvent.getComment(), mapping, issueEvent.getComment().getBody());
            } else {
                GenericValue changeLog = issueEvent.getChangeLog();
                if (changeLog != null) {
                    Long id = changeLog.getLong("id");
                    List<GenericValue> changes = this.delegator.findByAnd("ChangeItem", MapBuilder.build("group", id));
                    if (changes != null) {
                        for (GenericValue change : changes) {
                        	try{
                                String fieldType = change.getString("fieldtype");
                                String field = change.getString("field");
                                if (fieldType.equals(ChangeItemBean.STATIC_FIELD)
                                        && field.equals(IssueFieldConstants.RESOLUTION)) {
                                    Resolution resolution = issue.getResolutionObject();
                                    if (resolution != null) {
                                        String note = helper.getText("jira.ticket.resolved.note", baseUrl, issue.getKey(),
                                                resolution.getName(), issue.getStatusObject().getName());
                                        createdNote(issue, mapping, note);
                                    } else {
                                        String note = helper.getText("jira.ticket.unresolved.note", baseUrl,
                                                issue.getKey(), issue.getStatusObject().getName());
                                        createdNote(issue, mapping, note);
                                    }
                                } else if (fieldType.equals(ChangeItemBean.STATIC_FIELD)
                                        && field.equals(IssueFieldConstants.SUMMARY)) {
                                	/* Changes for JUVP-37 - start */
                                	String originalValue = change.getString("oldstring");
                                	String note = helper.getText("jira.ticket.summary.note", originalValue, baseUrl, issue.getKey());
                                    
                                    createdNote(issue, mapping, note);
                                    updateFeedbackField(mapping, "title", issue.getSummary());
                                    /* Changes for JUVP-37 - end */
                                } else if (fieldType.equals(ChangeItemBean.STATIC_FIELD)
                                        && field.equals(IssueFieldConstants.DESCRIPTION)) {
                                	/* Changes for JUVP-37 - start */
                                	String originalValue = change.getString("oldstring");
                                    String note = helper.getText("jira.ticket.description.note", originalValue, baseUrl, issue.getKey());
                                    createdNote(issue, mapping, note);
                                    updateFeedbackField(mapping, "text", issue.getDescription());
                                    /* Changes for JUVP-37 - end */
                                }
                                /* Changes for JUVP-28 - start */
                                else if (fieldType.equals(ChangeItemBean.STATIC_FIELD)
                                        && field.equals(IssueFieldConstants.STATUS)) {
                                    Status status = issue.getStatusObject();
                                    if (status != null) {
                                    	Mapping projMapping = PropertyUtil.getMapping(issue.getProjectObject().getName() + " (" + issue.getProjectId() + ")", issue.getIssueTypeId(), ao);
                                    	if(projMapping != null){
                                        	FbStatusMap[] ticketStatusMaps = PropertyUtil.getFbStatusMaps(String.valueOf(projMapping.getID()), ao);
                                        	FbStatusMap fbStatusMap= PropertyUtil.findFbStatusMapByJiraStatusName(status.getName(), ticketStatusMaps);
                                        	
                                        	this.updateFeedbackStatus(mapping, fbStatusMap.getUvStatusId(), fbStatusMap.getUserVoiceStatus());
                                    	}
                                    }
                                }
                                /* Changes for JUVP-28 - end */
                            }catch(Exception excep){
                            	excep.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        IssueTicket issueTicket = PropertyUtil.getTicketMappingByIssue(issue.getId(), ao);
        
        if (issueTicket != null) {
            String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
            ApplicationUser user = UserUtils.getUser(PropertyUtil.getUserVoiceConfig(ao).getJiraAdmin());
            I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(user);

            if (issueEvent.getEventTypeId() == EventType.ISSUE_DELETED_ID) {
                //Uservoice API does not support message deletion
                //Do nothing
            }
            else if(issueEvent.getEventTypeId() == EventType.ISSUE_COMMENTED_ID){
                createdNoteForTicket(issue,issueTicket, issueEvent.getComment().getBody() );
            } else {
                GenericValue changeLog = issueEvent.getChangeLog();
                if (changeLog != null) {
                    Long id = changeLog.getLong("id");
                    List<GenericValue> changes = this.delegator.findByAnd("ChangeItem", MapBuilder.build("group", id));
                    if (changes != null) {
                        for (GenericValue change : changes) {
                        	try{
                                String fieldType = change.getString("fieldtype");
                                String field = change.getString("field");
                                if (fieldType.equals(ChangeItemBean.STATIC_FIELD)
                                        && field.equals(IssueFieldConstants.RESOLUTION)) {
                                    Resolution resolution = issue.getResolutionObject();
                                    if (resolution != null) {
                                        String note = helper.getText("jira.ticket.resolved.note", baseUrl, issue.getKey(),
                                                resolution.getName(), issue.getStatusObject().getName());
                                        createdNoteForTicket(issue, issueTicket, note);
                                    } else {
                                        String note = helper.getText("jira.ticket.unresolved.note", baseUrl,
                                                issue.getKey(), issue.getStatusObject().getName());
                                        createdNoteForTicket(issue, issueTicket, note);
                                    }
                                } else if (fieldType.equals(ChangeItemBean.STATIC_FIELD)
                                        && field.equals(IssueFieldConstants.SUMMARY)) {
                                	/* Changes for JUVP-37 - start */
                                	String originalValue = change.getString("oldstring");
                                    String note = helper.getText("jira.ticket.summary.note", originalValue, baseUrl, issue.getKey());
                                    
                                    createdNoteForTicket(issue, issueTicket, note);
                                    updateTicketField(issueTicket.getTicket(), "title", issue.getSummary());
                                    /* Changes for JUVP-37 - end */
                                } else if (fieldType.equals(ChangeItemBean.STATIC_FIELD)
                                        && field.equals(IssueFieldConstants.DESCRIPTION)) {
                                    String note = helper.getText("jira.ticket.description.note", baseUrl, issue.getKey());
                                    createdNoteForTicket(issue, issueTicket, note);
                                }
                                /* Changes for JUVP-28 - start */
                                else if (fieldType.equals(ChangeItemBean.STATIC_FIELD)
                                        && field.equals(IssueFieldConstants.STATUS)) {
                                    Status status = issue.getStatusObject();
                                    if (status != null) {
                                    	TicketMapping ticketMapping = PropertyUtil.getTicketMapping(issue.getProjectObject().getName() + " (" + issue.getProjectId() + ")", issue.getIssueTypeId(), ao);
                                    	if(ticketMapping != null){
                                        	TicketStatusMap[] ticketStatusMaps = PropertyUtil.getTicketStatusMaps(String.valueOf(ticketMapping.getID()), ao);
                                        	TicketStatusMap ticketStatusMap= PropertyUtil.findTicketStatusMapByJiraStatusName(status.getName(), ticketStatusMaps);
                                        	
                                        	this.updateUVState(issueTicket, ticketStatusMap.getUvStatusId());
                                    	}
                                    }
                                }
                                /* Changes for JUVP-28 - end */
                                /* Changes for JUVP-22 - start */
                                else if (fieldType.equals(ChangeItemBean.CUSTOM_FIELD)) {
                                	if(ApplicationUtil.doNotListen(String.valueOf(issue.getId()))){
                                		ApplicationUtil.removeDoNotListen(String.valueOf(issue.getId()));
                                		break;
                                	}
                                	String originalValue = change.getString("oldstring");
                                	String cfValue = getCustomFieldValue(issue, field);
                                	if(StringUtils.isNotBlank(originalValue)){
                                		if(originalValue.equals(cfValue)){
                                    		break;
                                    	}
                                	}
                                	
                                	TicketMapping ticketMapping = PropertyUtil.getTicketMapping(issue.getProjectObject().getName() + " (" + issue.getProjectId() + ")", issue.getIssueTypeId(), ao);
                                	if(ticketMapping != null){
                                		TktCustomFieldMap[] tktCFMaps = PropertyUtil.getTktCFMaps(String.valueOf(ticketMapping.getID()), ao);
                                    	String uvCFName=ApplicationUtil.getUvCFName(tktCFMaps,field);
                                    	if(StringUtils.isNotBlank(uvCFName)){
                                    		String note = helper.getText("jira.ticket.custom_field.note", field, originalValue, baseUrl, issue.getKey());
                                        	createdNoteForTicket(issue, issueTicket, note);
                                    		
                                    		updateTicketCustomField(issueTicket.getTicket(), uvCFName, cfValue);
                                    	}
                                	}
                                }
                                /* Changes for JUVP-22 - end */
                        	}catch(Exception excep){
                        		excep.printStackTrace();
                        	}
                        }
                    }
                }
            }
        }
    }

    private void createdNote(Issue issue, FeedbackMapping mapping, String note) {
        boolean created = this.userVoiceManager.createNote(mapping.getForum(), mapping.getFeedback(), note);
        if (!created) {
            log.error("Could not add note on the feedback for change in resolution of " + issue.getKey());
        }
    }

    private void createdNoteForTicket(Issue issue, IssueTicket issueTicket, String note) {
        boolean created = this.userVoiceManager.createNoteForTicket(issueTicket.getTicket(), "Comment posted on " + issue.getKey() + " by "+ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getDisplayName()+" : " + note);
        if (!created) {
            log.error("Could not add note on the feedback for change in resolution of " + issue.getKey());
        }
    }

    private void createMessage(Comment comment, IssueTicket issueTicket) {
        boolean created = this.userVoiceManager.createMessage(comment, issueTicket.getTicket(), comment.getBody());
        if (!created) {
            log.error("Could not add note on the feedback for change in resolution of " + comment.getIssue().getKey());
        }
    }

    private void createComment(Comment issueComment, FeedbackMapping mapping, String comment) {
        boolean created = this.userVoiceManager.createComment(issueComment.getId(), mapping.getForum(), mapping.getFeedback(), comment);
        if (!created) {
            log.error("Could not add comment on the feedback for issue " + issueComment.getIssue().getKey());
        }
    }
    
    /* Changes for JUVP-28 - start */
    private boolean updateUVState(IssueTicket issueTicket, String status){
    	boolean updated=false;
    	updated = this.userVoiceManager.updateUVState(issueTicket.getTicket(), status);
    	if(!updated){
    		log.error("Could not update status on the ticket for issue " + issueTicket.getTicket());
    	}
    	return updated;
    }
    
    private boolean updateFeedbackStatus(FeedbackMapping feedbackMapping, String statusId, String statusName){
    	boolean updated=false;
    	updated = this.userVoiceManager.updateFeedbackStatus(feedbackMapping.getForum(), feedbackMapping.getFeedback(), statusId, statusName);
    	if(!updated){
    		log.error("Could not update status on the ticket for feedback " + feedbackMapping.getFeedback());
    	}
    	return updated;
    }
    /* Changes for JUVP-28 - end */
    
    /* Changes for JUVP-37 - start */
    private boolean updateFeedbackField(FeedbackMapping feedbackMapping, String fieldName, String modifiedText){
    	boolean updated=false;
    	updated = this.userVoiceManager.updateFeedbackField(feedbackMapping.getForum(), feedbackMapping.getFeedback(), fieldName, modifiedText);
    	if(!updated){
    		log.error("Could not update the field " + fieldName + "of UserVoice for feedback " + feedbackMapping.getFeedback());
    	}
    	return updated;
    }
    
    private boolean updateTicketField(String ticketId, String fieldName, String modifiedText){
    	boolean updated=false;
    	updated = this.userVoiceManager.updateTicketField(ticketId, fieldName, modifiedText);
    	if(!updated){
    		log.error("Could not update the field " + fieldName + "of UserVoice for ticket " + ticketId);
    	}
    	return updated;
    }
    /* Changes for JUVP-37 - end */
    
    /* Changes for JUVP-22 - start */
    private boolean updateTicketCustomField(String ticketId, String fieldName, String fieldVale){
    	boolean updated=false;
    	String modifiedText = "{\"" + fieldName +  "\":\"" + fieldVale +"\"}";
    	updated = this.userVoiceManager.updateTicketField(ticketId, "custom_field", modifiedText);
    	if(!updated){
    		log.error("Could not update the field " + fieldName + "of UserVoice for ticket " + ticketId);
    	}
    	return updated;
    }
    
    private CustomField getCustomField(Issue issue, String fieldName){
    	CustomField cf = null;
    	TicketMapping ticketMapping = PropertyUtil.getTicketMapping(issue.getProjectObject().getName() + " (" + String.valueOf(issue.getProjectId()) + ")", issue.getIssueTypeId(), ao);
    	TktCustomFieldMap[] tktCFMaps = PropertyUtil.getTktCFMaps(String.valueOf(ticketMapping.getID()), ao);
    	for(TktCustomFieldMap map: tktCFMaps){
    		if(fieldName.equals(map.getJiraCustomFieldName())){
    			cf = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(fieldName);
    			break;
    		}
    	}
    	return cf;
    }
    
    private String getCustomFieldValue(Issue issue, String fieldName){
    	String cfValue = "";
		CustomField cf = getCustomField(issue, fieldName);
		Object jiraCFValue = issue.getCustomFieldValue(cf);
		Object jiraCFType = cf.getCustomFieldType();
		if(jiraCFType instanceof GenericTextCFType){
			cfValue = (String)jiraCFValue;
		}else if(jiraCFType instanceof MultipleSettableCustomFieldType){
			Option option = (Option)jiraCFValue;
			cfValue = option.getValue();
		}
    	return cfValue;
    }
    /* Changes for JUVP-22 - end */
    
    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }
}
