package com.go2group.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.java.ao.Query;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.go2group.entity.CommentMapping;
import com.go2group.entity.CustomFieldMappingVO;
import com.go2group.entity.TktCustomFieldMap;
import com.go2group.entity.FbStatusMap;
import com.go2group.entity.FeedbackMapping;
import com.go2group.entity.IssueTicket;
import com.go2group.entity.Mapping;
import com.go2group.entity.MappingConfig;
import com.go2group.entity.MessageMapping;
import com.go2group.entity.StatusMappingVO;
import com.go2group.entity.TickMappingConf;
import com.go2group.entity.TicketMapping;
import com.go2group.entity.TicketStatusMap;
import com.go2group.entity.UVTicketFilter;
import com.go2group.entity.UserVoiceConfig;

public class PropertyUtil {

	public static void saveUserVoiceConfig(String jAdmin, String cKey, String sSecret, String uvAdmin, String uvUrl,
			ActiveObjects ao) {
		UserVoiceConfig[] configs = ao.find(UserVoiceConfig.class);
		UserVoiceConfig newConfig;
		if (configs != null && configs.length == 1) {
			newConfig = configs[0];
		} else {
			// Add new
			newConfig = ao.create(UserVoiceConfig.class);
		}
		newConfig.setJiraAdmin(jAdmin);
		newConfig.setConsumerKey(cKey);
		newConfig.setSharedSecret(sSecret);
		newConfig.setUserVoiceAdmin(uvAdmin);
		newConfig.setUserVoiceUrl(uvUrl);
		newConfig.save();
	}

	public static void saveSecret(String token, String secret, ActiveObjects ao) {
		UserVoiceConfig[] configs = ao.find(UserVoiceConfig.class);
		if (configs != null && configs.length == 1) {
			configs[0].setOAuthToken(token);
			configs[0].setOAuthSecret(secret);
			configs[0].save();
		}
	}

	public static void saveLastRun(Date lastRun, ActiveObjects ao) {
		UserVoiceConfig[] configs = ao.find(UserVoiceConfig.class);
		if (configs != null && configs.length == 1) {
			configs[0].setLastRun(lastRun);
			configs[0].save();
		}
	}

	public static UserVoiceConfig getUserVoiceConfig(ActiveObjects ao) {
		UserVoiceConfig[] configs = ao.find(UserVoiceConfig.class);
		if (configs != null && configs.length == 1) {
			return configs[0];
		}
		return null;
	}

	public static Mapping saveMapping(String jiraProject, String jiraIssueType, String uvForum, String uvType, int id,
			ActiveObjects ao) {
		Mapping mapping;
		if (id != -1) {
			mapping = ao.get(Mapping.class, id);
		} else {
			mapping = ao.create(Mapping.class);
		}
		mapping.setJiraProject(jiraProject);
		mapping.setJiraIssueType(jiraIssueType);
		mapping.setUvForum(uvForum);
		mapping.setUvType(uvType);
		mapping.save();
		return mapping;
	}

    public static TicketMapping saveTicketMapping(String jiraProject, String jiraIssueType, int mappingId,
                                      ActiveObjects ao) {
        TicketMapping mapping;
        if (mappingId != -1) {
            mapping = ao.get(TicketMapping.class, mappingId);
        } else {
            mapping = ao.create(TicketMapping.class);
        }
        mapping.setJiraProject(jiraProject);
        mapping.setJiraIssueType(jiraIssueType);
        mapping.save();
        return mapping;
    }

	public static Mapping[] getMappings(ActiveObjects ao) {
		return ao.find(Mapping.class);
	}

    public static TicketMapping[] getTicketMappings(ActiveObjects ao) {
        return ao.find(TicketMapping.class);
    }

	public static void deleteMappings(ActiveObjects ao) {
		Mapping[] mappings = ao.find(Mapping.class);
		ao.delete(mappings);
	}

    public static void deleteTicketMappings(ActiveObjects ao) {
        TicketMapping[] mappings = ao.find(TicketMapping.class);
        ao.delete(mappings);
    }

