package com.go2group.entity;

import net.java.ao.Entity;

public interface UVTicketFilter extends Entity {

	public String getMapId();
	
	public void setMapId(String mapId);
	
	public String getFilterOption();
	
	public void setFilterOption(String filterOption);
	
	public String getFilter();
	
	public void setFilter(String filter);
}
