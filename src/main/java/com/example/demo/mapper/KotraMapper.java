package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.demo.model.Company;
import com.example.demo.model.InCompany;
import com.example.demo.model.OutSample;

@Mapper
public interface KotraMapper {

	
	@Select("SELECT * FROM in_company WHERE seq = #{seq}")
	 InCompany findBySeq(@Param("seq") int seq);
	
	List<InCompany> findByInCompanyList(int count);
	
	@Insert("INSERT INTO out_sample(id, name, country, url, changeUrl, homeText, `language`) VALUES(#{id}, #{name}, #{country}, #{url}, #{changeUrl}, #{homeText}, #{language})")
	void add(OutSample bean);

	@Select("select * from out_sample limit 1")
	OutSample get();

	@Insert("INSERT INTO kotra_2.ko_International (line_num, id, name, country_cd, url, ch_url, org_text, org_lang_cd, trans_text, byte_length, reg_dt, mod_dt) VALUES(#{lineNum}, #{id}, #{name}, #{countryCd}, #{url}, #{chUrl}, #{orgText}, #{orgLangCd}, #{transText}, #{byteLength}, now(), now())")
	void addInternationalCompany(Company com);

	@Insert("INSERT INTO kotra_2.ko_Domestic (line_num, id, name, url, ch_url, org_text, org_lang_cd, trans_text, byte_length, reg_dt, mod_dt) VALUES(#{lineNum}, #{id}, #{name},  #{url}, #{chUrl}, #{orgText}, #{orgLangCd}, #{transText}, #{byteLength}, now(), now())")
	void addDomesticCompany(Company com);
	
	
}