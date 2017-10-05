package com.go2group.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.context.JiraContextNode;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.I18nHelper;
import com.go2group.entity.TktCustomFieldMap;
import com.go2group.entity.UVFilterField;

public class ApplicationUtil {
	
	private static OptionsManager optionsManager = null;
	public static OptionsManager getOptionsManager() {
        if (optionsManager == null) {
        	optionsManager = ComponentAccessor.getOptionsManager();
        }
        return optionsManager;
    }
	
	private static FieldConfig getFieldConfig(CustomField customField,
			Project project, IssueType issueType) {
		List<FieldConfigScheme> configSchemes = customField
				.getConfigurationSchemes();
		FieldConfig fieldConfig = null;
		for (FieldConfigScheme fieldConfigScheme : configSchemes) {
			// Check for projects
			List<JiraContextNode> contexts = fieldConfigScheme.getContexts();
			for (JiraContextNode jiraContextNode : contexts) {
				Project curProject = jiraContextNode.getProjectObject();
				if ((curProject == null || curProject.getId().equals(
						project.getId()))) {
					// Check for issue types
					Map<String, FieldConfig> fieldConfigs = fieldConfigScheme
							.getConfigs();
					if (fieldConfigs.keySet().contains(null)
							|| fieldConfigs.keySet()
									.contains(issueType.getId())) {

						fieldConfig = fieldConfigs.get(issueType.getId());
						if (fieldConfig == null)
							fieldConfig = fieldConfigs.get(null);
						if (fieldConfigScheme.isGlobal()) {
							// Let us seek project scheme first
							continue;
						} else {
							return fieldConfig;
						}
					}
				}
			}
		}

		return fieldConfig;
	}
	
	public static Options getOptions(
			CustomField customField, Project project, IssueType issueType) {
		FieldConfig fieldConfig = getFieldConfig(customField, project,
				issueType);
		return getOptionsManager().getOptions(fieldConfig);
	}
	
	public static String getUvCFName(TktCustomFieldMap[] tktCFMaps, String jiraCFName){
		for(TktCustomFieldMap map: tktCFMaps){
			if(jiraCFName.equals(map.getJiraCustomFieldName())){
				return map.getUvCustomFieldName();
			}
		}
		return null;
	}
	
	private static List<String> doNotListenList =new ArrayList<String>();
	public static boolean doNotListen(String issueId){
		return doNotListenList.contains(issueId);
	}
	
	public static void addToDoNotListen(String issueId){
		synchronized (doNotListenList) {
			doNotListenList.add(issueId);
		}
	}
	
	public static void removeDoNotListen(String issueId){
		synchronized (doNotListenList) {
			doNotListenList.remove(issueId);
		}
	}
	
	public static List<UVFilterField> getUVFilterList(ActiveObjects ao){
		List<UVFilterField> filterList=new ArrayList<UVFilterField>();
		ApplicationUser user = UserUtils.getUser(PropertyUtil.getUserVoiceConfig(ao).getJiraAdmin());
		I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(user);
        
        String[] uvTicketFilterFieldIds=helper.getText("uv.ticket.filter.field.ids").split(",");
        String[] uvTicketFilterFieldNames=helper.getText("uv.ticket.filter.field.names").split(",");
        
        if(uvTicketFilterFieldIds.length > 0 && uvTicketFilterFieldIds.length == uvTicketFilterFieldNames.length){
        	UVFilterField filterField=null;
        	for(int counter = 0; counter < uvTicketFilterFieldIds.length;counter++){
        		filterField = new UVFilterField();
        		filterField.setId(uvTicketFilterFieldIds[counter]);
        		filterField.setName(uvTicketFilterFieldNames[counter]);
        		filterList.add(filterField);
        	}
        }
        return filterList;
	}
}
