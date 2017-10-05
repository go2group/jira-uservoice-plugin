package com.go2group.services;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.scheduling.PluginJob;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.manager.G2GManager;
import com.go2group.uservoice.manager.UserVoiceManager;
import com.go2group.util.PropertyUtil;

public class UserVoiceService implements PluginJob {

    private Logger log = Logger.getLogger(UserVoiceService.class);

    private G2GManager g2gManager;
    private ActiveObjects ao;
    private UserVoiceManager userVoiceManager;
    private IssueService issueService;
    private RemoteIssueLinkService remoteIssueLinkService;
    private CommentManager commentManager;
    private I18nHelper helper;

    @Override
    public void execute(Map<String, Object> jobDataMap) {
        initialize();

        log.info("Running Uservoice service. Fetching the new Feedbacks....");
        UserVoiceConfig userVoiceConfig = PropertyUtil.getUserVoiceConfig(ao);
        if(userVoiceConfig != null){
            Date now = new Date();
            FeedbackScannerUtil.run(ao, userVoiceConfig, g2gManager);
            TicketScannerUtil.run(ao, userVoiceConfig, g2gManager);
            PropertyUtil.saveLastRun(now, ao);
            
            log.info("Uservoice service finished");
        }
        else{
            log.info("Uservoice plugin not configured. Service will not run.");
        }
    }


    private void initialize() {
        if (this.g2gManager == null) {
            this.g2gManager = ComponentAccessor.getOSGiComponentInstanceOfType(G2GManager.class);
            this.ao = this.g2gManager.getActiveObjects();
            this.userVoiceManager = this.g2gManager.getUserVoiceManager();
        }
        if (this.issueService == null)
            this.issueService = ComponentAccessor.getIssueService();
        if (this.remoteIssueLinkService == null)
            this.remoteIssueLinkService = ComponentAccessor.getComponent(RemoteIssueLinkService.class);
        if (this.commentManager == null)
            this.commentManager = ComponentAccessor.getCommentManager();
    }
}