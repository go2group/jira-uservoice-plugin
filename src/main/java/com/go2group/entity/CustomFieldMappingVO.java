package com.go2group.entity;

public class CustomFieldMappingVO implements Comparable<CustomFieldMappingVO> {

	private String mapId;
	
	private String jiraCustomFieldId;

	private String jiraCustomFieldName;

	private String uvCustomFieldId;

	private String uvCustomFieldName;

	public String getMapId() {
		return mapId;
	}

	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	public String getJiraCustomFieldId() {
		return jiraCustomFieldId;
	}

	public void setJiraCustomFieldId(String jiraCustomFieldId) {
		this.jiraCustomFieldId = jiraCustomFieldId;
	}

	public String getJiraCustomFieldName() {
		return jiraCustomFieldName;
	}

	public void setJiraCustomFieldName(String jiraCustomFieldName) {
		this.jiraCustomFieldName = jiraCustomFieldName;
	}

	public String getUvCustomFieldId() {
		return uvCustomFieldId;
	}

	public void setUvCustomFieldId(String uvCustomFieldId) {
		this.uvCustomFieldId = uvCustomFieldId;
	}

	public String getUvCustomFieldName() {
		return uvCustomFieldName;
	}

	public void setUvCustomFieldName(String uvCustomFieldName) {
		this.uvCustomFieldName = uvCustomFieldName;
	}

	@Override
	public int compareTo(CustomFieldMappingVO cfMappingVO) {
		if(cfMappingVO == null){
			return 1;
		}
		return uvCustomFieldName.compareTo(cfMappingVO.getUvCustomFieldName());
	}
	
	@Override
	public boolean equals(Object obj1) {
		if (obj1 == null) {
			return false;
		}
		CustomFieldMappingVO newObject = obj1 instanceof CustomFieldMappingVO ? (CustomFieldMappingVO) obj1
				: null;
		if (newObject == null) {
			return false;
		}
		boolean equality = false;
		if ((this.getUvCustomFieldName().equals(newObject.getUvCustomFieldName()))
				& this.getJiraCustomFieldName().equals(newObject.getJiraCustomFieldName())) {
			equality = true;
		}

		return equality;
	}

	@Override
	public int hashCode() {
		return ((int) (this.getUvCustomFieldName().hashCode() + this
				.getJiraCustomFieldName().hashCode()) / 10);
	}
}