	public static void saveFeedbackMapping(Long issue, String feedback, String forum, String mappingId, ActiveObjects ao) {
		FeedbackMapping mapping = ao.create(FeedbackMapping.class);
		mapping.setIssue(issue);
		mapping.setFeedback(feedback);
		mapping.setForum(forum);
		mapping.setMapping(mappingId);
		mapping.save();
	}

    public static void saveTicketMapping(Long jiraIssue, String uvTicket, String projectId, String mappingId, ActiveObjects ao) {
        IssueTicket mapping = ao.create(IssueTicket.class);
        mapping.setIssue(jiraIssue);
        mapping.setTicket(uvTicket);
        mapping.setProject(projectId);
        mapping.setMapping(mappingId);
        mapping.save();
    }

	public static FeedbackMapping getFeedbackMappingByIssue(Long issue, ActiveObjects ao) {
		FeedbackMapping[] feedbackMappings = ao.find(FeedbackMapping.class, Query.select().where("ISSUE = ?", issue));
		if (feedbackMappings.length > 0)
			return feedbackMappings[0];
		else
			return null;
	}

    public static IssueTicket getTicketMappingByIssue(Long issue, ActiveObjects ao) {
        IssueTicket[] mappings = ao.find(IssueTicket.class, Query.select().where("ISSUE = ?", issue));
        if (mappings.length > 0)
            return mappings[0];
        else
            return null;
    }

	public static FeedbackMapping getFeedbackMappingByFeedback(String feedback, ActiveObjects ao) {
		FeedbackMapping[] feedbackMappings = ao.find(FeedbackMapping.class,
				Query.select().where("FEEDBACK = ?", feedback));
		if (feedbackMappings.length > 0)
			return feedbackMappings[0];
		else
			return null;
	}

    public static IssueTicket getMessageMappingByTicket(String ticket, ActiveObjects ao) {
        IssueTicket[] messageMappings = ao.find(IssueTicket.class,
                Query.select().where("TICKET = ?", ticket));
        if (messageMappings.length > 0)
            return messageMappings[0];
        else
            return null;
    }

    //IssueTicket is the equivalent of FeedbackMapping
    //It stores the JIRA issue and the UserVoice Ticket mapping
    public static IssueTicket getTicketMappingByTicket(String ticket, ActiveObjects ao) {
        IssueTicket[] issueTickets = ao.find(IssueTicket.class,
                Query.select().where("TICKET = ?", ticket));
        if (issueTickets.length > 0)
            return issueTickets[0];
        else
            return null;
    }

    public static IssueTicket getTicketMappingByTicketAndProject(String ticket, String projectId, ActiveObjects ao) {
        IssueTicket[] issueTickets = ao.find(IssueTicket.class,
                Query.select().where("TICKET = ? AND PROJECT = ?", ticket, projectId));
        if (issueTickets.length > 0)
            return issueTickets[0];
        else
            return null;
    }

	public static void saveCommentMapping(Long issueCommentId, String feedbackCommentId, String feedback, String forum,
			ActiveObjects ao) {
		CommentMapping mapping = ao.create(CommentMapping.class);
		mapping.setIssueComment(issueCommentId);
		mapping.setFeedbackComment(feedbackCommentId);
		mapping.setFeedback(feedback);
		mapping.setForum(forum);
		mapping.save();
	}

    public static void saveMessageMapping(Long issueCommentId, String ticketMessageId, String ticket,
                                          ActiveObjects ao) {
        MessageMapping mapping = ao.create(MessageMapping.class);
        mapping.setIssueComment(issueCommentId);
        mapping.setTicketMessage(ticketMessageId);
        mapping.setTicket(ticket);
        mapping.save();
    }

	public static CommentMapping getCommentMappingByIssueComment(Long issueCommentId, ActiveObjects ao) {
		CommentMapping[] commentMappings = ao.find(CommentMapping.class,
				Query.select().where("ISSUE_COMMENT = ?", issueCommentId));
		if (commentMappings.length > 0)
			return commentMappings[0];
		else
			return null;
	}

	public static CommentMapping getCommentMappingByFeedbackComment(String feedbackCommentId, ActiveObjects ao) {
		CommentMapping[] commentMappings = ao.find(CommentMapping.class,
				Query.select().where("FEEDBACK_COMMENT = ?", feedbackCommentId));
		if (commentMappings.length > 0)
			return commentMappings[0];
		else
			return null;
	}

