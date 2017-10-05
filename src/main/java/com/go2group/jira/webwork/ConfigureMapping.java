package com.go2group.jira.webwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import org.apache.commons.lang.StringUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.go2group.entity.FbStatusMap;
import com.go2group.entity.MappingConfig;
import com.go2group.entity.StatusMappingVO;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.uservoice.bean.FeedbackStatus;
import com.go2group.uservoice.manager.UserVoiceManager;
import com.go2group.uservoice.manager.impl.UserVoiceManagerImpl;
import com.go2group.util.PropertyUtil;

public class ConfigureMapping extends JiraWebActionSupport {

	private int id = -1;
	private String allowedStatus;
	private boolean success;
	private String syncPriorEntities;
	
	private List<FeedbackStatus> feedbackStatuses;
	private List<StatusMappingVO> statusMappingVOs;
	private Collection<Status> jiraStatuses;
	private String jsonFBSMapping;
	
	private final ActiveObjects ao;

	public ConfigureMapping(ActiveObjects ao) {
		this.ao = ao;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAllowedStatus() {
		return allowedStatus;
	}

	public void setAllowedStatus(String allowedStatus) {
		this.allowedStatus = allowedStatus;
	}

	@Override
	public String doDefault() throws Exception {
		success = false;
		if (id != -1) {
			MappingConfig mappingConfig = PropertyUtil.getMappingConfig(id, ao);
			if (mappingConfig != null) {
				setAllowedStatus(mappingConfig.getAllowedStatus());
				setSyncPriorEntities(mappingConfig.getSyncPriorEntities() ? "Yes" : null);
			}
			
			/* Changes for JUVP-28 - start */
			UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
			OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
			UserVoiceManager uvManager = new UserVoiceManagerImpl(ao);
			this.setFeedbackStatuses(uvManager.getFeedbackStatuses(config, consumer));
			
			FbStatusMap[] fbStatusMaps = PropertyUtil.getFbStatusMaps(String.valueOf(id), ao);
			
			if(fbStatusMaps != null){
				List<StatusMappingVO> feedbackStatusMappings=new ArrayList<StatusMappingVO>();
				for(FbStatusMap fbStatusMap: fbStatusMaps){
					StatusMappingVO fbStatusMapping=new StatusMappingVO();
					fbStatusMapping.setMapId(fbStatusMap.getMapId());
					
					fbStatusMapping.setUvStatusId(fbStatusMap.getUvStatusId());
					fbStatusMapping.setUserVoiceStatus(fbStatusMap.getUserVoiceStatus());
					fbStatusMapping.setJiraStatusId(fbStatusMap.getJiraStatusId());
					fbStatusMapping.setJiraStatus(fbStatusMap.getJiraStatus());
					
					feedbackStatusMappings.add(fbStatusMapping);
				}
				this.setStatusMappingVOs(feedbackStatusMappings);
			}
			/* save jira statuses */
			this.setJiraStatuses(ComponentAccessor.getConstantsManager().getStatusObjects());
			/* Changes for JUVP-28 - end */
		}
		return super.doDefault();
	}

	@Override
	protected String doExecute() throws Exception {
		boolean sync = "Yes".equals(syncPriorEntities);
		PropertyUtil.saveMappingConfig(id, allowedStatus, sync, ao);
		
		/* Changes for JUVP-28 - start */
		List<StatusMappingVO> feedbackStatusMappings=new ArrayList<StatusMappingVO>();
		StatusMappingVO fbStatusMapping=null;
		JSONArray mappings = new JSONArray(jsonFBSMapping);
		for (int i = 0; i < mappings.length(); i++) {
            JSONObject mapping = mappings.getJSONObject(i);
            if (mapping.has("id") && StringUtils.isNotBlank(mapping.getString("id"))) {
            	fbStatusMapping = new StatusMappingVO();
            	fbStatusMapping.setMapId(mapping.getString("id"));
            	
            	fbStatusMapping.setUvStatusId(mapping.getString("uvStatusId"));
            	fbStatusMapping.setUserVoiceStatus(mapping.getString("uvStatus"));
            	fbStatusMapping.setJiraStatusId(mapping.getString("jiraStatusId"));
            	fbStatusMapping.setJiraStatus(mapping.getString("jiraStatus"));

            	feedbackStatusMappings.add(fbStatusMapping);
            }
        }
		PropertyUtil.saveFeedbackStatusMappingConfig(String.valueOf(id), feedbackStatusMappings, ao);
		this.setStatusMappingVOs(feedbackStatusMappings);
		/* save jira statuses */
		this.setJiraStatuses(ComponentAccessor.getConstantsManager().getStatusObjects());
		/* Changes for JUVP-28 - end */
		
		success = true;
		return SUCCESS;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getSyncPriorEntities() {
		return syncPriorEntities;
	}

	public void setSyncPriorEntities(String syncPriorEntities) {
		this.syncPriorEntities = syncPriorEntities;
	}

	public List<FeedbackStatus> getFeedbackStatuses() {
		return feedbackStatuses;
	}

	public void setFeedbackStatuses(List<FeedbackStatus> feedbackStatuses) {
		this.feedbackStatuses = feedbackStatuses;
	}

	public String getJsonFBSMapping() {
		return jsonFBSMapping;
	}

	public void setJsonFBSMapping(String jsonFBSMapping) {
		this.jsonFBSMapping = jsonFBSMapping;
	}

	public Collection<Status> getJiraStatuses() {
		return jiraStatuses;
	}

	public void setJiraStatuses(Collection<Status> jiraStatuses) {
		this.jiraStatuses = jiraStatuses;
	}

	public List<StatusMappingVO> getStatusMappingVOs() {
		return statusMappingVOs;
	}

	public void setStatusMappingVOs(List<StatusMappingVO> fbStatusMappings) {
		this.statusMappingVOs = fbStatusMappings;
	}
}
