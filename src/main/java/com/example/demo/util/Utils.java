package com.example.demo.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.dongliu.requests.Parameter;

public class Utils {
	@SuppressWarnings("unchecked")
	public static List<Parameter<? extends Entry<String, ?>>> buildParams(String text, String src, String dest, String token) {
		Parameter [] params = {
		Parameter.of("client", "webapp"),
		Parameter.of("sl", src),
		Parameter.of("tl", dest),
		Parameter.of("hl", dest),
		Parameter.of("dt", "at"),
		Parameter.of("dt", "bd"),
		Parameter.of("dt", "ex"),
		Parameter.of("dt", "ld"),
		Parameter.of("dt", "md"),
		Parameter.of("dt", "qca"),
		Parameter.of("dt", "rw"),
		Parameter.of("dt", "rm"),
		Parameter.of("dt", "ss"),
		Parameter.of("dt", "t"),
		Parameter.of("ie", "UTF-8"),
		Parameter.of("oe", "UTF-8"),
		Parameter.of("otf", 1),
		Parameter.of("ssel", 0),
		Parameter.of("tsel", 0),
		Parameter.of("tk", token),
		Parameter.of("q", text)
		};
		return Arrays.asList(params);
	}

	public static List formatJson(String original) {
		//System.out.println("original:  "+original);
		List list = null;

		try {
		list = new Gson().fromJson(original, List.class);
		}catch (Exception e) {

			list = null;
			throw new IllegalStateException("Expected BEGIN_ARRAY but was STRING at line 1 column 1 path $");
		}
	    return list;
	}

	public static void main(String [] args) {
		buildParams("철","ko","en","token");
	}

}
