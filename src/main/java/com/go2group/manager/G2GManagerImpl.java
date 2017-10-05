package com.go2group.manager;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.go2group.uservoice.manager.UserVoiceManager;

public class G2GManagerImpl implements G2GManager{
	
	private final ActiveObjects ao;
	private final UserVoiceManager userVoiceManager;

	public G2GManagerImpl(ActiveObjects ao, UserVoiceManager userVoiceManager) {
		this.ao = ao;
		this.userVoiceManager = userVoiceManager;
	}

	@Override
	public UserVoiceManager getUserVoiceManager() {
		return this.userVoiceManager;
	}

	@Override
	public ActiveObjects getActiveObjects() {
		return this.ao;
	}

}
