package com.go2group.entity;

import net.java.ao.Entity;

public interface  TktCustomFieldMap extends Entity {

	public String getMapId();
	
	public String getJiraCustomFieldId();

	public String getJiraCustomFieldName();

	public String getUvCustomFieldId();

	public String getUvCustomFieldName();

	public void setJiraCustomFieldId(String jiraCustomFieldId);

	public void setJiraCustomFieldName(String jiraCustomFieldName);

	public void setMapId(String mapId);

	public void setUvCustomFieldId(String uvCustomFieldId);

	public void setUvCustomFieldName(String uvCustomFieldName);
}
