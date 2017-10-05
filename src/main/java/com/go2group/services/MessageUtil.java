package com.go2group.services;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.go2group.uservoice.bean.Message;
import com.go2group.util.PropertyUtil;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageUtil {
    public static void createMessage(ActiveObjects ao, Issue issue, ApplicationUser jiraAdmin, Message message) {
        I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(jiraAdmin);
        String messageText = helper.getText("jira.comment.text", message.getSender().getName(), message.getSender()
                .getUrl(), message.getBody());
        com.atlassian.jira.issue.comments.Comment comment = ComponentAccessor.getCommentManager().create(issue, jiraAdmin.getName(),
                messageText, false);
        PropertyUtil.saveMessageMapping(comment.getId(), message.getId(), message.getTicketId(), ao);
    }
}
