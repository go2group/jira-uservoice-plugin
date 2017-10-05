package com.go2group.entity;

import net.java.ao.Entity;

public interface MappingConfig extends Entity {

	public int getMappingId();

	public void setMappingId(int mappingId);

	public String getAllowedStatus();

	public void setAllowedStatus(String allowedStatus);

	public Boolean getSyncPriorEntities();

	public void setSyncPriorEntities(Boolean syncPriorEntities);

	public Boolean getSyncedOnce();

	public void setSyncedOnce(Boolean syncedOnce);

}
