package com.example.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.demo.mapper.KotraMapper;
import com.example.demo.model.Company;
import com.example.demo.model.TransResult;
import com.example.demo.util.Translator;
import com.opencsv.CSVReader;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class KOCompanyApplication implements CommandLineRunner{
	
	@Autowired
	private KotraMapper kotraMapper;
	
	private static KotraMapper mapper;

	@PostConstruct
	public void setKotraMapper() {
		this.mapper = this.kotraMapper;
	}
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(KOCompanyApplication.class, args);
		long beforeTime = System.currentTimeMillis(); //코드 실행 전에 시간 받아오기
		
		//static String cvsfile ="D:\\작업폴더\\KOTRA\\크롤링자료\\inter_co_sample4.csv";
//		String csvfile ="D:\\작업폴더\\KOTRA\\크롤링자료\\url\\해외기업.csv";
		String csvfile ="D:\\작업폴더\\KOTRA\\크롤링자료\\url\\국내기업.csv";
		File filename = new File(csvfile);
		
		if(csvfile.contains("국내")) {
			log.debug("======> 국내기업 크롤링 및 번역 시작.");
			domestic(filename,50);
		}else if(csvfile.contains("해외")){
			log.debug("======> 해외기업 크롤링 및 번역 시작.");
			international(filename, 1);
		}else {
			log.debug("======> csv 파일명을 확인하세요.");
		}
		
		
		
		
        long afterTime = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
        long secDiffTime = (afterTime - beforeTime)/1000; //두 시간에 차 계산
        log.debug("시간차이 : "+secDiffTime+"(초)");
		
	}

	

	

	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	/** 해외기업크롤링 번역
	 * @param fileName
	 * @param startLine
	 */
	private static void international(File fileName, int startLine) {
		log.debug("============> 해외기업 시작라인 : "+ startLine);
		int cutlen = 5500;
		int lineNum = 0;
		
	    List<Company> data = CSVReadInternational(fileName);
	    log.debug("============> 해외기업 전체 건수:"+data.size());
	    Iterator<Company> it = data.iterator();
	    while (it.hasNext()) {
	    	lineNum++;
	    	//log.debug("============================>"+lineNum);
	    	Company com = (Company) it.next();
	    	//국가 올션조절
	    	if(lineNum >= 1 && lineNum <= 2) {
	    	//if(lineNum >= startLine ) {
	        	String bodyText = jsoupBody(com.getChUrl());
	        	if(bodyText != null) {
	        		com.setOrgText(bodyText);
	        		//log.debug("bodyText:"+bodyText);
	        		//번역
	        		
	        		TransResult tr = Translator.subStrByteAndTranslate(bodyText, cutlen);
	        		//log.debug(tr.toString());
	        		if(tr != null) {
	        			com.setTransText(tr.getTransText());
	        			com.setOrgLangCd(tr.getLangCd());
	        			com.setByteLength(tr.getByteLength());
	        		}
	        	}
	            
	        	com.setLineNum(lineNum);
	        	//log.debug("get=====>"+mapper.get().toString());
	        	mapper.addInternationalCompany(com);
	            log.debug(com.toString());
	    	}
	    }
	}
	
	/** 국내기업 크롤링 번역
	 * @param fileName
	 * @param startLine
	 */
	private static void domestic(File fileName, int startLine) {
		log.debug("============> 국내기업 시작라인 : "+ startLine);
		int cutlen = 5500;
		int lineNum = 0;
		
	    List<Company> data = CSVReadDomestic(fileName);
	    log.debug("============> 국내기업 전체 건수:"+data.size());
	    Iterator<Company> it = data.iterator();
	    while (it.hasNext()) {
	    	lineNum++;
	    	//log.debug("============================>"+lineNum);
	    	Company com = (Company) it.next();
	    	//국가 올션조절
	    	if(lineNum >= startLine && lineNum <= 51) {
	    	//if(lineNum >= startLine ) {
	        	String bodyText = jsoupBody(com.getChUrl());
	        	if(bodyText != null) {
	        		com.setOrgText(bodyText);
	        		//log.debug("bodyText:"+bodyText);
	        		//번역
	        		
	        		TransResult tr = Translator.subStrByteAndTranslate(bodyText, cutlen);
	        		//log.debug(tr.toString());
	        		if(tr != null) {
	        			com.setTransText(tr.getTransText());
	        			com.setOrgLangCd(tr.getLangCd());
	        			com.setByteLength(tr.getByteLength());
	        		}
	        	}
	            
	        	com.setLineNum(lineNum);
	        	//log.debug("get=====>"+mapper.get().toString());
	        	mapper.addDomesticCompany(com);
	            log.debug(com.toString());
	    	}
	    }
		
	}
	
	/** 국내 csv파일 일기
	 * @param filename
	 * @return
	 */
	private static List<Company> CSVReadDomestic(File fileName) {
		List<Company> data = new ArrayList<Company>();
		try {
            CSVReader reader = new CSVReader(new FileReader(fileName));
            // UTF-8
            // CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"), ",", '"', 1);
            ColumnPositionMappingStrategy<Company> start = new ColumnPositionMappingStrategy<Company>();
            start.setType(Company.class);
            String[] columns = new String[] { "id", "name", "url","chUrl" };
            start.setColumnMapping(columns);
            CsvToBean<Company> csv = new CsvToBean<Company>();
            data = csv. parse(start, reader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
		return data;
	}

	/** 해외 csv파일 일기
	 * @param filename
	 * @return
	 */
	private static List<Company> CSVReadInternational(File filename) {
		List<Company> data = new ArrayList<Company>();
		try {
            CSVReader reader = new CSVReader(new FileReader(filename));
            // UTF-8
            // CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"), ",", '"', 1);
            ColumnPositionMappingStrategy<Company> start = new ColumnPositionMappingStrategy<Company>();
            start.setType(Company.class);
            String[] columns = new String[] { "id", "name", "countryCd","url","chUrl" };
            start.setColumnMapping(columns);
            CsvToBean<Company> csv = new CsvToBean<Company>();
            data = csv. parse(start, reader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
		return data;
	}
	
	
	
	/** URL을 받아 body text 크롤링
	 * @param webPage
	 * @return
	 */
	private static String jsoupBody(String webPage) {
		
		//String webPage = "http://www.aspic.co.kr";//en
        
        //webPage = "http://www.hdlift.co.kr";
		String bodyText = null;
        try {
			//String html2 = Jsoup.connect(webPage).get().html();
        	//get input stream from the URL
            InputStream inStream = new URL(webPage).openStream();
            Document document = Jsoup.parse(inStream, "UTF-8", webPage);
        	
			//Document document = Jsoup.connect(webPage).get();
			//System.out.printf("Html: %s%n", html2);
			bodyText = document.select("body").text();
			
			//log.debug("Language:"+detectLanguage(bodyText));
			//System.out.printf("Body: %s", bodyText);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return bodyText;
	}
	
	
	
	/*
	private static List<String []> CSVRead(File filename) {
		List<String[]> data = new ArrayList<String[]>();
		try {
            CSVReader reader = new CSVReader(new FileReader(filename));
            // UTF-8
            // CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"), ",", '"', 1);
            String[] s;
            while ((s = reader.readNext()) != null) {
                log.debug(s);
            	data.add(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
		return data;
	}
	*/
	
	
	
	
	

}
