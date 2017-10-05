package com.go2group.jira.webwork;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.util.PropertyUtil;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;

public class ConfigureUserVoice extends JiraWebActionSupport {

    private final UserUtil userUtil;
    private final PermissionManager permissionManager;
    private final WebResourceManager webResourceManager;
    private final ApplicationProperties applicationProperties;
    private final ActiveObjects ao;

    private String jiraAdmin;
    private String userVoiceAdmin;
    private String consumerKey;
    private String sharedSecret;
    private String userVoiceUrl;
    private String reconfigure;

    public String getJiraAdmin() {
        return jiraAdmin;
    }

    public void setJiraAdmin(String jiraAdmin) {
        this.jiraAdmin = jiraAdmin;
    }

    public String getUserVoiceAdmin() {
        return userVoiceAdmin;
    }

    public void setUserVoiceAdmin(String userVoiceAdmin) {
        this.userVoiceAdmin = userVoiceAdmin;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public String getUserVoiceUrl() {
        return userVoiceUrl;
    }

    public void setUserVoiceUrl(String userVoiceUrl) {
        this.userVoiceUrl = userVoiceUrl;
    }

    /*
    **  Bhushan: 09/25/2013
    **  Used to display message if unassigned issues is turned off
    */
    private Boolean allowUnassigned;

    public Boolean getAllowUnassigned(){
        return allowUnassigned;
    }

    public void setAllowUnassigned(){
        this.allowUnassigned = ComponentAccessor.getApplicationProperties().getOption("jira.option.allowunassigned");
    }
    /*
    * End of code added by Bhushan on 09/25/2013
    */

    public ConfigureUserVoice(UserUtil userUtil, PermissionManager permissionManager,
                              WebResourceManager webResourceManager, ApplicationProperties applicationProperties, ActiveObjects ao) {
        this.userUtil = userUtil;
        this.permissionManager = permissionManager;
        this.webResourceManager = webResourceManager;
        this.applicationProperties = applicationProperties;
        this.ao = ao;
        this.webResourceManager.requireResource("com.atlassian.auiplugin:ajs");
    }

    @Override
    public String doDefault() throws Exception {

        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        if (getReconfigure() == null && config != null && config.getOAuthToken() != null
                && config.getOAuthSecret() != null) {
            return getRedirect(this.applicationProperties.getString("jira.baseurl")
                    + "/secure/admin/MapForums!default.jspa", true);
        } else if (config != null) {
            setJiraAdmin(config.getJiraAdmin());
            setUserVoiceAdmin(config.getUserVoiceAdmin());
            setUserVoiceUrl(config.getUserVoiceUrl());
            setConsumerKey(config.getConsumerKey());
            setSharedSecret(config.getSharedSecret());
        }
        /*
        * Bhushan: 09/25/2013
        * Setting allowUnassigned value
        */
        setAllowUnassigned();

        return INPUT;
    }

    @Override
    protected String doExecute() throws Exception {
        if (!permissionManager.hasPermission(Permissions.ADMINISTER, ComponentAccessor.getJiraAuthenticationContext().getUser())) {
            return "securitybreach";
        }

        PropertyUtil.saveUserVoiceConfig(jiraAdmin, consumerKey, sharedSecret, userVoiceAdmin, userVoiceUrl, ao);
        OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, sharedSecret);
        OAuthProvider provider = new DefaultOAuthProvider(userVoiceUrl + "/oauth/request_token", userVoiceUrl
                + "/oauth/access_token", userVoiceUrl + "/oauth/authorize");
        String uri = new String();
        try
        {
            uri = provider.retrieveRequestToken(consumer, this.applicationProperties.getString("jira.baseurl")
                    + "/secure/admin/MapForums!default.jspa");
        }
        catch(Exception e){
            addErrorMessage("Unable to authenticate with Uservoice with the provided keys. Please try again.");
            setAllowUnassigned();
            return ERROR;
        }
        this.request.getSession().setAttribute("consumer", consumer);
        this.request.getSession().setAttribute("provider", provider);
        return getRedirect(uri, true);
    }

    public Collection<ApplicationUser> getAdmins() {
        return this.userUtil.getJiraAdministrators();
    }

    public String getReconfigure() {
        return reconfigure;
    }

    public void setReconfigure(String reconfigure) {
        this.reconfigure = reconfigure;
    }
}