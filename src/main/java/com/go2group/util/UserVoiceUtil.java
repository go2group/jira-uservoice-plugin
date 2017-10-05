package com.go2group.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import oauth.signpost.OAuthConsumer;

public class UserVoiceUtil {

	public static String getUserVoiceData(OAuthConsumer consumer, String signedUrl, String method) throws IOException {
		HttpURLConnection connection = null;
		InputStream inputStream = null;
		try {
			URL url = new URL(signedUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method);
			consumer.sign(connection);
			connection.connect();
			inputStream = connection.getInputStream();
			return getString(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (connection != null)
				connection.disconnect();
			if (inputStream != null)
				inputStream.close();
		}
		;
		return null;
	}
	
	public static String postUserVoiceData(OAuthConsumer consumer, String signedUrl, String method, String contentType, String body) throws IOException {
		HttpURLConnection connection = null;
		InputStream inputStream = null;
		String charset = "UTF-8";/*  Changes for JUVP-36 */ 
		try {
			URL url = new URL(signedUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method);
			/*  Changes for JUVP-36 - start */ 
			connection.setRequestProperty("Content-Type", contentType + ";charset=" + charset);
			connection.setRequestProperty("Accept-Charset", charset);
			connection.setRequestProperty("Content-Length", String.valueOf(body.getBytes().length));
			/*  Changes for JUVP-36 - end */ 
			connection.setDoInput(true);
			connection.setDoOutput(true);
			
			consumer.sign(connection);
			
			DataOutputStream outStream = new DataOutputStream(connection.getOutputStream());
			outStream.write(body.getBytes(charset));/*  Changes for JUVP-36 */ 
			outStream.flush();
			outStream.close();
			
			connection.connect();
			
			inputStream = connection.getInputStream();
			return getString(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (connection != null)
				connection.disconnect();
			if (inputStream != null)
				inputStream.close();
		}
		return null;
	}
	
	private static String getString(InputStream inputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null)
				sb.append(new StringBuilder().append(line).append("\n").toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				reader.close();
			if (inputStream != null)
				inputStream.close();
		}

		return sb.toString();
	}
}
