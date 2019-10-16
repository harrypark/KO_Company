package com.example.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.model.BackTrans;
import com.example.demo.model.Translated;
import com.example.demo.util.Translator;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class BackTranslationKeyword {

	static String csvfile1 ="D:\\작업폴더\\KOTRA\\역번역_keywords\\keywords_trans.csv";
	static String csvfile2 ="D:\\작업폴더\\KOTRA\\역번역_keywords\\keywords_trans_result.csv";
	public static void main(String[] args) throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IOException, InterruptedException {
		log.info("키워드 역번역 시작");
		
		
		
		Translator ts = new Translator();
		
		List<BackTrans> data = CSVReadKeyword(csvfile1);
		int totalRow = data.size();
		
		log.info("CSV 파일 총갯수:{}" ,totalRow);
		
		List<BackTrans> wlist = new ArrayList<BackTrans>();
		
		//결과파일의 마지막 라인
		int readLastRow = CSVReadBackTransKeyword(csvfile2);
		log.info("readLastRow:{}",readLastRow);
		
		int start = readLastRow;
		int row=1;
		
		int modSize = 1;
		ArrayList<String> ar = new ArrayList<String>();
		for(BackTrans t : data) {
			if(row > start) {
				ar.add(t.getId()+'^'+'"'+t.getKeyword()+'"');
				//ar.add(t.getKeyword());
				wlist.add(t);
				if(row % modSize == 0) {
					String str = String.join(",", ar);
					Translated td = ts.translate(str, "ko", "auto");
					
					//log.info("1========================>"+wlist.size());
						//System.out.println(td.getOrigin());
						String [] origin = td.getOrigin().split(",");
						int originSize = origin.length;
						//log.info("Origin() size:{}",origin.length);
						//System.out.println(td.getText());
						if(modSize ==1 && td.getText().contains(",")) {
							td.setText(td.getText().replaceAll(",", ""));
						}
												
						String [] trans = td.getText().split(",");
						int transSize = trans.length;
						//log.info("trans size:{}",trans.length);
						if(originSize==transSize) {
							for(int i=0 ; i<originSize; i++ ) {
								//System.out.println(origin[i] +":"+ trans[i]);
								String tranSp [] = trans[i].split("\\^");
								if(tranSp.length < 2) {
									System.out.println(i+ "---->>splet Array Error : "+trans[i]);
									System.out.println(td.getOrigin());
									System.out.println(td.getText());
									wlist.get(i).setTrans(tranSp[0].replaceAll("\"", "").trim());
								}else {
									wlist.get(i).setTrans(tranSp[1].replaceAll("\"", "").trim());
								}
								
								//System.out.println(trans[i].split("\\^").length);
								//System.out.println(wlist.get(i).toString());
							}
							//log.info("2========================>"+wlist.size());
							CSVWriteBackTransKeyword(wlist, csvfile2);	
							log.info("======>>> cvs write : {}" ,wlist.get(originSize-1).getId());
							//Thread.sleep(2000); //1초 대기
						}else {
							log.info("origin size:{}",origin.length);
							log.info("trans size:{}",trans.length);
							System.out.println(td.getOrigin());
							System.out.println(td.getText());
							break;
						}
						ar.clear();
						wlist.clear();
					}
					
									
								
			}
			row++;	
		}
		
		
		
		
		
		
		/*
		int start = readLastRow;
		int row = 1;
	    int cutlen = 100;
	    int nCnt =0;
	    StringBuffer sbStr1 = new StringBuffer(cutlen);
		StringBuffer sbStr2 = new StringBuffer();
		for(BackTrans t : data) {
	    	
			if(row > start) {
					nCnt += String.valueOf(t.getKeyword()).getBytes().length;
					if(nCnt <= cutlen) {
						sbStr1.append(ch);
						//System.out.println("sbStr1:"+sbStr1.toString().getBytes().length);
					}else {
						sbStr2.append(ch);
						//System.out.println("sbStr2:"+sbStr2.toString().getBytes().length);
					}

				}
				//System.out.println(i++ +"sbStr1:"+sbStr1.toString());
				Translated td = ts.translate(sbStr1.toString(), "en", "auto");
				
				
				
			}
			
			
			
			
			if(row > start && row <301) {
			   	log.info(row +":"+t.getKeyword());
				list.add(t);
			}
	    	
			if(list.size()==10) {
				CSVWriteBackTransKeyword(list, csvfile2);
	    		list = new ArrayList<BackTrans>();
	    		log.info("csv write : {}", row);
			}

	    		
	    	
	    		
	    	row++;
	    }
		
		if(list.size()>0) {
			CSVWriteBackTransKeyword(list, csvfile2);
		}
	    */
	    
	    
	    
	    
		
		
		
		
		

	}

	private static int CSVReadBackTransKeyword(String csvfile) {
		int result = 0;
		File fileName = new File(csvfile);
		
		if(!fileName.exists()) { return result;}
				
		List<BackTrans> data = new ArrayList<BackTrans>();
		try {
            CSVReader reader //= new CSVReader(new FileReader(fileName),',','\"',1);
             = new CSVReaderBuilder(new FileReader(fileName)).build();
            
            
            // UTF-8
            // CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"), ",", '"', 1);
            ColumnPositionMappingStrategy<BackTrans> start = new ColumnPositionMappingStrategy<BackTrans>();
            start.setType(BackTrans.class);
            String[] columns = new String[] { "id", "keyword", "freq", "trans" };
            start.setColumnMapping(columns);
            
            CsvToBean<BackTrans> csvToBean = new CsvToBeanBuilder<BackTrans>(reader)
                    .withMappingStrategy(start)
                    //.withSkipLines(1)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            
            data = csvToBean.parse();
            
//            CsvToBean<BackTrans> csv = new CsvToBean<BackTrans>();
//            data = csv. parse(start, reader);
            if(data.size()>0) {
            	result = Integer.parseInt(data.get(data.size()-1).getId()); 
            }
            
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
		return result;
	}

	private static void CSVWriteBackTransKeyword(List<BackTrans> list, String csvfile) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
		
		File filename = new File(csvfile);
		Writer writer = new FileWriter(filename, true);
		
		 // mapping of columns with their positions
        ColumnPositionMappingStrategy<BackTrans> mappingStrategy = new ColumnPositionMappingStrategy<BackTrans>();
        // Set mappingStrategy type to Product Type
        mappingStrategy.setType(BackTrans.class);
        // Fields in Product Bean
        String[] columns = new String[] { "id", "keyword", "freq", "trans" };
        // Setting the colums for mappingStrategy
        mappingStrategy.setColumnMapping(columns);
		
		StatefulBeanToCsvBuilder<BackTrans> builder = new StatefulBeanToCsvBuilder<>(writer);
        StatefulBeanToCsv<BackTrans> beanWriter = builder
        		.withMappingStrategy(mappingStrategy)
        		.withQuotechar(CSVWriter.DEFAULT_QUOTE_CHARACTER)
        		.withSeparator(CSVWriter.DEFAULT_SEPARATOR)
        		.build();
     
        beanWriter.write(list);
        writer.close();
		
	}

	@SuppressWarnings("deprecation")
	private static List<BackTrans> CSVReadKeyword(String csvfile) {
		File fileName = new File(csvfile);
		List<BackTrans> data = new ArrayList<BackTrans>();
		try {
            CSVReader reader //= new CSVReader(new FileReader(fileName),',','\"',1);
             = new CSVReaderBuilder(new FileReader(fileName)).withSkipLines(1).build();
            
            
            // UTF-8
            // CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"), ",", '"', 1);
            ColumnPositionMappingStrategy<BackTrans> start = new ColumnPositionMappingStrategy<BackTrans>();
            start.setType(BackTrans.class);
            String[] columns = new String[] { "id", "keyword", "freq" };
            start.setColumnMapping(columns);
            
            CsvToBean<BackTrans> csvToBean = new CsvToBeanBuilder<BackTrans>(reader)
                    .withMappingStrategy(start)
                    .withSkipLines(1)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            
            data = csvToBean.parse();
            
//            CsvToBean<BackTrans> csv = new CsvToBean<BackTrans>();
//            data = csv. parse(start, reader);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
		return data;
	}

}
