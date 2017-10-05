package com.go2group.uservoice.bean;

public class Forum {
	
	private String name;
	private String id;
	private String url;
	private String description;
	
	public Forum() {
		super();
	}

	public Forum(String name, String id, String url, String description) {
		super();
		this.name = name;
		this.id = id;
		this.url = url;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
