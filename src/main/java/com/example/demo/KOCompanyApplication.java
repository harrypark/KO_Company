package com.example.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.demo.mapper.KotraMapper;
import com.example.demo.model.Company;
import com.example.demo.model.Crawling;
import com.example.demo.model.TransResult;
import com.example.demo.util.Translator;
import com.opencsv.CSVReader;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.vdurmont.emoji.EmojiParser;

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

	static Pattern em = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+");


	public static void main(String[] args) throws Exception {
		SpringApplication.run(KOCompanyApplication.class, args);
		long beforeTime = System.currentTimeMillis(); //코드 실행 전에 시간 받아오기

		//static String cvsfile ="D:\\작업폴더\\KOTRA\\크롤링자료\\inter_co_sample4.csv";
		String csvfile ="D:\\작업폴더\\KOTRA\\크롤링자료\\url\\해외기업.csv";
//		String csvfile ="D:\\작업폴더\\KOTRA\\크롤링자료\\url\\국내기업.csv";
		File filename = new File(csvfile);
		try {

			if(csvfile.contains("국내")) {
				log.debug("======> 국내기업 크롤링 및 번역 시작.");
				int duplicated_line_num_count = mapper.getDomesticDuplicatedineNumCount();
				if(duplicated_line_num_count > 0) {
					log.debug("======>###### 국내기업 중복되는 라인번호가 존재 합니다. DB를 확인 하세요.");
				}else {
					int max_line_num = mapper.getDomesticMaxLineNum();
					domestic(filename,max_line_num+1);
				}

			}else if(csvfile.contains("해외")){
				log.debug("======> 해외기업 크롤링 및 번역 시작.");
				int duplicated_line_num_count = mapper.getInternationalDuplicatedineNumCount();
				if(duplicated_line_num_count > 0) {
					log.debug("======>###### 해외기업 중복되는 라인번호가 존재 합니다. DB를 확인 하세요.");
				}else {
					
					int max_line_num = mapper.getInternationalMaxLineNum200000();
					/*
					InetAddress local = InetAddress.getLocalHost();
					String ip = local.getHostAddress();
					System.out.println(ip);
					*/
					international(filename,max_line_num+1);
				}
			}else {
				log.debug("======> csv 파일명을 확인하세요.");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			long afterTime = System.currentTimeMillis(); // 코드 실행 후에 시간 받아오기
	        long secDiffTime = (afterTime - beforeTime)/1000; //두 시간에 차 계산
	        log.debug("시간차이 : "+secDiffTime+"(초)");
		}





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
		int cutlen = 5000;
		int lineNum = 0;

	    List<Company> data = CSVReadInternational(fileName);
	    log.debug("============> 해외기업 전체 건수:"+data.size());
	    Iterator<Company> it = data.iterator();
	    while (it.hasNext()) {
	    	lineNum++;
	    	//log.debug("============================>"+lineNum);
	    	Company com = (Company) it.next();
	    	//국가 올션조절
//	    	if(lineNum >= startLine && lineNum <= 1000) {
	    	if(lineNum >= startLine ) {
	    		Crawling cr= jsoupBody(com.getChUrl());
	    		//chURL길이체크
	    		if(com.getChUrl().length() >600) {
	    			com.setChUrl(com.getChUrl().substring(0, 600));
	    		}
	        	if(cr!= null) {
	        		if(StringUtils.isNotEmpty(cr.getDescription())) {
	        			com.setOrgDesc(cr.getDescription());
	        			//System.out.println(cr.getDescription());
	        			TransResult tr = Translator.subStrByteAndTranslate(cr.getDescription(), cutlen);
	        			if(tr != null) {
	        				//System.out.println(tr.getTransText());
		        			com.setTransDesc(tr.getTransText());
		        		}

	        		}

		        	if(StringUtils.isNotEmpty(cr.getKeywords())) {
	        			com.setOrgKeywords(cr.getKeywords());
	        			TransResult tr = Translator.subStrByteAndTranslate(cr.getKeywords(), cutlen);
	        			if(tr != null) {
		        			com.setTransKeywords(tr.getTransText());
		        		}
	        		}
		        	
		        	
				    	if(StringUtils.isNotEmpty(cr.getBodyText())){
				    		com.setOrgText(cr.getBodyText());
				    		TransResult tr = Translator.subStrByteAndTranslate(cr.getBodyText(), cutlen);
				    		if(tr != null) {
			        			com.setTransText(tr.getTransText());
			        			com.setOrgLangCd(tr.getLangCd());
			        			com.setByteLength(tr.getByteLength());
			        		}
			    		}
		        	
	    		}

	        	com.setLineNum(lineNum);
	        	//log.debug("get=====>"+mapper.get().toString());
	        	//log.debug(com.toString());
	        	log.debug("l_n: {} , byte : {} ,url: {}",com.getLineNum(),com.getByteLength(),com.getChUrl() );
	        	mapper.addInternationalCompany(com);
	    	}
	    }
	}

	/** 국내기업 크롤링 번역
	 * @param fileName
	 * @param startLine
	 */
	private static void domestic(File fileName, int startLine) {
		log.debug("============> 국내기업 시작라인 : "+ startLine);
		int cutlen = 5000;
		int lineNum = 0;

		TransResult tr1 = new TransResult();
		TransResult tr2 = new TransResult();
		TransResult tr3 = new TransResult();

	    List<Company> data = CSVReadDomestic(fileName);
	    log.debug("============> 국내기업 전체 건수:"+data.size());
	    Iterator<Company> it = data.iterator();
	    while (it.hasNext()) {
	    	lineNum++;
	    	//log.debug("============================>"+lineNum);
	    	Company com = (Company) it.next();
	    	//국가 올션조절
//	    	if(lineNum >= startLine && lineNum <= 7260) {
	    	if(lineNum >= startLine ) {
	    	//if(lineNum >= startLine ) {
	    		Crawling cr= jsoupBody(com.getChUrl());
	        	if(cr!= null) {
	        		if(StringUtils.isNotEmpty(cr.getDescription())) {
	        			com.setOrgDesc(cr.getDescription());
	        			tr1 = Translator.subStrByteAndTranslate(cr.getDescription(), cutlen);
	        			if(tr1 != null) {
		        			com.setTransDesc(tr1.getTransText());
		        		}

	        		}

		        	if(StringUtils.isNotEmpty(cr.getKeywords())) {
	        			com.setOrgKeywords(cr.getKeywords());
	        			tr2 = Translator.subStrByteAndTranslate(cr.getKeywords(), cutlen);
	        			if(tr2 != null) {
		        			com.setTransKeywords(tr2.getTransText());
		        		}
	        		}

			    	if(StringUtils.isNotEmpty(cr.getBodyText())){
			    		com.setOrgText(cr.getBodyText());
			    		tr3 = Translator.subStrByteAndTranslate(cr.getBodyText(), cutlen);
			    		if(tr3 != null) {
		        			com.setTransText(tr3.getTransText());
		        			com.setOrgLangCd(tr3.getLangCd());
		        			com.setByteLength(tr3.getByteLength());
		        		}
		    		}
	    		}



	        	com.setLineNum(lineNum);
	        	//log.debug("get=====>"+mapper.get().toString());
	        	//log.debug(com.toString());
	        	log.debug("l_n: {} , byte : {} ,url: {}",com.getLineNum(),com.getByteLength(),com.getChUrl() );
	        	mapper.addDomesticCompany(com);
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
	@SuppressWarnings("finally")
	private static Crawling jsoupBody(String webPage) {

		String bodyText = null;
		String description = null;
		String keywords = null;
        try {
			//String html2 = Jsoup.connect(webPage).get().html();
        	//get input stream from the URL
//            InputStream inStream = new URL(webPage).openStream();
//            Document document = Jsoup.parse(inStream, "UTF-8", webPage);

			Document document = Jsoup.connect(webPage).timeout(10*1000).get();

			//meta tag
			String desc1=document.select("meta[name=description]").attr("content");
			String desc2=document.select("meta[property=og:description]").attr("content");
			String desc3=document.select("meta[name=twitter:description]").attr("content");
			if(StringUtils.isNotEmpty(desc1)) {
				description = emChange(desc1);
			}else if(StringUtils.isNotEmpty(desc2)) {
				description = emChange(desc2);
			}else if(StringUtils.isNotEmpty(desc3)) {
				description = emChange(desc3);
			}

			if(StringUtils.isNotEmpty(description)) {
				description = EmojiParser.replaceAllEmojis(description, "");
				keywords = emChange(description);
				byte[] descriptionbyte = description.getBytes();
				if(descriptionbyte.length>3000) {
					description = new String(descriptionbyte,0,3000);
				}
			}

			//log.debug("Meta Description: " + description);
			String key =  document.select("meta[name=keywords]").attr("content");
			if(StringUtils.isNotEmpty(key)) {
				keywords = emChange(key);
				byte[] keybyte = keywords.getBytes();
				if(keybyte.length>3000) {
					keywords = new String(keybyte,0,3000);
				}
			}
			//log.debug("keywords: " + keywords);
					
			
			bodyText = document.select("body").text();
			//이모티콘제거
			bodyText = emChange(bodyText.replaceAll("amp;", " "));
			//log.debug(bodyText);

			/*
			if(bodyText == null || bodyText.trim().length()==0) {
				bodyText = null;
				description = null;
				keywords = null;
			}
			*/

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			return new Crawling(description, keywords, bodyText);
		}


	}

	/** 이모티콘제거
	 * @param str
	 * @return
	 */
	private static String emChange(String str) {
		Matcher emMatcher = em.matcher(str);
		str = emMatcher.replaceAll("");
		str = str.replaceAll("(\r|\n|\r\n|\n\r)","");
		return str;
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
