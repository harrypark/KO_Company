package com.example.demo;

import java.io.IOException;
import java.net.UnknownHostException;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class JsoupTest {

	public static void main(String[] args)  {
		
		//
		String url = "http://www.eyserkids.com";
		//http://www.tiesege.com
		
		try {
			Response response = Jsoup.connect(url).execute();
			System.out.println(response.statusCode() + " : " + response.url());
			
			Document document = response.parse();
			
			String bodyText = document.select("body").text();
			
			System.out.println("bodyText:"+bodyText);
		}catch (UnknownHostException e) {
			//e.printStackTrace();
			System.out.println("UnknownHostException");
			// TODO: handle exception
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

}