    /*
    public static MessageMapping[] getMessageMappingByTicketMessage(String ticketMessageId, ActiveObjects ao) {
        MessageMapping[] messageMappings = ao.find(MessageMapping.class,
                Query.select().where("TICKET_MESSAGE = ?", ticketMessageId));
        if (messageMappings.length > 0)
            return messageMappings;
        else
            return null;
    }
    */
    public static MessageMapping getMessageMappingByTicketMessageId(String ticketMessageId, ActiveObjects ao) {
        MessageMapping[] messageMappings = ao.find(MessageMapping.class,
                Query.select().where("TICKET_MESSAGE = ?", ticketMessageId));
        if (messageMappings.length > 0)
            return messageMappings[0];
        else
            return null;
    }

    public static CommentMapping[] getCommentMappingsByFeedback(String feedback, ActiveObjects ao){
        CommentMapping[] commentMappings = ao.find(CommentMapping.class,
                Query.select().where("FEEDBACK = ?", feedback));
        if (commentMappings.length > 0)
            return commentMappings;
        else
            return null;
    }

    public static MessageMapping[] getMessageMappingsByTicket(String ticketId, ActiveObjects ao){
        MessageMapping[] mappings = ao.find(MessageMapping.class,
                Query.select().where("TICKET = ?", ticketId));
        if (mappings.length > 0)
            return mappings;
        else
            return null;
    }

	public static MappingConfig getMappingConfig(int id, ActiveObjects ao) {
		MappingConfig[] mappingConfigs = ao.find(MappingConfig.class, Query.select().where("MAPPING_ID = ?", id));
		if (mappingConfigs.length > 0)
			return mappingConfigs[0];
		else
			return null;
	}

    public static TickMappingConf getTicketMappingConfig(int id, ActiveObjects ao) {
        TickMappingConf[] mappingConfigs = ao.find(TickMappingConf.class, Query.select().where("MAPPING_ID = ?", id));
        if (mappingConfigs.length > 0)
            return mappingConfigs[0];
        else
            return null;
    }

	public static void saveMappingConfig(int id, String allowedStatus, boolean syncPriorEntities, ActiveObjects ao) {
		MappingConfig mappingConfig = getMappingConfig(id, ao);
		if (mappingConfig == null) {
			mappingConfig = ao.create(MappingConfig.class);
			mappingConfig.setMappingId(id);
		}
		mappingConfig.setAllowedStatus(allowedStatus);
		mappingConfig.setSyncPriorEntities(syncPriorEntities);
		mappingConfig.save();
	}

    public static void saveTicketMappingConfig(int id, String allowedStatus, boolean syncPriorEntities, ActiveObjects ao) {
        TickMappingConf mappingConfig = getTicketMappingConfig(id, ao);
        if (mappingConfig == null) {
            mappingConfig = ao.create(TickMappingConf.class);
            mappingConfig.setMappingId(id);
        }
        mappingConfig.setAllowedStatus(allowedStatus);
        mappingConfig.setSyncPriorEntities(syncPriorEntities);
        mappingConfig.save();
    }
    
