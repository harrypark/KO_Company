package com.example.demo.model;

import com.opencsv.bean.CsvBindByPosition;

import lombok.Data;

@Data
public class BackTrans {
//	@CsvBindByPosition(position = 0)
	private String id;
//	@CsvBindByPosition(position = 1)
	private String keyword;
//	@CsvBindByPosition(position = 2)
	private String freq;
//	@CsvBindByPosition(position = 3)
	private String trans;
}
