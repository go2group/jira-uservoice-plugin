package com.go2group.jira.webwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;

import org.apache.commons.lang.StringUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.go2group.entity.Mapping;
import com.go2group.entity.MappingConfig;
import com.go2group.entity.TickMappingConf;
import com.go2group.entity.TicketMapping;
import com.go2group.entity.TicketStatusMap;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.uservoice.bean.Forum;
import com.go2group.uservoice.manager.UserVoiceManager;
import com.go2group.util.PropertyUtil;

public class MapForums extends JiraWebActionSupport {

	private static final long serialVersionUID = 1L;

	private static final String UV_TYPE = "uvType";

    private static final String UV_FORUM = "uvForum";

    private static final String JIRA_ISSUE_TYPE = "jiraIssueType";

    private static final String JIRA_PROJECT = "jiraProject";

    private String jsonMapping;
    private String ticketJsonMapping;
    private boolean success;

    private final ActiveObjects ao;
    private final UserVoiceManager userVoiceManager;
    private final ProjectManager projectManager;
    private final ConstantsManager constantsManager;

    public MapForums(ActiveObjects ao, UserVoiceManager userVoiceManager, ProjectManager projectManager,
                     ConstantsManager constantsManager) {
        this.ao = ao;
        this.userVoiceManager = userVoiceManager;
        this.projectManager = projectManager;
        this.constantsManager = constantsManager;
    }

    @Override
    public String doDefault() throws Exception {
        success = false;
        UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
        if (config != null && (config.getOAuthToken() == null || config.getOAuthSecret() == null)) {
            OAuthConsumer consumer = (OAuthConsumer) this.request.getSession().getAttribute("consumer");
            OAuthProvider provider = (OAuthProvider) this.request.getSession().getAttribute("provider");
            String oauthVerifier = this.request.getParameter("oauth_verifier");
            try
            {
                provider.retrieveAccessToken(consumer, oauthVerifier);
            }
            catch(Exception e){
                addErrorMessage("Unable to contact Uservoice. Please try again.");
                return ERROR;
            }
            String token = consumer.getToken();
            String secret = consumer.getTokenSecret();

            this.request.getSession().removeAttribute("consumer");
            this.request.getSession().removeAttribute("provider");
            // Save Secrets
            PropertyUtil.saveSecret(token, secret, ao);
        }
        if(getProjects() == null || getProjects().size() == 0){
            addErrorMessage("No projects to configure. Create a JIRA project to get started.");
            return ERROR;
        }
        return INPUT;
    }

    @Override
    protected String doExecute() throws Exception {
        if (jsonMapping != null) {
            // We need to delete only the ones removed in UI so that primary key
            // is not changed
            Mapping[] currentMappings = PropertyUtil.getMappings(ao);
            List<Mapping> savedMappings = new ArrayList<Mapping>();
            JSONArray mappings = new JSONArray(jsonMapping);
            for (int i = 0; i < mappings.length(); i++) {
                JSONObject mapping = mappings.getJSONObject(i);
                if (!mapping.has("id") || StringUtils.isBlank(mapping.getString("id"))) {
                    Mapping savedMapping = PropertyUtil.saveMapping(mapping.getString(JIRA_PROJECT), mapping.getString(JIRA_ISSUE_TYPE),
                            mapping.getString(UV_FORUM), mapping.getString(UV_TYPE), -1, ao);
                    boolean sync = "true".equals(mapping.getString("syncPriorEntities"));
                    PropertyUtil.saveMappingConfig(savedMapping.getID(), mapping.getString("allowedStatus"), sync, ao);
                } else {
                    Mapping savedMapping = PropertyUtil.saveMapping(mapping.getString(JIRA_PROJECT),
                            mapping.getString(JIRA_ISSUE_TYPE), mapping.getString(UV_FORUM),
                            mapping.getString(UV_TYPE), mapping.getInt("id"), ao);
                    boolean sync = "true".equals(mapping.getString("syncPriorEntities"));
                    PropertyUtil.saveMappingConfig(savedMapping.getID(), mapping.getString("allowedStatus"), sync, ao);
                    savedMappings.add(savedMapping);
                }
            }
            // Find out mappings to be deleted. i.e. old mappings which are not
            // saved now
            List<Mapping> mappingToBeDeleted = new ArrayList<Mapping>();
            List<MappingConfig> mappingConfigToBeDeleted = new ArrayList<MappingConfig>();
            for (Mapping mapping : currentMappings) {
                if (!savedMappings.contains(mapping)) {
                    mappingToBeDeleted.add(mapping);
                    MappingConfig mappingConfig = PropertyUtil.getMappingConfig(mapping.getID(), ao);
                    if (mappingConfig != null){
                        mappingConfigToBeDeleted.add(mappingConfig);
                    }
                }
            }
            /* Changes for JUVP-22 - start */
            if(mappingToBeDeleted != null && mappingToBeDeleted.size() > 0){
            	for(Mapping map: mappingToBeDeleted){
            		PropertyUtil.deleteFbStatusMaps(String.valueOf(map.getID()), ao);
            		PropertyUtil.deleteFeedbackMappings(String.valueOf(map.getID()), ao);
            	}
            }
            /* Changes for JUVP-22 - end */
            Mapping[] tobeDeleted = new Mapping[mappingToBeDeleted.size()];
            ao.delete(mappingToBeDeleted.toArray(tobeDeleted));
            // Clear the relevant mapping config
            MappingConfig[] tobeDeletedConfig = new MappingConfig[mappingConfigToBeDeleted.size()];
            ao.delete(mappingConfigToBeDeleted.toArray(tobeDeletedConfig));
            success = true;
        }
        return SUCCESS;
    }

