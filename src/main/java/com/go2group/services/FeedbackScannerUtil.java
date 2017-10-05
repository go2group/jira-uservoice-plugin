package com.go2group.services;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.I18nHelper;
import com.go2group.entity.CommentMapping;
import com.go2group.entity.FeedbackMapping;
import com.go2group.entity.Mapping;
import com.go2group.entity.MappingConfig;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.manager.G2GManager;
import com.go2group.uservoice.bean.Comment;
import com.go2group.uservoice.bean.Feedback;
import com.go2group.util.PropertyUtil;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 10:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class FeedbackScannerUtil {

    private static Logger log = Logger.getLogger(FeedbackScannerUtil.class);

    public static void run(ActiveObjects ao, UserVoiceConfig userVoiceConfig, G2GManager g2gManager){
    	ApplicationUser jiraAdmin = UserUtils.getUser(userVoiceConfig.getJiraAdmin());
        I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(jiraAdmin);
        Date lastRun = userVoiceConfig.getLastRun();
        // Date lastRun = DateUtils.addDays(now, -40);
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(jiraAdmin);
        Mapping[] mappings = PropertyUtil.getMappings(ao);
        for (Mapping mapping : mappings) {
            String project = mapping.getJiraProject();
            String jiraProjectId = project.substring(project.lastIndexOf('(') + 1, project.lastIndexOf(')'));
            String jiraIssueTypeId = mapping.getJiraIssueType();
            String uvForum = mapping.getUvForum();
            String forumId = uvForum.substring(uvForum.lastIndexOf('(') + 1, uvForum.lastIndexOf(')'));
            String uvType = mapping.getUvType();

            MappingConfig config = PropertyUtil.getMappingConfig(mapping.getID(), ao);
            if (config != null) {
                String allowedStatuses = config.getAllowedStatus();
                String[] statuses = StringUtils.isBlank(allowedStatuses) ? new String[0] : allowedStatuses.split(",");
                List<String> statusList = Arrays.asList(statuses);

                if (uvType.equals("Feedback")) {
                    // Get All feedbacks if syncPrior Entities is true and not
                    // yet synced
                    if (isTrue(config.getSyncPriorEntities()) && !isTrue(config.getSyncedOnce())) {
                        List<Feedback> feedbacks = g2gManager.getUserVoiceManager().getFeedbacks(forumId);
                        log.debug("Got " + feedbacks.size() + " feedbacks");
                        for (Feedback feedback : feedbacks) {
                            if (statusList.size() == 0 || statusList.contains(feedback.getStatus())) {
                                FeedbackUtil.createFeedback(ao, g2gManager, jiraAdmin, jiraProjectId, jiraIssueTypeId, forumId, feedback, baseUrl, config);
                            }
                        }
                        // Set syncedOnce to true
                        config.setSyncedOnce(true);
                        config.save();
                    } else if (lastRun != null) { // Get feedbacks after the
                        // last run
                        List<Object> events = g2gManager.getUserVoiceManager().getEvents(forumId, lastRun);
                        log.debug("Got " + events.size() + " events");
                        for (Object event : events) {
                            if (event instanceof Feedback) {
                                Feedback feedback = (Feedback) event;
                                if (statusList.size() == 0 || statusList.contains(feedback.getStatus())) {
                                    FeedbackUtil.createFeedback(ao, g2gManager, jiraAdmin, jiraProjectId, jiraIssueTypeId, forumId, feedback,
                                            baseUrl, config);
                                }
                            } else if (event instanceof Comment) {
                                Comment comment = (Comment) event;
                                CommentMapping commentMapping = PropertyUtil.getCommentMappingByFeedbackComment(
                                        comment.getId(), ao);
                                if (commentMapping == null) {
                                    FeedbackMapping feedbackMapping = PropertyUtil.getFeedbackMappingByFeedback(
                                            comment.getFeedbackId(), ao);
                                    if (feedbackMapping != null) {
                                        IssueService.IssueResult issueResult = ComponentAccessor.getIssueService().getIssue(jiraAdmin,
                                                feedbackMapping.getIssue());
                                        if (issueResult != null) {
                                            CommentUtil.createComment(ao, issueResult.getIssue(), jiraAdmin, forumId, comment);
                                        }
                                    }
                                }
                            }
                        }
                        
                        /* Changes for JUVP-28 - start */
                        List<Feedback> feedbacks = g2gManager.getUserVoiceManager().getFeedbacks(forumId, lastRun);
                        for (Feedback feedback : feedbacks) {
                        	FeedbackUtil.updateFeedbackStatus(ao, g2gManager, jiraAdmin, jiraProjectId, jiraIssueTypeId, forumId, feedback, config);
                        }
                        /* Changes for JUVP-28 - end */
                    }
                }
            }
        }
    }

    private static Boolean isTrue(Boolean bool) {
        return bool!=null && bool.booleanValue();
    }
}