    /* Changes for JUVP-28 - start */
    public static void saveFeedbackStatusMappingConfig(String mappingId, List<StatusMappingVO> feedbackStatusMappings, ActiveObjects ao) {
    	if(StringUtils.isBlank(mappingId)){
    		return;
    	}
    	if(feedbackStatusMappings == null || feedbackStatusMappings.size() <= 0){
    		return;
    	}
    	
		StatusMappingVO[] newMaps=new StatusMappingVO[feedbackStatusMappings.size()];
		newMaps=feedbackStatusMappings.toArray(newMaps);
		Arrays.sort(newMaps);
		
		FbStatusMap[] fbStatusMaps = getFbStatusMaps(mappingId, ao);
		if(fbStatusMaps != null && fbStatusMaps.length > 0){ /* Entries exist */
			StatusMappingVO[] oldMaps=copyAndGetFbStatusMappingVO(fbStatusMaps);
			Arrays.sort(oldMaps);
			
			boolean equal = Arrays.equals(oldMaps, newMaps);
			if(!equal){
				deleteFbStatusMaps(mappingId, ao);/* delete old value mappings */
				for(StatusMappingVO fbStatusMapping: feedbackStatusMappings){  //add new value mappings 
					FbStatusMap fbStatusMap = ao.create(FbStatusMap.class);
					fbStatusMap.setMapId(mappingId);
					
					fbStatusMap.setUvStatusId(fbStatusMapping.getUvStatusId());
					fbStatusMap.setUserVoiceStatus(fbStatusMapping.getUserVoiceStatus());
					fbStatusMap.setJiraStatusId(fbStatusMapping.getJiraStatusId());
					fbStatusMap.setJiraStatus(fbStatusMapping.getJiraStatus());
					
					fbStatusMap.save();
				}
			}
		}else{
			for(StatusMappingVO fbStatusMapping: feedbackStatusMappings){
				FbStatusMap fbStatusMap = ao.create(FbStatusMap.class);
				fbStatusMap.setMapId(mappingId);
				
				fbStatusMap.setUvStatusId(fbStatusMapping.getUvStatusId());
				fbStatusMap.setUserVoiceStatus(fbStatusMapping.getUserVoiceStatus());
				fbStatusMap.setJiraStatusId(fbStatusMapping.getJiraStatusId());
				fbStatusMap.setJiraStatus(fbStatusMapping.getJiraStatus());
				
				
				fbStatusMap.save();
			}
		}
	}
    
    public static void saveTicketStatusMappingConfig(String mappingId, List<StatusMappingVO> statusMappings, ActiveObjects ao) {
    	if(StringUtils.isBlank(mappingId)){
    		return;
    	}
    	if(statusMappings == null || statusMappings.size() <= 0){
    		return;
    	}
    	
		StatusMappingVO[] newMaps=new StatusMappingVO[statusMappings.size()];
		newMaps=statusMappings.toArray(newMaps);
		Arrays.sort(newMaps);
		
		TicketStatusMap[] ticketStatusMaps = getTicketStatusMaps(mappingId, ao);
		if(ticketStatusMaps != null && ticketStatusMaps.length > 0){ /* Entries exist */
			StatusMappingVO[] oldMaps=copyAndGetFbStatusMappingVO(ticketStatusMaps);
			Arrays.sort(oldMaps);
			
			boolean equal = Arrays.equals(oldMaps, newMaps);
			if(!equal){
				deleteTicketStatusMaps(mappingId, ao);/* delete old value mappings */
				for(StatusMappingVO statusMapping: statusMappings){  //add new value mappings 
					TicketStatusMap ticketStatusMap = ao.create(TicketStatusMap.class);
					ticketStatusMap.setMapId(mappingId);
					
					ticketStatusMap.setUvStatusId(statusMapping.getUvStatusId());
					ticketStatusMap.setUserVoiceStatus(statusMapping.getUserVoiceStatus());
					ticketStatusMap.setJiraStatusId(statusMapping.getJiraStatusId());
					ticketStatusMap.setJiraStatus(statusMapping.getJiraStatus());
					
					ticketStatusMap.save();
				}
			}
		}else{
			for(StatusMappingVO statusMapping: statusMappings){  //add new value mappings 
				TicketStatusMap ticketStatusMap = ao.create(TicketStatusMap.class);
				ticketStatusMap.setMapId(mappingId);
				
				ticketStatusMap.setUvStatusId(statusMapping.getUvStatusId());
				ticketStatusMap.setUserVoiceStatus(statusMapping.getUserVoiceStatus());
				ticketStatusMap.setJiraStatusId(statusMapping.getJiraStatusId());
				ticketStatusMap.setJiraStatus(statusMapping.getJiraStatus());
				
				ticketStatusMap.save();
			}
		}
	}
    
    public static TicketStatusMap[] getTicketStatusMappings(ActiveObjects ao) {
		return ao.find(TicketStatusMap.class);
	}
    
