package com.example.demo.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class Translated {
	private String src;
	private String dest;
	private String origin;
	private String text;
	private String pronunciation;
	private Map<String,Object> extraData;
	//extra_data
}

