package com.go2group.entity;

import java.util.ArrayList;
import java.util.List;

public class UVFilterVO {
	private List<UVFilterField> uvFilterFields=new ArrayList<UVFilterField>();
	private String mapId;
	private String filterOption;
	private String filter;

	public String getMapId() {
		return mapId;
	}

	public void setMapId(String mapId) {
		this.mapId = mapId;
	}

	public String getFilterOption() {
		return filterOption;
	}

	public void setFilterOption(String filterOption) {
		this.filterOption = filterOption;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public List<UVFilterField> getUvFilterFields() {
		return uvFilterFields;
	}

	public void setUvFilterFields(List<UVFilterField> uvFilterFields) {
		this.uvFilterFields = uvFilterFields;
	}
}