    public static FbStatusMap[] getFbStatusMaps(String id, ActiveObjects ao) {
    	FbStatusMap[] fbStatusMaps = ao.find(FbStatusMap.class, Query.select().where("MAP_ID = ?", id));
        if (fbStatusMaps.length > 0)
            return fbStatusMaps;
        else
            return null;
    }
    
    public static TicketStatusMap[] getTicketStatusMaps(String id, ActiveObjects ao) {
    	TicketStatusMap[] ticketStatusMaps = ao.find(TicketStatusMap.class, Query.select().where("MAP_ID = ?", id));
        if (ticketStatusMaps.length > 0)
            return ticketStatusMaps;
        else
            return null;
    }
    
    public static int deleteFbStatusMaps(String id, ActiveObjects ao) {
    	int noOfRecords = 0;
    	FbStatusMap[] fbStatusMaps = ao.find(FbStatusMap.class, Query.select().where("MAP_ID = ?", id));
        if (fbStatusMaps.length > 0){
        	noOfRecords = fbStatusMaps.length;
        	ao.delete(fbStatusMaps);
        }
        return noOfRecords;
    }
    
    public static int deleteTicketStatusMaps(String id, ActiveObjects ao) {
    	int noOfRecords = 0;
    	TicketStatusMap[] ticketStatusMaps = ao.find(TicketStatusMap.class, Query.select().where("MAP_ID = ?", id));
        if (ticketStatusMaps.length > 0){
        	noOfRecords = ticketStatusMaps.length;
        	ao.delete(ticketStatusMaps);
        }
        return noOfRecords;
    }
    
    private static StatusMappingVO[] copyAndGetFbStatusMappingVO(FbStatusMap[] fbStatusMaps){
    	StatusMappingVO[] fbStatusMappings = new StatusMappingVO[fbStatusMaps.length];
		for(int counter=0; counter < fbStatusMaps.length;counter++){
			FbStatusMap map=fbStatusMaps[counter];
			StatusMappingVO vo=new StatusMappingVO();
			vo.setMapId(map.getMapId());
			vo.setUvStatusId(map.getUvStatusId());
			vo.setUserVoiceStatus(map.getUserVoiceStatus());
			vo.setJiraStatusId(map.getJiraStatusId());
			vo.setJiraStatus(map.getJiraStatus());
			
			fbStatusMappings[counter] = vo;
		}
		
		return fbStatusMappings;
    }
    
    private static StatusMappingVO[] copyAndGetFbStatusMappingVO(TicketStatusMap[] ticketStatusMaps){
    	StatusMappingVO[] statusMappings = new StatusMappingVO[ticketStatusMaps.length];
		for(int counter=0; counter < ticketStatusMaps.length;counter++){
			TicketStatusMap map=ticketStatusMaps[counter];
			StatusMappingVO vo=new StatusMappingVO();
			vo.setMapId(map.getMapId());
			vo.setUvStatusId(map.getUvStatusId());
			vo.setUserVoiceStatus(map.getUserVoiceStatus());
			vo.setJiraStatusId(map.getJiraStatusId());
			vo.setJiraStatus(map.getJiraStatus());
			
			statusMappings[counter] = vo;
		}
		
		return statusMappings;
    }
    
    public static FbStatusMap findFbStatusMapByJiraStatusName(String name, FbStatusMap[] fbStatusMaps){
    	FbStatusMap map=null;
    	if(StringUtils.isBlank(name)){
    		return null;
    	}
    	for(FbStatusMap tm: fbStatusMaps){
    		if(name.equals(tm.getJiraStatus())){
    			map = tm;
    			break;
    		}
    	}
    	
    	return map;
    }
    
    public static FbStatusMap findFbStatusMapByUVState(String name, FbStatusMap[] fbStatusMaps){
    	FbStatusMap map=null;
    	if(StringUtils.isBlank(name)){
    		return null;
    	}
    	for(FbStatusMap tm: fbStatusMaps){
    		if(name.equals(tm.getUserVoiceStatus())){
    			map = tm;
    			break;
    		}
    	}
    	
    	return map;
    }
    
