package com.go2group.uservoice.bean;

import java.util.List;

public class UserVoiceCustomField {

	private String id;
	private String name;
	private String description;
	private boolean active;
	private boolean privatelyVisible;
	private boolean predefined;
	private boolean readOnly;
	private boolean allowBlank;
	private List<UserVoiceCustomFieldValue> possibleValues;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isPredefined() {
		return predefined;
	}

	public void setPredefined(boolean predefined) {
		this.predefined = predefined;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isAllowBlank() {
		return allowBlank;
	}

	public void setAllowBlank(boolean allowBlank) {
		this.allowBlank = allowBlank;
	}

	public List<UserVoiceCustomFieldValue> getPossibleValues() {
		return possibleValues;
	}

	public void setPossibleValues(List<UserVoiceCustomFieldValue> possibleValues) {
		this.possibleValues = possibleValues;
	}

	public boolean isPrivatelyVisible() {
		return privatelyVisible;
	}

	public void setPrivatelyVisible(boolean privatelyVisible) {
		this.privatelyVisible = privatelyVisible;
	}
}
