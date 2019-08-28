package com.example.demo.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class MatchTest {

	
	static String reg1 = "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w_\\.-]*)*\\/?$";
	static Pattern pc = Pattern.compile(reg1);
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String readLine = "1041429;유니디자인;http://www.unidesign.co.kr/shop/display/home.php?mode=home";
		
		String str[] = readLine.split(";");
//		System.out.println(str.length);
		if(str.length==3) {
			String chUrl = urlcheck(str[2]);
			System.out.println(chUrl);
			//data.add(new String[] { str[0], str[1], str[2], str[3], chUrl  });
			
			System.out.println(str[0]+"/"+ str[1]+"/"+ str[2]);
			//writer.writeNext(new String[] { str[0], str[1], str[2], str[3], chUrl  });
			//writer.writeNext(new String[] { str[0], str[1], str[2],  chUrl  });
		}
		
		
		
	}

	private static String urlcheck(String str) {
		String result=null;
		//String reg1 = "https?://(\\w*:\\w*@)?[-\\w.]+(:\\d+)?(/([\\w/_.]*(\\?\\S+)?)?)?";
		  
		  //http://s.nts.go.kr/menu/intro/intro.asp?tax_code=214#basic
		//String reg2 = "([a-z0-9-]+\\.)+[a-z0-9]{2,4}.*$/"; 
		try {
		String str2 = StringUtils.trimAllWhitespace(str).toLowerCase();
		System.out.println(str2);
	//	System.out.println(str2.matches(reg1)); 
		
		
		Matcher mc = pc.matcher(str2);
		
		if(mc.matches()){
			System.out.println("not null");
			if(StringUtils.startsWithIgnoreCase(str2, "www.")){
				result = "http://"+str2;
			}else {
				result = str2;
			}
		}else {
			result = null;
			System.out.println("null");
		}
		System.out.println("=====================================");
		System.out.println(result);	
		System.out.println("=====================================");
		}catch (Exception e) {
			e.printStackTrace(); 
		}	
			
		
		return result;
	}
}