    public static TicketStatusMap findTicketStatusMapByJiraStatusName(String name, TicketStatusMap[] ticketStatusMaps){
    	TicketStatusMap map=null;
    	if(StringUtils.isBlank(name)){
    		return null;
    	}
    	for(TicketStatusMap tm: ticketStatusMaps){
    		if(name.equalsIgnoreCase(tm.getJiraStatus())){
    			map = tm;
    			break;
    		}
    	}
    	
    	return map;
    }
    
    public static TicketStatusMap findTicketStatusMapByUVState(String name, TicketStatusMap[] ticketStatusMaps){
    	TicketStatusMap map=null;
    	if(StringUtils.isBlank(name)){
    		return null;
    	}
    	for(TicketStatusMap tm: ticketStatusMaps){
    		if(name.equalsIgnoreCase(tm.getUserVoiceStatus())){
    			map = tm;
    			break;
    		}
    	}
    	
    	return map;
    }
    
    public static TicketMapping getTicketMapping(String jiraProject, String jiraIssueType, ActiveObjects ao) {
    	TicketMapping mappingResult=null;
		TicketMapping[] mappings = ao.find(TicketMapping.class);
		
		if (mappings != null && mappings.length > 0) {
			for(TicketMapping mapping: mappings ){
				if(mapping.getJiraProject().trim().equals(jiraProject.trim()) && mapping.getJiraIssueType().equalsIgnoreCase(jiraIssueType)){
					mappingResult = mapping;
					break;
				}
			}
		}
		return mappingResult;
	}
    
    public static Mapping getMapping(String jiraProject, String jiraIssueType, ActiveObjects ao) {
    	Mapping mappingResult=null;
    	Mapping[] mappings = ao.find(Mapping.class);
		
		if (mappings != null && mappings.length > 0) {
			for(Mapping mapping: mappings ){
				if(mapping.getJiraProject().trim().equals(jiraProject.trim()) && mapping.getJiraIssueType().equalsIgnoreCase(jiraIssueType)){
					mappingResult = mapping;
					break;
				}
			}
		}
		
		return mappingResult;
	}
    /* Changes for JUVP-28 - end */
    
    /* Changes for JUVP-22 - start */
    public static void saveTicketCFMappingConfig(String mappingId, List<CustomFieldMappingVO> ticketCFMappings, ActiveObjects ao) {
    	if(StringUtils.isBlank(mappingId)){
    		return;
    	}
    	if(ticketCFMappings == null || ticketCFMappings.size() <= 0){
    		return;
    	}
    	
    	CustomFieldMappingVO[] newMaps=new CustomFieldMappingVO[ticketCFMappings.size()];
		newMaps=ticketCFMappings.toArray(newMaps);
		Arrays.sort(newMaps);
		
		TktCustomFieldMap[] fbCFMaps = getTktCFMaps(mappingId, ao);
		if(fbCFMaps != null && fbCFMaps.length > 0){ /* Entries exist */
			CustomFieldMappingVO[] oldMaps=copyAndGetTktCFMappingVO(fbCFMaps);
			Arrays.sort(oldMaps);
			
			boolean equal = Arrays.equals(oldMaps, newMaps);
			if(equal){
				return;
			}else{
				deleteTktCFMaps(mappingId, ao);/* delete old value mappings */
			}
		}
		for(CustomFieldMappingVO fbCFMapping: ticketCFMappings){  //add new value mappings 
			TktCustomFieldMap tktCFMap = ao.create(TktCustomFieldMap.class);
			tktCFMap.setMapId(mappingId);
			
			tktCFMap.setUvCustomFieldId(fbCFMapping.getUvCustomFieldId());
			tktCFMap.setUvCustomFieldName(fbCFMapping.getUvCustomFieldName());
			tktCFMap.setJiraCustomFieldId(fbCFMapping.getJiraCustomFieldId());
			tktCFMap.setJiraCustomFieldName(fbCFMapping.getJiraCustomFieldName());
			
			tktCFMap.save();
		}
	}
    
