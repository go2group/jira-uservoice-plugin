package com.go2group.services;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.I18nHelper;
import com.go2group.entity.IssueTicket;
import com.go2group.entity.MessageMapping;
import com.go2group.entity.TickMappingConf;
import com.go2group.entity.TicketMapping;
import com.go2group.entity.UVTicketFilter;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.manager.G2GManager;
import com.go2group.uservoice.bean.Message;
import com.go2group.uservoice.bean.Ticket;
import com.go2group.util.PropertyUtil;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class TicketScannerUtil {
    private static Logger log = Logger.getLogger(TicketScannerUtil.class);

    public static boolean run(ActiveObjects ao, UserVoiceConfig userVoiceConfig, G2GManager g2gManager){
    	boolean status=true;
    	ApplicationUser jiraAdmin = UserUtils.getUser(userVoiceConfig.getJiraAdmin());
        I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(jiraAdmin);
        Date lastRun = userVoiceConfig.getLastRun();
        // Date lastRun = DateUtils.addDays(now, -40);
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(jiraAdmin);
        TicketMapping[] mappings = PropertyUtil.getTicketMappings(ao);
        for (TicketMapping mapping : mappings) {
            String project = mapping.getJiraProject();
            String jiraProjectId = project.substring(project.lastIndexOf('(') + 1, project.lastIndexOf(')'));
            String jiraIssueTypeId = mapping.getJiraIssueType();

            TickMappingConf config = PropertyUtil.getTicketMappingConfig(mapping.getID(), ao);
            if (config != null) {
                //String allowedStatuses = config.getAllowedStatus();
                //String[] statuses = StringUtils.isBlank(allowedStatuses) ? new String[0] : allowedStatuses.split(",");
                //List<String> statusList = Arrays.asList(statuses);
            	
                if (isTrue(config.getSyncPriorEntities()) && !isTrue(config.getSyncedOnce())) {
                    List<Ticket> tickets = null;
                    
                    UVTicketFilter filter=PropertyUtil.getFilter(String.valueOf(mapping.getID()), ao);
                    if(filter != null && "true".equalsIgnoreCase(filter.getFilterOption())){
                    	String filterText=filter.getFilter();
            			filterText = filterText.replaceAll("_", " ");
            			filterText = filterText.replaceAll("\"User Satisfaction Rating\"", "satisfaction_rating");
                    	tickets = g2gManager.getUserVoiceManager().searchTickets(null, filterText, true);
                    }else{
                    	tickets = g2gManager.getUserVoiceManager().getTickets();
                    }
                    if(tickets != null){
	                    log.debug("Got " + tickets.size() + " tickets");
	                    for (Ticket ticket : tickets) {
	                        //if (statusList.size() == 0 || statusList.contains(feedback.getStatus())) {
	                    	boolean currentTicketStatus = TicketUtil.createTicket(ao, g2gManager, jiraAdmin, jiraProjectId, jiraIssueTypeId, ticket, userVoiceConfig, config);
	                    	if(currentTicketStatus == false){
	                    		log.warn("Ticket " + ticket.getTicketNumber() + " is not created successfully.");
	                    	}
	                    	status = status && currentTicketStatus;
	                        //}
	                    }
		                config.setSyncedOnce(true);
	                    config.save();
                    }
                } else if (lastRun != null) { // Get feedbacks after the last run
                	UVTicketFilter filter=PropertyUtil.getFilter(String.valueOf(mapping.getID()), ao);
                    if(filter != null && "true".equalsIgnoreCase(filter.getFilterOption())){
                    	String filterText=filter.getFilter();
            			filterText = filterText.replaceAll("_", " ");
            			filterText = filterText.replaceAll("\"User Satisfaction Rating\"", "satisfaction_rating");
            			List<Ticket> tickets = g2gManager.getUserVoiceManager().searchTickets(lastRun, filterText, true);
            			
            			if(tickets != null){
    	                    log.debug("Got " + tickets.size() + " tickets");
    	                    for (Ticket ticket : tickets) {
    	                    	boolean currentTicketStatus = TicketUtil.createTicket(ao, g2gManager, jiraAdmin, jiraProjectId, jiraIssueTypeId, ticket, userVoiceConfig, config);
    	                    	if(currentTicketStatus == false){
    	                    		log.warn("Ticket " + ticket.getTicketNumber() + " is not created successfully.");
    	                    	}
    	                    	status = status && currentTicketStatus;
    	                    }
                        }
                    }else{
	                    List<Object> events = g2gManager.getUserVoiceManager().getEvents(lastRun);
	                    log.debug("Got " + events.size() + " events");
	                    for (Object event : events) {
	                        if (event instanceof Ticket) {
	                            Ticket ticket = (Ticket) event;
	                            //if (statusList.size() == 0 || statusList.contains(feedback.getStatus())) {
	                            boolean currentTicketStatus = TicketUtil.createTicket(ao, g2gManager, jiraAdmin, jiraProjectId, jiraIssueTypeId, ticket, userVoiceConfig, config);
	                            if(!currentTicketStatus){
	                        		log.warn("Ticket " + ticket.getTicketNumber() + " is not created successfully.");
	                        	}
	                        	status = status && currentTicketStatus;
	                            //}
	                        } else if (event instanceof Message) {
	                            Message message = (Message) event;
	                            MessageMapping messageMapping = PropertyUtil.getMessageMappingByTicketMessageId(
	                                    message.getId(), ao);
	                            if (messageMapping == null) {
	                                IssueTicket ticketMapping = PropertyUtil.getMessageMappingByTicket(
	                                        message.getTicketId(), ao);
	                                if (ticketMapping != null) {
	                                    IssueService.IssueResult issueResult = ComponentAccessor.getIssueService().getIssue(jiraAdmin,
	                                            ticketMapping.getIssue());
	                                    if (issueResult != null) {
	                                        MessageUtil.createMessage(ao, issueResult.getIssue(), jiraAdmin, message);
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
                    
                    List<Ticket> tickets = null;
                    if(filter != null && "true".equalsIgnoreCase(filter.getFilterOption())){
                    	String filterText=filter.getFilter();
            			filterText = filterText.replaceAll("_", " ");
            			filterText = filterText.replaceAll("\"User Satisfaction Rating\"", "satisfaction_rating");
                    	tickets = g2gManager.getUserVoiceManager().searchTickets(lastRun, filterText, false);
                    }else{
                    	tickets = g2gManager.getUserVoiceManager().getTickets(lastRun);
                    }
                    if(tickets.size() > 0){
                    	for (Ticket ticket : tickets) {
	                    	boolean currentTicketStatus = TicketUtil.updateIssue(ao, g2gManager, jiraAdmin, jiraProjectId, jiraIssueTypeId, ticket, userVoiceConfig, config);
	                    	if(!currentTicketStatus){
	                    		log.warn("Ticket " + ticket.getTicketNumber() + " is not updated successfully.");
	                    	}
	                    	status = status && currentTicketStatus;
	                    }
                	}
                }
            }
        }
        
        return status;
    }

    private static Boolean isTrue(Boolean bool) {
        return bool!=null && bool.booleanValue();
    }
}
