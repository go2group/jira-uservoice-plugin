package com.go2group.jira.webwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import org.apache.commons.lang.StringUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.go2group.entity.CustomFieldMappingVO;
import com.go2group.entity.StatusMappingVO;
import com.go2group.entity.TickMappingConf;
import com.go2group.entity.TicketStatusMap;
import com.go2group.entity.TktCustomFieldMap;
import com.go2group.entity.UVFilterField;
import com.go2group.entity.UVFilterVO;
import com.go2group.entity.UVTicketFilter;
import com.go2group.entity.UserVoiceConfig;
import com.go2group.uservoice.bean.TicketStatus;
import com.go2group.uservoice.bean.UserVoiceCustomField;
import com.go2group.uservoice.manager.UserVoiceManager;
import com.go2group.uservoice.manager.impl.UserVoiceManagerImpl;
import com.go2group.util.ApplicationUtil;
import com.go2group.util.PropertyUtil;

public class ConfigureTicketMapping extends JiraWebActionSupport
{
	private static final long serialVersionUID = 1L;
	private int id = -1;
    private String allowedStatus;
    private boolean success;
    private String syncPriorEntities;

    private final ActiveObjects ao;
    
    private List<TicketStatus> ticketStatuses;
	private List<StatusMappingVO> statusMappingVOs;
	private Collection<Status> jiraStatuses;
	private String jsonTicketStatusMapping;
	
	/* Changes for JUVP-22 - start */
	private List<UserVoiceCustomField> uvCustomFields;
	private String jsonTktCFMapping;
	private List<CustomFieldMappingVO> uvCFMappingVOs;
	private List<CustomField> jiraCustomFields;
	/* Changes for JUVP-22 - end */
	private String jsonTktFilter;
	private String filterOption;
	private UVFilterVO uvFilterVO;

