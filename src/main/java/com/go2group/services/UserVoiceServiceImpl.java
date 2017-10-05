package com.go2group.services;

import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;

/**
 * User: Bhushan Nagaraj
 * Date: 25/9/13
 * Time: 3:21 PM
 */
public class UserVoiceServiceImpl implements UserVoiceServiceMonitor, LifecycleAware, DisposableBean {
    static final String KEY = UserVoiceServiceImpl.class.getName() + ":instance";
    private static final String JOB_NAME = UserVoiceServiceImpl.class.getName() + ":job";

    private final Logger logger = Logger.getLogger(UserVoiceServiceImpl.class);
    private final PluginScheduler pluginScheduler;

    private long interval = 60000L;      // default job interval (1 minute)

    public UserVoiceServiceImpl(PluginScheduler pluginScheduler) {
        this.pluginScheduler = pluginScheduler;
    }

    // declared by LifecycleAware
    public void onStart() {
        reschedule(interval);
    }

    public void reschedule( long interval) {
        this.interval = interval;

        pluginScheduler.scheduleJob(
                JOB_NAME,                   // unique name of the job
                UserVoiceService.class,     // class of the job
                new HashMap<String,Object>() {{
                    put(KEY, UserVoiceServiceImpl.this);
                }},                         // data that needs to be passed to the job
                new Date(),                 // the time the job is to start
                interval);                  // interval between repeats, in milliseconds
        logger.info(String.format("UserVoice service task scheduled to run every %dms", interval));
    }

    @Override
    public void destroy() throws Exception{
    	try{
    		pluginScheduler.unscheduleJob(JOB_NAME);
    	}catch(IllegalArgumentException iax){
    		logger.error(iax.getMessage(), iax);
    	}
        ComponentAccessor.getServiceManager().removeServiceByName(JOB_NAME);
    }
}