    public static TktCustomFieldMap[] getTktCFMaps(String id, ActiveObjects ao) {
    	TktCustomFieldMap[] tktCFMaps = ao.find(TktCustomFieldMap.class, Query.select().where("MAP_ID = ?", id));
        if (tktCFMaps.length > 0)
            return tktCFMaps;
        else
            return null;
    }
    private static CustomFieldMappingVO[] copyAndGetTktCFMappingVO(TktCustomFieldMap[] cfMaps){
    	CustomFieldMappingVO[] tktCFMappings = new CustomFieldMappingVO[cfMaps.length];
		for(int counter=0; counter < cfMaps.length;counter++){
			TktCustomFieldMap map=cfMaps[counter];
			CustomFieldMappingVO vo=new CustomFieldMappingVO();
			vo.setMapId(map.getMapId());
			vo.setUvCustomFieldId(map.getUvCustomFieldId());
			vo.setUvCustomFieldName(map.getUvCustomFieldName());
			vo.setJiraCustomFieldId(map.getJiraCustomFieldId());
			vo.setJiraCustomFieldName(map.getJiraCustomFieldName());
			
			tktCFMappings[counter] = vo;
		}
		
		return tktCFMappings;
    }
    
    public static int deleteTktCFMaps(String id, ActiveObjects ao) {
    	int noOfRecords = 0;
    	TktCustomFieldMap[] tktCFMaps = ao.find(TktCustomFieldMap.class, Query.select().where("MAP_ID = ?", id));
        if (tktCFMaps.length > 0){
        	noOfRecords = tktCFMaps.length;
        	ao.delete(tktCFMaps);
        }
        return noOfRecords;
    }
    /* Changes for JUVP-22 - end */
    
    public static int deleteTicketMappings(String mappingId,  ActiveObjects ao) {
    	int noOfRecords = 0;
        IssueTicket[] issueTickets = ao.find(IssueTicket.class,
                Query.select().where("MAPPING = ?", mappingId));
        if (issueTickets.length > 0){
        	noOfRecords = issueTickets.length;
            ao.delete(issueTickets);
        }
        return noOfRecords;
    }
    
    public static int deleteFeedbackMappings(String mappingId,  ActiveObjects ao) {
    	int noOfRecords = 0;
    	FeedbackMapping[] FeedbackMappings = ao.find(FeedbackMapping.class,
                Query.select().where("MAPPING = ?", mappingId));
        if (FeedbackMappings.length > 0){
        	noOfRecords = FeedbackMappings.length;
            ao.delete(FeedbackMappings);
        }
        return noOfRecords;
    }
    
    public static void saveFilter(String mappingId, String filterOption, String filter,  ActiveObjects ao){
    	if(StringUtils.isBlank(mappingId)){
    		return;
    	}
    	if(StringUtils.isBlank(filter)){
    		return;
    	}
    	UVTicketFilter[] filters = ao.find(UVTicketFilter.class,
                Query.select().where("MAP_ID = ?", mappingId));
    	if (filters.length > 0){
    		if("true".equalsIgnoreCase(filterOption)){
    			UVTicketFilter uvFilter=filters[0];
    			uvFilter.setFilterOption("true");
    			uvFilter.setFilter(filter);
    			uvFilter.save();
        	}else{
        		UVTicketFilter uvFilter=filters[0];
    			uvFilter.setFilterOption("false");
    			uvFilter.save();
        	}
        }else{
        	UVTicketFilter uvFilter=ao.create(UVTicketFilter.class);
        	uvFilter.setMapId(mappingId);
        	uvFilter.setFilterOption("true");
			uvFilter.setFilter(filter);
			uvFilter.save();
        }
    }
    
    public static UVTicketFilter getFilter(String mappingId, ActiveObjects ao){
    	UVTicketFilter filter=null;
    	if(StringUtils.isNotBlank(mappingId)){
    		UVTicketFilter[] filters = ao.find(UVTicketFilter.class,
                    Query.select().where("MAP_ID = ?", mappingId));
    		if (filters.length > 0){
    			filter = filters[0];
    		}
    	}
    	return filter;
    }
    
    public static int deleteFilter(String mappingId, ActiveObjects ao){
    	int noOfRecords = 0;
    	UVTicketFilter[] filters = ao.find(UVTicketFilter.class,
                Query.select().where("MAP_ID = ?", mappingId));
        if (filters.length > 0){
        	noOfRecords = filters.length;
            ao.delete(filters);
        }
        return noOfRecords;
    }
}
