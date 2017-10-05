package com.go2group.manager;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.go2group.uservoice.manager.UserVoiceManager;

public interface G2GManager {
	
	public UserVoiceManager getUserVoiceManager();
	
	public ActiveObjects getActiveObjects();

}
