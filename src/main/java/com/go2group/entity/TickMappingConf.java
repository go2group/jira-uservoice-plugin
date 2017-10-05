package com.go2group.entity;

import net.java.ao.Entity;

/**
 * Created with IntelliJ IDEA.
 * User: bhushan
 * Date: 30/10/13
 * Time: 3:14 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TickMappingConf extends Entity {

    //TicketMappingConfiguration name too big for AO

    public int getMappingId();

    public void setMappingId(int mappingId);

    public String getAllowedStatus();

    public void setAllowedStatus(String allowedStatus);

    public Boolean getSyncPriorEntities();

    public void setSyncPriorEntities(Boolean syncPriorEntities);

    public Boolean getSyncedOnce();

    public void setSyncedOnce(Boolean syncedOnce);
}
