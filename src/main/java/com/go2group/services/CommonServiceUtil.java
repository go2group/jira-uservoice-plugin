package com.go2group.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.MultipleSettableCustomFieldType;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.go2group.entity.FbStatusMap;
import com.go2group.entity.TicketStatusMap;
import com.go2group.entity.TktCustomFieldMap;
import com.go2group.uservoice.bean.Feedback;
import com.go2group.uservoice.bean.Ticket;
import com.go2group.uservoice.bean.UserVoiceCustomFieldValue;
import com.go2group.util.ApplicationUtil;
import com.go2group.util.PropertyUtil;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult;
import com.atlassian.jira.bc.issue.IssueService.UpdateValidationResult;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 10:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class CommonServiceUtil {

    private static Logger log = Logger.getLogger(CommonServiceUtil.class);

    public static void logErrors(ErrorCollection errors) {
        Collection<String> errorMessages = errors.getErrorMessages();
        for (String errorMessage : errorMessages) {
            log.error(errorMessage);
        }
        Map<String, String> errorMap = errors.getErrors();
        for (String errorKey : errorMap.keySet()) {
            log.error("Error for " + errorKey + ": " + errorMap.get(errorKey));
        }
    }

    public static MutableIssue createIssue(String jiraProjectId, String jiraIssueTypeId, ApplicationUser jiraAdmin, Feedback feedback, FbStatusMap[] fbStatusMaps) {
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project project = projectManager.getProjectObj(new Long(jiraProjectId));
//        Long assigneeType = ComponentAccessor.getProjectManager().getProjectObj(new Long(jiraProjectId)).getAssigneeType();
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters inputParameters = issueService.newIssueInputParameters();
        inputParameters.setProjectId(new Long(jiraProjectId));
        inputParameters.setIssueTypeId(jiraIssueTypeId);
        inputParameters.setSummary(feedback.getTitle());
        //If default assignee is project lead, set assignee
        if(!ComponentAccessor.getApplicationProperties().getOption("jira.option.allowunassigned"))
            inputParameters.setAssigneeId(project.getProjectLead().getUsername());
        if (feedback.getText() != null && !"null".equals(feedback.getText()))
            inputParameters.setDescription(feedback.getText());
        inputParameters.setReporterId(jiraAdmin.getName());
        IssueService.CreateValidationResult validationResult = issueService.validateCreate(jiraAdmin, inputParameters);
        if (validationResult.isValid()) {
        	if(fbStatusMaps != null){
	            IssueService.IssueResult result = issueService.create(jiraAdmin, validationResult);
	            if (!result.isValid()) {
	                logErrors(result.getErrorCollection());
	            } else {
	            	
	            	MutableIssue issue = result.getIssue();
	            	/* Changes for JUVP-28 - start */
	            	String state = feedback.getState();
	            	FbStatusMap fbStatusMap=PropertyUtil.findFbStatusMapByUVState(state, fbStatusMaps);
	            	if(fbStatusMap != null){
		            	int actionId  = getActionIdForTransition(fbStatusMap.getJiraStatus(), issue);
		            	
		            	TransitionValidationResult transitionValidationResult = issueService.validateTransition(jiraAdmin, issue.getId(), actionId, inputParameters);
		        		if (transitionValidationResult.isValid()){
		        			IssueService.IssueResult transitionResult = issueService.transition(jiraAdmin, transitionValidationResult);
		        		    if (transitionResult.isValid()){
		        		    	issue = transitionResult.getIssue();
		        		    }else{
		        		    	logErrors(transitionResult.getErrorCollection());
		        		    }
		        		}else {
		                    logErrors(transitionValidationResult.getErrorCollection());
		                }
	            	}
	                return issue;
	                /* Changes for JUVP-28 - end */
	            }
        	}else{
        		log.warn("JIRA issue not created since UserVoice Feedback status mapping not configured.");
        	}
        } else {
            logErrors(validationResult.getErrorCollection());
        }
        return null;
    }

    /* Changes for JUVP-28 - start */
    public static MutableIssue updateIssue(String jiraProjectId, String jiraIssueTypeId, ApplicationUser jiraAdmin, Feedback feedback, MutableIssue issue, FbStatusMap[] fbStatusMaps) {
    	MutableIssue updatedIssue = null;
        IssueService issueService = ComponentAccessor.getIssueService();
        
        /* Changes for JUVP-37 - start */
        if(feedback != null && issue != null){
        	I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(jiraAdmin);
        	IssueInputParameters updateInputParameters = issueService.newIssueInputParameters();
        	if(StringUtils.isNotBlank(feedback.getTitle()) && !feedback.getTitle().equals(issue.getSummary())){
            	updateInputParameters.setSummary(feedback.getTitle());
            	String comment = helper.getText("uv.ticket.title.note", issue.getSummary());
            	updateInputParameters.setComment(comment);
            }
        	if(StringUtils.isNotBlank(feedback.getText()) && !feedback.getText().equals(issue.getDescription())){
            	updateInputParameters.setDescription(feedback.getText());
            	String comment = helper.getText("uv.ticket.description.note", issue.getDescription());
            	updateInputParameters.setComment(comment);
            }
        	
        	updateIssueFields(jiraProjectId, issue.getId(), jiraAdmin, updateInputParameters);
        }else{
        	return null;
        }
        /* Changes for JUVP-37 - end */
        
        IssueInputParameters inputParameters = issueService.newIssueInputParameters();
        inputParameters.setProjectId(new Long(jiraProjectId));
        inputParameters.setIssueTypeId(jiraIssueTypeId);
        inputParameters.setReporterId(jiraAdmin.getName());
        String state = feedback.getStatus();

        int actionId = -1;
		FbStatusMap fbStatusMap=PropertyUtil.findFbStatusMapByUVState(state, fbStatusMaps);
		if(fbStatusMap != null){
			actionId = getActionIdForTransition(fbStatusMap.getJiraStatus(), issue);
	        
	        if(actionId != -1){
	        	updatedIssue = update(jiraProjectId, jiraIssueTypeId, jiraAdmin, inputParameters, issue.getId(), actionId);
	        }
		}
        
        return updatedIssue;
    }
    /* Changes for JUVP-28 - end */
    
    /* Changes for JUVP-22 - start */
    private static String findJiraCustomFieldName(String uvFieldKey, TktCustomFieldMap[] tktCFMaps){
    	for(TktCustomFieldMap map: tktCFMaps){
    		if(map.getUvCustomFieldName().equals(uvFieldKey)){
    			return map.getJiraCustomFieldName();
    		}
    	}
    	return null;
    }
    
    private static CustomField findJiraCustomField(String uvFieldKey, TktCustomFieldMap[] tktCFMaps){
    	CustomField cf = null;
    	String jiraCustomFieldName = findJiraCustomFieldName(uvFieldKey, tktCFMaps);
    	if(StringUtils.isNotBlank(jiraCustomFieldName)){
    		cf = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(jiraCustomFieldName);
    	}
    	
    	return cf;
    }
    /* Changes for JUVP-22 - end */
    
    public static MutableIssue createIssue(String jiraProjectId, String jiraIssueTypeId, ApplicationUser jiraAdmin, Ticket ticket, TicketStatusMap[] ticketStatusMaps, TktCustomFieldMap[] tktCFMaps) {
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project project = projectManager.getProjectObj(new Long(jiraProjectId));
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters inputParameters = issueService.newIssueInputParameters();
        inputParameters.setProjectId(new Long(jiraProjectId));
        inputParameters.setIssueTypeId(jiraIssueTypeId);
        inputParameters.setSummary(ticket.getSubject());
        if(!ComponentAccessor.getApplicationProperties().getOption("jira.option.allowunassigned"))
            inputParameters.setAssigneeId(project.getProjectLead().getUsername());
        if (ticket.getSubject() != null && !"null".equals(ticket.getSubject()))
            inputParameters.setDescription(ticket.getSubject());
        inputParameters.setReporterId(jiraAdmin.getName());
        
        IssueService.CreateValidationResult validationResult = issueService.validateCreate(jiraAdmin, inputParameters);
        if (validationResult.isValid()) {
        	if(ticketStatusMaps != null){
        		IssueService.IssueResult result = issueService.create(jiraAdmin, validationResult);
	            if (!result.isValid()) {
	                logErrors(result.getErrorCollection());
	            } else {
	            	MutableIssue issue = result.getIssue();
	            	/* Changes for JUVP-28 - start */
	            	String state = ticket.getState();
	            	TicketStatusMap ticketStatusMap=PropertyUtil.findTicketStatusMapByUVState(state, ticketStatusMaps);
	            	if(ticketStatusMap != null){
		            	int actionId  = getActionIdForTransition(ticketStatusMap.getJiraStatus(), issue);
		            	
		            	TransitionValidationResult transitionValidationResult = issueService.validateTransition(jiraAdmin, issue.getId(), actionId, inputParameters);
		        		if (transitionValidationResult.isValid()){
		        			IssueService.IssueResult transitionResult = issueService.transition(jiraAdmin, transitionValidationResult);
		        		    if (transitionResult.isValid()){
		        		    	issue = transitionResult.getIssue();
		        		    }else{
		        		    	logErrors(transitionResult.getErrorCollection());
		        		    }
		        		}else {
		                    logErrors(transitionValidationResult.getErrorCollection());
		                }
	            	}
	            	/* Changes for JUVP-28 - end */
	            	
	            	/* Changes for JUVP-22 - start */
	            	IssueInputParameters updateInputParameters = issueService.newIssueInputParameters();
	                List<UserVoiceCustomFieldValue> uvCustomFields=ticket.getUvCustomFields();
	                if(uvCustomFields != null && uvCustomFields.size() > 0){
	                	if(tktCFMaps != null){
	        	        	for(UserVoiceCustomFieldValue uvCustomField: uvCustomFields){
	        	        		CustomField cf=findJiraCustomField(uvCustomField.getCustomFieldKey(), tktCFMaps);
	        	        		if(cf != null){
	        	            		Object jiraCFType = cf.getCustomFieldType();
	        	            		if(jiraCFType instanceof GenericTextCFType){
	        	            			inputParameters.addCustomFieldValue(cf.getId(), uvCustomField.getValue());
	        	            		}else if(jiraCFType instanceof MultipleSettableCustomFieldType){
	        	            			String uvCFValue = uvCustomField.getValue();
	        	            			Options options = ApplicationUtil.getOptions(cf, project, issue.getIssueTypeObject());
	        	            			if(options != null){
		        	            			for(Option option: options){
		    		            				if(option.getValue().equals(uvCFValue)){
		    		            					/*
		    		            					 * Update custom field
		    		            					 */
		    		            					updateInputParameters.addCustomFieldValue(cf.getId(), String.valueOf(option.getOptionId()));
		    		            					
		    		            					updateIssueFields(jiraProjectId, issue.getId(), jiraAdmin, updateInputParameters);
		    		            				}
		    		            			}
	        	            			}
	        	            		}
	                    		}
	        	        	}
	                	}else{
	                		log.warn("JIRA issue custom field not synched since UserVoice Ticket custom field mapping not configured.");
	                	}
	                }
	                /* Changes for JUVP-22 - end */
	                return issue;
	            }
        	}else{
        		log.warn("JIRA issue not created since UserVoice Ticket status mapping not configured.");
        	}
        } else {
            logErrors(validationResult.getErrorCollection());
        }
        return null;
    }

    /* Changes for JUVP-28 - start */
    public static MutableIssue updateIssue(String jiraProjectId, String jiraIssueTypeId, ApplicationUser jiraAdmin, Ticket ticket, MutableIssue issue,TicketStatusMap[] ticketStatusMaps, TktCustomFieldMap[] tktCFMaps) {
    	MutableIssue updatedIssue = null;
        IssueService issueService = ComponentAccessor.getIssueService();
        ProjectManager projectManager = ComponentAccessor.getProjectManager();
        Project project = projectManager.getProjectObj(new Long(jiraProjectId));
        IssueInputParameters inputParameters = issueService.newIssueInputParameters();
        inputParameters.setProjectId(new Long(jiraProjectId));
        inputParameters.setIssueTypeId(jiraIssueTypeId);
        
        
        /* Changes for JUVP-37 - start */
        if(ticket != null && issue != null){
        	IssueInputParameters updateInputParameters = issueService.newIssueInputParameters();
        	if(StringUtils.isNotBlank(ticket.getSubject()) && !ticket.getSubject().equals(issue.getSummary())){
            	updateInputParameters.setSummary(ticket.getSubject());
            }
        	
        	/* Changes for JUVP-22 - start */
            List<UserVoiceCustomFieldValue> uvCustomFields=ticket.getUvCustomFields();
            if(uvCustomFields != null && uvCustomFields.size() > 0){
            	if(tktCFMaps != null){
	            	for(UserVoiceCustomFieldValue uvCustomField: uvCustomFields){
	            		CustomField cf=findJiraCustomField(uvCustomField.getCustomFieldKey(), tktCFMaps);
	            		if(cf != null){
		            		Object jiraCFValue = issue.getCustomFieldValue(cf);
		            		Object jiraCFType = cf.getCustomFieldType();
		            		if(jiraCFType instanceof GenericTextCFType){
		            			String uvCFValue = uvCustomField.getValue();
		            			String cfValue = (String)jiraCFValue;
		                		if(StringUtils.isNotBlank(uvCFValue) && !uvCFValue.equals(cfValue)){
		                			ApplicationUtil.addToDoNotListen(String.valueOf(issue.getId()));
		    	            		updateInputParameters.addCustomFieldValue(cf.getId(), uvCustomField.getValue());
		                		}
		            		}else if(jiraCFType instanceof MultipleSettableCustomFieldType){
		            			String uvCFValue = uvCustomField.getValue();
		            			Option cfValue = (Option)jiraCFValue;
		            			if(StringUtils.isNotBlank(uvCFValue) && (cfValue != null && !uvCFValue.equals(cfValue.getValue()))){
			            			Options options = ApplicationUtil.getOptions(cf, project, issue.getIssueTypeObject());
			            			if(options != null){
				            			for(Option option: options){
				            				if(option.getValue().equalsIgnoreCase(uvCFValue)){
				            					ApplicationUtil.addToDoNotListen(String.valueOf(issue.getId()));
				            					updateInputParameters.addCustomFieldValue(cf.getId(), String.valueOf(option.getOptionId()));
				            				}
				            			}
			            			}
		            			}
		            		}
	            		}
	            	}
            	}else{
            		log.warn("JIRA issue custom field not synched since UserVoice Ticket custom field mapping not configured.");
            	}
            }
            /* Changes for JUVP-22 - end */
            
        	updateIssueFields(jiraProjectId, issue.getId(), jiraAdmin, updateInputParameters);
        }else{
        	return null;
        }
        /* Changes for JUVP-37 - end */
        
        String state = ticket.getState();

        int actionId = -1;
		TicketStatusMap ticketStatusMap=PropertyUtil.findTicketStatusMapByUVState(state, ticketStatusMaps);
		if(ticketStatusMap != null){
			actionId = getActionIdForTransition(ticketStatusMap.getJiraStatus(), issue);
	        
	        if(actionId != -1){
	        	updatedIssue = update(jiraProjectId, jiraIssueTypeId, jiraAdmin, inputParameters, issue.getId(), actionId);
	        }
		}
        
        return updatedIssue;
    }
    
    private static MutableIssue update(String jiraProjectId, String jiraIssueTypeId, ApplicationUser jiraAdmin, IssueInputParameters inputParameters, Long issueId, int actionId){
    	MutableIssue updatedIssue = null;
    	IssueService issueService = ComponentAccessor.getIssueService();
    	TransitionValidationResult transitionValidationResult = issueService.validateTransition(jiraAdmin, issueId, actionId, inputParameters);
		if (transitionValidationResult.isValid()){
			IssueService.IssueResult transitionResult = issueService.transition(jiraAdmin, transitionValidationResult);
		    if (transitionResult.isValid()){
		    	updatedIssue = transitionResult.getIssue();
		    }else{
		    	logErrors(transitionResult.getErrorCollection());
		    }
		}else {
            logErrors(transitionValidationResult.getErrorCollection());
        }
    	return updatedIssue;
    }
    
    public static MutableIssue findIssue(ApplicationUser jiraAdmin, Long issueId){
    	IssueService issueService = ComponentAccessor.getIssueService();
    	IssueService.IssueResult result = issueService.getIssue(jiraAdmin, issueId);
    	if(result != null){
    		return result.getIssue();
    	}
    	
    	return null;
    }
    /* Changes for JUVP-28 - end */
    
    /* Changes for JUVP-37 - start */
    private static MutableIssue updateIssueFields(String jiraProjectId, Long issueId, ApplicationUser jiraAdmin, IssueInputParameters inputParameters){
    	MutableIssue updatedIssue = null;
    	IssueService issueService = ComponentAccessor.getIssueService();
    	UpdateValidationResult updateValidationResult = issueService.validateUpdate(jiraAdmin, issueId, inputParameters);
		if (updateValidationResult.isValid()){
			IssueService.IssueResult updateResult = issueService.update(jiraAdmin, updateValidationResult);
		    if (updateResult.isValid()){
		    	updatedIssue = updateResult.getIssue();
		    }else{
		    	logErrors(updateResult.getErrorCollection());
		    }
		}else {
            logErrors(updateValidationResult.getErrorCollection());
        }
    	return updatedIssue;
    }
    /* Changes for JUVP-37 - end */
    
    public static void createRemoteLink(Issue issue, Feedback feedback, String forumId, ApplicationUser jiraAdmin) {
        RemoteIssueLinkService remoteIssueLinkService = ComponentAccessor.getComponent(RemoteIssueLinkService.class);
        final RemoteIssueLink remoteIssueLink = new RemoteIssueLinkBuilder().url(feedback.getUrl())
                .title(feedback.getTitle()).globalId(feedback.getId()).issueId(issue.getId())
                .relationship("Uservoice Link").applicationName("Uservoice").applicationType("Uservoice").build();

        RemoteIssueLinkService.CreateValidationResult validationResult = remoteIssueLinkService.validateCreate(
                jiraAdmin, remoteIssueLink);
        if (validationResult.isValid()) {
            RemoteIssueLinkService.RemoteIssueLinkResult result = remoteIssueLinkService.create(jiraAdmin, validationResult);
            if (!result.isValid()) {
                CommonServiceUtil.logErrors(result.getErrorCollection());
            }
        } else {
            CommonServiceUtil.logErrors(validationResult.getErrorCollection());
        }
    }

    public static void createRemoteLink(Issue issue, Ticket ticket, ApplicationUser jiraAdmin) {
        RemoteIssueLinkService remoteIssueLinkService = ComponentAccessor.getComponent(RemoteIssueLinkService.class);
        final RemoteIssueLink remoteIssueLink = new RemoteIssueLinkBuilder().url(ticket.getUrl())
                .title(ticket.getSubject()).globalId(ticket.getId()).issueId(issue.getId())
                .relationship("Uservoice Link").applicationName("Uservoice").applicationType("Uservoice").build();

        RemoteIssueLinkService.CreateValidationResult validationResult = remoteIssueLinkService.validateCreate(
                jiraAdmin, remoteIssueLink);
        if (validationResult.isValid()) {
            RemoteIssueLinkService.RemoteIssueLinkResult result = remoteIssueLinkService.create(jiraAdmin, validationResult);
            if (!result.isValid()) {
                CommonServiceUtil.logErrors(result.getErrorCollection());
            }
        } else {
            CommonServiceUtil.logErrors(validationResult.getErrorCollection());
        }
    }
    
    private static int getActionIdForTransition(String targetStatus, Issue issue) {
    	WorkflowManager wfManager=ComponentAccessor.getWorkflowManager();
        JiraWorkflow workflow = wfManager.getWorkflow(issue);

        Status targetStatusObj = ComponentAccessor.getConstantsManager().getStatusByName(targetStatus);
        log.debug("Target Status Object : "+targetStatusObj.getName());
        StepDescriptor targetStepDescriptor = workflow.getLinkedStep(targetStatusObj);
        if (targetStepDescriptor == null){
              //Which means no linked step, so just return -1
              return -1;
        }
        int targetStepId = targetStepDescriptor.getId();
        log.debug("The linked step Id for the target status "+targetStatus+" is "+targetStepId);
        StepDescriptor stepDescriptor = workflow.getLinkedStep(issue.getStatusObject());
        List<ActionDescriptor> actionsList = stepDescriptor.getActions();
        
        for (ActionDescriptor action : actionsList){
              int resultantStepId = action.getUnconditionalResult().getStep();
              if (resultantStepId == targetStepId){
                   return action.getId();//There you go, this is what we are hunting for
              }
        }

        //return -1 if no right action is matched
        return -1;
  }
}
