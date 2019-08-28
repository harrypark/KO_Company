package com.example.demo.model;

import java.sql.Date;

import lombok.Data;

@Data
public class Company {
	private int lineNum; //라인번호
	private String id;   //기업아이디
	private String name; //기업명
	private String countryCd; //국가코드
	private String url;     //원본 URL
	private String chUrl; //변형 URL
	private int num;        
	private String orgText; 
	private String orgLangCd; //원본 언어코드
	
	private String transText; //번역
	
	private int byteLength;// 원본 byte사이즈
	
	private Date regDt;
	private Date modDt;
}
