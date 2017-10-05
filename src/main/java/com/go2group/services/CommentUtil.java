package com.go2group.services;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.go2group.uservoice.bean.Comment;
import com.go2group.util.PropertyUtil;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 31/10/13
 * Time: 10:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class CommentUtil {

    public static void createComment(ActiveObjects ao, Issue issue, ApplicationUser jiraAdmin, String forumId, Comment comment) {
        I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(jiraAdmin);
        String commentText = helper.getText("jira.comment.text", comment.getCreator().getName(), comment.getCreator()
                .getUrl(), comment.getText());
        com.atlassian.jira.issue.comments.Comment commentObj = ComponentAccessor.getCommentManager().create(issue, jiraAdmin.getName(),
                commentText, false);
        PropertyUtil.saveCommentMapping(commentObj.getId(), comment.getId(), comment.getForumId(), forumId, ao);
    }
}
