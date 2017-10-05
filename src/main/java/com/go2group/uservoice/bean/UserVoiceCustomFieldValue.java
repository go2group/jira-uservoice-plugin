package com.go2group.uservoice.bean;

public class UserVoiceCustomFieldValue {

	private String customFieldKey;
	private String id;
	private String value;
	private boolean active;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getCustomFieldKey() {
		return customFieldKey;
	}

	public void setCustomFieldKey(String customFieldKey) {
		this.customFieldKey = customFieldKey;
	}


}
