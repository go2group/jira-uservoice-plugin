package com.go2group.services;

import java.util.List;

import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.go2group.entity.FbStatusMap;
import com.go2group.entity.FeedbackMapping;
import com.go2group.entity.MappingConfig;
import com.go2group.manager.G2GManager;
import com.go2group.uservoice.bean.Comment;
import com.go2group.uservoice.bean.Feedback;
import com.go2group.util.PropertyUtil;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 10:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackUtil {

    private static Logger log = Logger.getLogger(FeedbackUtil.class);

    public static void createFeedback(ActiveObjects ao, G2GManager g2gManager, ApplicationUser jiraAdmin, String jiraProjectId, String jiraIssueTypeId, String forumId,
                                Feedback feedback, String baseUrl, MappingConfig mappingConf) {
        I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(jiraAdmin);
        FeedbackMapping feedbackMapping = PropertyUtil.getFeedbackMappingByFeedback(feedback.getId(), ao);
        if (feedbackMapping == null) { // If mapping doesn't exist already
        	FbStatusMap[] fbStatusMaps = PropertyUtil.getFbStatusMaps(String.valueOf(mappingConf.getMappingId()), ao);
        	
            Issue issue = CommonServiceUtil.createIssue(jiraProjectId, jiraIssueTypeId, jiraAdmin, feedback, fbStatusMaps);
            if (issue != null) {
                CommonServiceUtil.createRemoteLink(issue, feedback, forumId, jiraAdmin);
                PropertyUtil.saveFeedbackMapping(issue.getId(), feedback.getId(), forumId, String.valueOf(mappingConf.getMappingId()), ao);
                List<Comment> comments = g2gManager.getUserVoiceManager().getComments(forumId, feedback.getId(),
                        "published");
                log.debug("Got " + comments.size() + " comments");
                for (Comment comment : comments) {
                    CommentUtil.createComment(ao, issue, jiraAdmin, forumId, comment);
                }
                String note = helper.getText("jira.ticket.note", baseUrl, issue.getKey());
                boolean created = g2gManager.getUserVoiceManager().createNote(forumId, feedback.getId(), note);
                if (!created) {
                    log.error("JIRA ticket reference not added as a note in Uservoice");
                }
            }
        }
    }
    
    /* Changes for JUVP-28 - start */
	public static void updateFeedbackStatus(ActiveObjects ao, G2GManager g2gManager, ApplicationUser jiraAdmin, String jiraProjectId, String jiraIssueTypeId, String forumId, Feedback feedback, MappingConfig mappingConf) {
//		I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(jiraAdmin);
		FeedbackMapping feedbackMapping = PropertyUtil.getFeedbackMappingByFeedback(feedback.getId(), ao);
		if (feedbackMapping != null) { 
			MutableIssue foundIssue = CommonServiceUtil.findIssue(jiraAdmin,feedbackMapping.getIssue());
			if (foundIssue != null) {
				FbStatusMap[] fbStatusMaps=PropertyUtil.getFbStatusMaps(String.valueOf(mappingConf.getMappingId()), ao);
				Issue issue = CommonServiceUtil.updateIssue(jiraProjectId,jiraIssueTypeId, jiraAdmin, feedback, foundIssue, fbStatusMaps);
				if (issue != null) {
					PropertyUtil.saveFeedbackMapping(issue.getId(),
							feedback.getId(), forumId, String.valueOf(mappingConf.getMappingId()), ao);
				}
			}
		}
	}
	/* Changes for JUVP-28 - end */
}