    public ConfigureTicketMapping(ActiveObjects ao) {
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
            TickMappingConf mappingConfig = PropertyUtil.getTicketMappingConfig(id, ao);
            if (mappingConfig != null) {
                setAllowedStatus(mappingConfig.getAllowedStatus());
                setSyncPriorEntities(mappingConfig.getSyncPriorEntities() ? "Yes" : null);
            }
            
            ApplicationUser user = UserUtils.getUser(PropertyUtil.getUserVoiceConfig(ao).getJiraAdmin());
            I18nHelper helper = ComponentAccessor.getI18nHelperFactory().getInstance(user);
            
            String[] ticketStatusIds=helper.getText("uv.ticket.status.ids").split(",");
            String[] ticketStatusNames = helper.getText("uv.ticket.statuses").split(",");
            List<TicketStatus> ticketStatuses=new ArrayList<TicketStatus>();
            for(int counter=0; counter < ticketStatusIds.length; counter++){
            	TicketStatus ts=new TicketStatus();
            	ts.setId(ticketStatusIds[counter]);
            	ts.setName(ticketStatusNames[counter]);
//            	ts.setEvent(ticketStatusIds[counter]);
            	
            	ticketStatuses.add(ts);
            }
            
            this.setTicketStatuses(ticketStatuses);
            
			TicketStatusMap[] ticketStatusMaps = PropertyUtil.getTicketStatusMaps(String.valueOf(id), ao);
			List<StatusMappingVO> statusMappings=new ArrayList<StatusMappingVO>();
			if(ticketStatusMaps != null){
				for(TicketStatusMap tktStatusMap: ticketStatusMaps){
					StatusMappingVO statusMapping=new StatusMappingVO();
					statusMapping.setMapId(tktStatusMap.getMapId());
					
					statusMapping.setUvStatusId(tktStatusMap.getUvStatusId());
					statusMapping.setUserVoiceStatus(tktStatusMap.getUserVoiceStatus());
					statusMapping.setJiraStatusId(tktStatusMap.getJiraStatusId());
					statusMapping.setJiraStatus(tktStatusMap.getJiraStatus());
					
					statusMappings.add(statusMapping);
				}
				this.setStatusMappingVOs(statusMappings);
			}
			
			/* save jira statuses */
			this.setJiraStatuses(ComponentAccessor.getConstantsManager().getStatusObjects());
			/* Changes for JUVP-28 - end */
			
			/* Changes for JUVP-22 - start */
			UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
			OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
			UserVoiceManager uvManager = new UserVoiceManagerImpl(ao);
			
			this.setUvCustomFields(uvManager.getUVCustomFields(config, consumer));
			
			TktCustomFieldMap[] tktCFMaps = PropertyUtil.getTktCFMaps(String.valueOf(id), ao);
			List<CustomFieldMappingVO> tktCFMappings=new ArrayList<CustomFieldMappingVO>();
			if(tktCFMaps != null){
				
				for(TktCustomFieldMap tktCFMap: tktCFMaps){
					CustomFieldMappingVO tktCFMapping=new CustomFieldMappingVO();
					tktCFMapping.setMapId(tktCFMap.getMapId());
					
					tktCFMapping.setUvCustomFieldId(tktCFMap.getUvCustomFieldId());
					tktCFMapping.setUvCustomFieldName(tktCFMap.getUvCustomFieldName());
					tktCFMapping.setJiraCustomFieldId(tktCFMap.getJiraCustomFieldId());
					tktCFMapping.setJiraCustomFieldName(tktCFMap.getJiraCustomFieldName());
					
					tktCFMappings.add(tktCFMapping);
				}
				this.setUvCFMappingVOs(tktCFMappings);
			}
			this.setJiraCustomFields(ComponentAccessor.getCustomFieldManager().getCustomFieldObjects());
			/* Changes for JUVP-22 - end */
			/* UV Ticket Filter - start */
			UVFilterVO uvFilterVO = new UVFilterVO();
			List<UVFilterField> filterFields=ApplicationUtil.getUVFilterList(ao);
			if(tktCFMappings.size() > 0){
				for(CustomFieldMappingVO cf: tktCFMappings){
					UVFilterField filterField=new UVFilterField();
					filterField.setId(cf.getUvCustomFieldName());
					filterField.setName(cf.getUvCustomFieldName());
					filterFields.add(filterField);
				}
			}
			uvFilterVO.setUvFilterFields(filterFields);
			
			UVTicketFilter filter = PropertyUtil.getFilter(String.valueOf(id), ao);
			if(filter != null){
				String filterText=filter.getFilter();
				filterText = filterText.replaceAll("_", ";\r\n");
				uvFilterVO.setFilter(filterText);
				uvFilterVO.setFilterOption(filter.getFilterOption());
				uvFilterVO.setMapId(filter.getMapId());
			}
			this.setUvFilterVO(uvFilterVO);
			/* UV Ticket Filter - end */
        }
        return super.doDefault();
    }

    @Override
    protected String doExecute() throws Exception {
        boolean sync = "Yes".equals(syncPriorEntities);
        PropertyUtil.saveTicketMappingConfig(id, allowedStatus, sync, ao);
        
        /* Changes for JUVP-28 - start */
		List<StatusMappingVO> statusMappings=new ArrayList<StatusMappingVO>();
		StatusMappingVO statusMapping=null;
		JSONArray mappings = new JSONArray(jsonTicketStatusMapping);
		for (int i = 0; i < mappings.length(); i++) {
            JSONObject mapping = mappings.getJSONObject(i);
            if (mapping.has("id") && StringUtils.isNotBlank(mapping.getString("id"))) {
            	statusMapping = new StatusMappingVO();
            	statusMapping.setMapId(mapping.getString("id"));
            	
            	statusMapping.setUvStatusId(mapping.getString("uvStatusId"));
            	statusMapping.setUserVoiceStatus(mapping.getString("uvStatus"));
            	statusMapping.setJiraStatusId(mapping.getString("jiraStatusId"));
            	statusMapping.setJiraStatus(mapping.getString("jiraStatus"));

            	statusMappings.add(statusMapping);
            }
        }
		PropertyUtil.saveTicketStatusMappingConfig(String.valueOf(id), statusMappings, ao);
		this.setStatusMappingVOs(statusMappings);
		/* save jira statuses */
		this.setJiraStatuses(ComponentAccessor.getConstantsManager().getStatusObjects());
		/* Changes for JUVP-28 - end */
		
		/* Changes for JUVP-22 - start */
		List<CustomFieldMappingVO> tktCFMappings=new ArrayList<CustomFieldMappingVO>();
		CustomFieldMappingVO tktCFMappingVO=null;
		JSONArray cfMappings = new JSONArray(jsonTktCFMapping);
		for(int i = 0; i < cfMappings.length(); i++){
			JSONObject mapping = cfMappings.getJSONObject(i);
			if (mapping.has("id") && StringUtils.isNotBlank(mapping.getString("id"))) {
				tktCFMappingVO = new CustomFieldMappingVO();
				
				tktCFMappingVO.setMapId(mapping.getString("id"));
				tktCFMappingVO.setUvCustomFieldId(mapping.getString("uvCFId"));
				tktCFMappingVO.setUvCustomFieldName(mapping.getString("uvCFName"));
				tktCFMappingVO.setJiraCustomFieldId(mapping.getString("jiraCFId"));
				tktCFMappingVO.setJiraCustomFieldName(mapping.getString("jiraCFName"));
				
				tktCFMappings.add(tktCFMappingVO);
			}
		}
		PropertyUtil.saveTicketCFMappingConfig(String.valueOf(id), tktCFMappings, ao);
		this.setUvCFMappingVOs(tktCFMappings);
		
		UserVoiceConfig config = PropertyUtil.getUserVoiceConfig(ao);
		UserVoiceManager uvManager = new UserVoiceManagerImpl(ao);
		OAuthConsumer consumer = new DefaultOAuthConsumer(config.getConsumerKey(), config.getSharedSecret());
		
		this.setUvCustomFields(uvManager.getUVCustomFields(config, consumer));
		this.setJiraCustomFields(ComponentAccessor.getCustomFieldManager().getCustomFieldObjects());
		/* Changes for JUVP-22 - end */
		
		/* UV Ticket Filter - start */
		if(StringUtils.isNotBlank(filterOption)){
			if(StringUtils.isNotBlank(jsonTktFilter)){
				jsonTktFilter = jsonTktFilter.replaceAll("\r", "");
				jsonTktFilter = jsonTktFilter.replaceAll("\n", "");
				jsonTktFilter = jsonTktFilter.replaceAll(";", "_");
				PropertyUtil.saveFilter(String.valueOf(id), filterOption, jsonTktFilter, ao);
			}
		}
		
		UVFilterVO uvFilterVO = new UVFilterVO();
		List<UVFilterField> filterFields=ApplicationUtil.getUVFilterList(ao);
		if(tktCFMappings.size() > 0){
			for(CustomFieldMappingVO cf: tktCFMappings){
				UVFilterField filterField=new UVFilterField();
				filterField.setId(cf.getUvCustomFieldName());
				filterField.setName(cf.getUvCustomFieldName());
				filterFields.add(filterField);
			}
		}
		uvFilterVO.setUvFilterFields(filterFields);
		
		UVTicketFilter filter = PropertyUtil.getFilter(String.valueOf(id), ao);
		if(filter != null){
			String filterText=filter.getFilter();
			filterText = filterText.replaceAll("_", ";\r\n");
			uvFilterVO.setFilter(filterText);
			uvFilterVO.setFilterOption(filter.getFilterOption());
			uvFilterVO.setMapId(filter.getMapId());
		}
		this.setUvFilterVO(uvFilterVO);
		/* UV Ticket Filter - end */
		
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

	public List<TicketStatus> getTicketStatuses() {
		return ticketStatuses;
	}

	public void setTicketStatuses(List<TicketStatus> ticketStatuses) {
		this.ticketStatuses = ticketStatuses;
	}

	public List<StatusMappingVO> getStatusMappingVOs() {
		return statusMappingVOs;
	}

	public void setStatusMappingVOs(List<StatusMappingVO> statusMappingVOs) {
		this.statusMappingVOs = statusMappingVOs;
	}

	public Collection<Status> getJiraStatuses() {
		return jiraStatuses;
	}

	public void setJiraStatuses(Collection<Status> jiraStatuses) {
		this.jiraStatuses = jiraStatuses;
	}

	public String getJsonTicketStatusMapping() {
		return jsonTicketStatusMapping;
	}

	public void setJsonTicketStatusMapping(String jsonTicketStatusMapping) {
		this.jsonTicketStatusMapping = jsonTicketStatusMapping;
	}

	public List<UserVoiceCustomField> getUvCustomFields() {
		return uvCustomFields;
	}

	public void setUvCustomFields(List<UserVoiceCustomField> uvCustomFields) {
		this.uvCustomFields = uvCustomFields;
	}

	public String getJsonTktCFMapping() {
		return jsonTktCFMapping;
	}

	public void setJsonTktCFMapping(String jsonTktCFMapping) {
		this.jsonTktCFMapping = jsonTktCFMapping;
	}

	public List<CustomFieldMappingVO> getUvCFMappingVOs() {
		return uvCFMappingVOs;
	}

	public void setUvCFMappingVOs(List<CustomFieldMappingVO> uvCFMappingVOs) {
		this.uvCFMappingVOs = uvCFMappingVOs;
	}

	public List<CustomField> getJiraCustomFields() {
		return jiraCustomFields;
	}

	public void setJiraCustomFields(List<CustomField> jiraCustomFields) {
		this.jiraCustomFields = jiraCustomFields;
	}

	public String getJsonTktFilter() {
		return jsonTktFilter;
	}

	public void setJsonTktFilter(String jsonTktFilter) {
		this.jsonTktFilter = jsonTktFilter;
	}

	public String getFilterOption() {
		return filterOption;
	}

	public void setFilterOption(String filterOption) {
		this.filterOption = filterOption;
	}

	public UVFilterVO getUvFilterVO() {
		return uvFilterVO;
	}

	public void setUvFilterVO(UVFilterVO uvFilterVO) {
		this.uvFilterVO = uvFilterVO;
	}
}
