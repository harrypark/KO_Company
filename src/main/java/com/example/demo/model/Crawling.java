package com.example.demo.model;


import lombok.Data;

@Data
public class Crawling {
	private String description;
	private String keywords;
	private String bodyText;
	
	public Crawling() {}
	
	public Crawling(String description, String keywords, String bodyText) {
		super();
		this.description = description;
		this.keywords = keywords;
		this.bodyText = bodyText;
	}
	
	
	
	
}