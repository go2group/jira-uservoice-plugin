package com.go2group.services;

import java.util.List;

import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.go2group.entity.IssueTicket;
import com.go2group.entity.TickMappingConf;
import com.go2group.entity.TicketStatusMap;
import com.go2group.entity.TktCustomFieldMap;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.manager.G2GManager;
import com.go2group.uservoice.bean.Message;
import com.go2group.uservoice.bean.Ticket;
import com.go2group.util.PropertyUtil;

/**
 * Created with IntelliJ IDEA. User: bhushan Date: 31/10/13 Time: 12:02 PM To
 * change this template use File | Settings | File Templates.
 */
public class TicketUtil {
	private static Logger log = Logger.getLogger(TicketUtil.class);

	public static boolean createTicket(ActiveObjects ao,
                                    G2GManager g2gManager,
                                    ApplicationUser jiraAdmin,
                                    String jiraProjectId,
                                    String jiraIssueTypeId,
                                    Ticket uvTicket,
                                    UserVoiceConfig userVoiceConfig, TickMappingConf mappingConf) {
		boolean status=false;
        I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(jiraAdmin);
        String baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
        IssueTicket issueTicket = PropertyUtil.getTicketMappingByTicketAndProject(uvTicket.getId(), jiraProjectId, ao);
        if (issueTicket == null) { // If mapping doesn't exist already
        	TicketStatusMap[] ticketStatusMaps=PropertyUtil.getTicketStatusMaps(String.valueOf(mappingConf.getMappingId()), ao);
        	TktCustomFieldMap[] tktCFMaps = PropertyUtil.getTktCFMaps(String.valueOf(mappingConf.getMappingId()), ao);
            Issue jiraIssue = CommonServiceUtil.createIssue(jiraProjectId, jiraIssueTypeId, jiraAdmin, uvTicket, ticketStatusMaps, tktCFMaps);
            if (jiraIssue != null) {
            	status = true;
                CommonServiceUtil.createRemoteLink(jiraIssue, uvTicket, jiraAdmin);
                PropertyUtil.saveTicketMapping(jiraIssue.getId(), uvTicket.getId(), jiraProjectId, String.valueOf(mappingConf.getMappingId()), ao);
                List<Message> messages = g2gManager.getUserVoiceManager().getMessages(uvTicket.getId());
                log.debug("Got " + messages.size() + " messages");
                for (Message message : messages) {
                    MessageUtil.createMessage(ao, jiraIssue, jiraAdmin, message);
                }
                String note = helper.getText("jira.ticket.note", baseUrl, jiraIssue.getKey());
                boolean created = g2gManager.getUserVoiceManager().createTicketNote(uvTicket.getId(), note);
                if (!created) {
                    log.error("JIRA ticket reference not added as a note in Uservoice");
                }
            }
        }
        
        return status;
    }

	/* Changes for JUVP-28 - start */
	public static boolean updateIssue(ActiveObjects ao, G2GManager g2gManager,
			ApplicationUser jiraAdmin, String jiraProjectId, String jiraIssueTypeId,
			Ticket ticket, UserVoiceConfig userVoiceConfig, TickMappingConf mappingConf) {
        boolean status=false;
		IssueTicket issueTicket = PropertyUtil.getTicketMappingByTicketAndProject(ticket.getId(),jiraProjectId, ao);
		if (issueTicket != null) { 
			MutableIssue foundIssue = CommonServiceUtil.findIssue(jiraAdmin,issueTicket.getIssue());
			if (foundIssue != null) {
				TicketStatusMap[] ticketStatusMaps=PropertyUtil.getTicketStatusMaps(String.valueOf(mappingConf.getMappingId()), ao);
				TktCustomFieldMap[] tktCFMaps = PropertyUtil.getTktCFMaps(String.valueOf(mappingConf.getMappingId()), ao);
				Issue issue = CommonServiceUtil.updateIssue(jiraProjectId, jiraIssueTypeId, jiraAdmin, ticket, foundIssue, ticketStatusMaps, tktCFMaps);
				if (issue != null) {
					PropertyUtil.saveTicketMapping(issue.getId(), ticket.getId(), jiraProjectId, String.valueOf(mappingConf.getMappingId()), ao);
					status = true;
				}
			}
		}
		/* Changes for JUVP-28 - end */
		
		return status;
	}
}
