package com.go2group.renderer;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.plugin.issuelink.AbstractIssueLinkRenderer;
import com.atlassian.jira.util.I18nHelper;
import com.go2group.entity.FeedbackMapping;
import com.go2group.uservoice.bean.Feedback;
import com.go2group.uservoice.manager.UserVoiceManager;
import com.go2group.util.PropertyUtil;
import com.google.common.collect.ImmutableMap;

public class FeedbackRenderer extends AbstractIssueLinkRenderer {

	private final UserVoiceManager userVoiceManager;
	private final ActiveObjects ao;
	public static final String DEFAULT_ICON_URL = "/download/resources/com.go2group.uservoice-plugin:uv-resources/images/Uservoice.png";

	public FeedbackRenderer(UserVoiceManager userVoiceManager, ActiveObjects ao) {
		this.userVoiceManager = userVoiceManager;
		this.ao = ao;
	}

	@Override
	public Map<String, Object> getInitialContext(RemoteIssueLink remoteIssueLink, Map<String, Object> context) {
		final I18nHelper i18n = getValue(context, "i18n", I18nHelper.class);
		final String baseUrl = getValue(context, "baseurl", String.class);
		return createContext(remoteIssueLink, i18n, baseUrl);
	}

	@Override
	public Map<String, Object> getFinalContext(RemoteIssueLink remoteIssueLink, Map<String, Object> context) {
		String feedbackId = remoteIssueLink.getGlobalId();
		FeedbackMapping mapping = PropertyUtil.getFeedbackMappingByFeedback(feedbackId, ao);
        ImmutableMap.Builder<String, Object> contextBuilder = ImmutableMap.builder();
        contextBuilder.putAll(getInitialContext(remoteIssueLink, context));
        try
        {
		    Feedback feedback = this.userVoiceManager.getFeedback(mapping.getForum(), feedbackId);
            if (feedback != null) {
                putMap(contextBuilder, "votes", Integer.valueOf(feedback.getVotes()));
                putMap(contextBuilder, "state", feedback.getState());
                putMap(contextBuilder, "status", feedback.getStatus());
                putMap(contextBuilder, "newtitle", feedback.getTitle().length() > 50 ? feedback.getTitle().substring(0, 50)
                        + "..." : feedback.getTitle());
                putMap(contextBuilder, "newtooltip", getTooltip(remoteIssueLink.getApplicationName(), remoteIssueLink.getSummary(), feedback.getTitle()));
            }
        }
        catch(Exception exc){
            //Not a feedback item so do nothing
        }
		return contextBuilder.build();
	}

	private <T> T getValue(Map<String, Object> context, String key, Class<T> klass) {
		Object obj = context.get(key);
		if (obj == null) {
			throw new IllegalArgumentException(String.format("Expected '%s' to exist in the context map", key));
		}
		return klass.cast(obj);
	}

	private static Map<String, Object> createContext(RemoteIssueLink remoteIssueLink, I18nHelper i18n, String baseUrl) {
		ImmutableMap.Builder<String, Object> contextBuilder = ImmutableMap.builder();

		String tooltip = getTooltip(remoteIssueLink.getApplicationName(), remoteIssueLink.getSummary(),
				remoteIssueLink.getTitle());
		final String iconUrl = StringUtils.defaultIfEmpty(remoteIssueLink.getIconUrl(), baseUrl + DEFAULT_ICON_URL);
		final String iconTooltip = getIconTooltip(remoteIssueLink, i18n);

		putMap(contextBuilder, "id", remoteIssueLink.getId());
		putMap(contextBuilder, "url", remoteIssueLink.getUrl());
		putMap(contextBuilder, "title", remoteIssueLink.getTitle().length() > 50 ? remoteIssueLink.getTitle()
				.substring(0, 50) + "..." : remoteIssueLink.getTitle());
		putMap(contextBuilder, "iconUrl", iconUrl);
		putMap(contextBuilder, "iconTooltip", iconTooltip);
		putMap(contextBuilder, "tooltip", tooltip);
		putMap(contextBuilder, "summary", remoteIssueLink.getSummary());
		putMap(contextBuilder, "statusIconUrl", remoteIssueLink.getStatusIconUrl());
		putMap(contextBuilder, "statusIconTooltip", remoteIssueLink.getStatusIconTitle());
		putMap(contextBuilder, "statusIconLink", remoteIssueLink.getStatusIconLink());
		putMap(contextBuilder, "resolved", remoteIssueLink.isResolved() == null ? false : remoteIssueLink.isResolved());
		return contextBuilder.build();
	}

	private static void putMap(ImmutableMap.Builder<String, Object> mapBuilder, String key, Object value) {
		if (value != null) {
			mapBuilder.put(key, value);
		}
	}

	private static String getIconTooltip(RemoteIssueLink remoteIssueLink, I18nHelper i18n) {
		final boolean hasApplicationName = StringUtils.isNotEmpty(remoteIssueLink.getApplicationName());
		final boolean hasIconText = StringUtils.isNotEmpty(remoteIssueLink.getIconTitle());

		if (hasApplicationName && hasIconText) {
			return "[" + remoteIssueLink.getApplicationName() + "] " + remoteIssueLink.getIconTitle();
		} else if (hasApplicationName) {
			return "[" + remoteIssueLink.getApplicationName() + "]";
		} else if (hasIconText) {
			return remoteIssueLink.getIconTitle();
		} else {
			return i18n.getText("issuelinking.remote.link.weblink.title");
		}
	}

	private static String getTooltip(String appliationName, String summary, String title) {
		final boolean hasApplicationName = StringUtils.isNotEmpty(appliationName);
		final boolean hasSummary = StringUtils.isNotEmpty(summary);

		if (hasApplicationName && hasSummary) {
			return "[" + appliationName + "] " + title + ": " + summary;
		} else if (hasApplicationName) {
			return "[" + appliationName + "] " + title;
		} else if (hasSummary) {
			return title + ": " + summary;
		} else {
			return title;
		}
	}

	@Override
	public boolean requiresAsyncLoading(RemoteIssueLink remoteIssueLink) {
		return true;
	}

}