    public String doTicketMapping() throws Exception{
        success = false;
        if (ticketJsonMapping != null) {
            // We need to delete only the ones removed in UI so that primary key
            // is not changed
            TicketMapping[] currentMappings = PropertyUtil.getTicketMappings(ao);
            List<TicketMapping> savedMappings = new ArrayList<TicketMapping>();
            JSONArray mappings = new JSONArray(ticketJsonMapping);
            for (int i = 0; i < mappings.length(); i++) {
                JSONObject mapping = mappings.getJSONObject(i);
                if (!mapping.has("id") || StringUtils.isBlank(mapping.getString("id"))) {
                    TicketMapping savedMapping = PropertyUtil.saveTicketMapping(mapping.getString(JIRA_PROJECT), mapping.getString(JIRA_ISSUE_TYPE), -1, ao);
                    boolean sync = "true".equals(mapping.getString("syncPriorEntities"));
                    PropertyUtil.saveTicketMappingConfig(savedMapping.getID(), null, sync, ao);
                } else {
                    TicketMapping savedMapping = PropertyUtil.saveTicketMapping(mapping.getString(JIRA_PROJECT),
                            mapping.getString(JIRA_ISSUE_TYPE), mapping.getInt("id"), ao);
                    boolean sync = "true".equals(mapping.getString("syncPriorEntities"));
                    PropertyUtil.saveTicketMappingConfig(savedMapping.getID(), null, sync, ao);
                    savedMappings.add(savedMapping);
                }
            }
            // Find out mappings to be deleted. i.e. old mappings which are not
            // saved now
            List<TicketMapping> mappingToBeDeleted = new ArrayList<TicketMapping>();
            List<TickMappingConf> mappingConfigToBeDeleted = new ArrayList<TickMappingConf>();
            for (TicketMapping mapping : currentMappings) {
                if (!savedMappings.contains(mapping)) {
                    mappingToBeDeleted.add(mapping);
                    TickMappingConf mappingConfig = PropertyUtil.getTicketMappingConfig(mapping.getID(), ao);
                    if (mappingConfig != null){
                        mappingConfigToBeDeleted.add(mappingConfig);
                    }
                }
            }
            /* Changes for JUVP-22 - start */
            if(mappingToBeDeleted != null && mappingToBeDeleted.size() > 0){
            	for(TicketMapping map: mappingToBeDeleted){
            		PropertyUtil.deleteTicketStatusMaps(String.valueOf(map.getID()), ao);
            		PropertyUtil.deleteTktCFMaps(String.valueOf(map.getID()), ao);
            		PropertyUtil.deleteTicketMappings(String.valueOf(map.getID()), ao);
            	}
            }
            /* Changes for JUVP-22 - end */
            
            
            
            TicketMapping[] tobeDeleted = new TicketMapping[mappingToBeDeleted.size()];
            ao.delete(mappingToBeDeleted.toArray(tobeDeleted));
            // Clear the relevant mapping config
            TickMappingConf[] tobeDeletedConfig = new TickMappingConf[mappingConfigToBeDeleted.size()];
            ao.delete(mappingConfigToBeDeleted.toArray(tobeDeletedConfig));
            success = true;
        }
        return SUCCESS;
    }
    
    public List<Forum> getForums() {
        return this.userVoiceManager.getForums();
    }

    public List<Project> getProjects() {
        return this.projectManager.getProjectObjects();
    }

    public Mapping[] getMappings() {
        return PropertyUtil.getMappings(ao);
    }

    public TicketMapping[] getTicketMappings() {
        return PropertyUtil.getTicketMappings(ao);
    }

    public Collection<IssueType> getIssuetypes() {
        return this.constantsManager.getRegularIssueTypeObjects();
    }

    public String getJsonMapping() {
        return jsonMapping;
    }

    public void setJsonMapping(String jsonMapping) {
        this.jsonMapping = jsonMapping;
    }

    public String getTicketJsonMapping() {
        return ticketJsonMapping;
    }

    public void setTicketJsonMapping(String ticketJsonMapping) {
        this.ticketJsonMapping = ticketJsonMapping;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isConfigured(Mapping mapping){
        return PropertyUtil.getMappingConfig(mapping.getID(), ao) != null;
    }

    public boolean isTicketConfigured(TicketMapping mapping){
        return PropertyUtil.getTicketMappingConfig(mapping.getID(), ao) != null;
    }

    public String getAllowedStatuses(Mapping mapping){
        MappingConfig mappingConfig = PropertyUtil.getMappingConfig(mapping.getID(), ao);
        return mappingConfig.getAllowedStatus();
    }

    public boolean isSyncPriorEntities(Mapping mapping){
        MappingConfig mappingConfig = PropertyUtil.getMappingConfig(mapping.getID(), ao);
        return mappingConfig.getSyncPriorEntities();
    }

    public boolean isSyncPriorEntities(TicketMapping mapping){
        TickMappingConf mappingConfig = PropertyUtil.getTicketMappingConfig(mapping.getID(), ao);
        return mappingConfig.getSyncPriorEntities();
    }
    
    /* Changes for JUVP-28 - start */
    public TicketStatusMap[] getTicketStatusMappings() {
        return PropertyUtil.getTicketStatusMappings(ao);
    }
    /* Changes for JUVP-28 - end */
}
