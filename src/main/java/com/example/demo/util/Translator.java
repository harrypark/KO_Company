package com.example.demo.util;

import static com.example.demo.util.Contants.DEFAULT_USER_AGENT;
import static com.example.demo.util.Contants.LANGCODES;
import static com.example.demo.util.Contants.LANGUAGES;
import static com.example.demo.util.Contants.SPECIAL_CASES;
import static com.example.demo.util.Contants.TRANSLATE;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.demo.model.Detected;
import com.example.demo.model.TransResult;
import com.example.demo.model.Translated;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

public class Translator {

	private Session session;
	private String[] serviceUrls = { "translate.google.com" };
	private String userAgent = DEFAULT_USER_AGENT;
	private TokenAcquirer tokenAcquirer;
	private long timeout = 3000;

	public Translator() {
		this.session = Requests.session();
		this.tokenAcquirer = new TokenAcquirer(session, pickServiceUrl());
	}
	
	public String pickServiceUrl() {
        if(serviceUrls.length == 1)
            return this.serviceUrls[0];
        return serviceUrls[new Random().nextInt(serviceUrls.length)];
	}

	public List<String[]> _translate(String text, String dest, String src) {
		String token = this.tokenAcquirer.get(text);
		String url = TRANSLATE.replaceFirst("\\{host\\}", pickServiceUrl());
		RawResponse r = session.get(url).params(Utils.buildParams(text,src,dest,token)).send();
		String result = r.readToText();
		//System.out.println(result);
		return Utils.formatJson(result);
	}

	public Translated translate(String text, String dest, String src) {
		if(dest == null)
			dest = "en";
		if(src == null)
			dest = "auto";
        dest = dest.toLowerCase().split("_",1)[0];
        src = src.toLowerCase().split("_",1)[0];
        
        if(!src.equals("auto") && !LANGUAGES.containsKey(src))
        	if(SPECIAL_CASES.containsKey(src))
        		src = SPECIAL_CASES.get(src);
        	else if(LANGCODES.containsKey(src))
        		src = LANGCODES.get(src);
        	else
        		throw new IllegalArgumentException("invalid source language");
        
        if(!dest.equals("auto") && !LANGUAGES.containsKey(dest))
        	if(SPECIAL_CASES.containsKey(dest))
        		dest = SPECIAL_CASES.get(dest);
        	else if(LANGCODES.containsKey(dest))
        		dest = LANGCODES.get(dest);
        	else
        		throw new IllegalArgumentException("invalid destination language");   
        
        List data = _translate(text, dest, src);
        //System.out.println(data);
        //System.out.println(data.get(0));	
        List<String> t2 = new ArrayList<>();
        for (Object object : (List) data.get(0)) {
			List<String> t = (List<String>) object;			
			t2.add(nvl(t.get(0)));
		} 
        String translated = String.join("", t2);
        //System.out.println(translated);
        
        Map<String,Object> extraData = parseExtraData(data);
		for(Entry<String,Object> entry : extraData.entrySet()) {
			//System.out.println(entry.getKey() + " : " + entry.getValue());
		}
		try {
			src = (String) data.get(2);
		}catch(Exception e) {
			e.printStackTrace();
		}
		String pron = text;
		try {
			List prons = ((List)((List)((List)data.get(0))).get(1));
//			System.out.println(prons.size());
//			System.out.println(prons.get(prons.size()-1));
			
			pron =  (String) prons.get(prons.size()-1);
		}catch(Exception e){
			//e.printStackTrace();
		}        
        return new Translated(src, dest, text, translated, pron, extraData);
	}
	
	Map<Integer,String> respPartsNameMap = Stream.of(
            new AbstractMap.SimpleEntry<>(0, "translation"),
            new AbstractMap.SimpleEntry<>(1, "all-translations"),
            new AbstractMap.SimpleEntry<>(2, "original-language"),
            new AbstractMap.SimpleEntry<>(5, "possible-translations"),
            new AbstractMap.SimpleEntry<>(6, "confidence"),
            new AbstractMap.SimpleEntry<>(7, "possible-mistakes"),
            new AbstractMap.SimpleEntry<>(8, "language"),
            new AbstractMap.SimpleEntry<>(11, "synonyms"),
            new AbstractMap.SimpleEntry<>(12, "definitions"),
            new AbstractMap.SimpleEntry<>(13, "examples"),
            new AbstractMap.SimpleEntry<>(14, "see-also")
			).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	
	public Map<String,Object> parseExtraData(List data){
		Map<String,Object> extra = new HashMap<>();
		int dtSize = data.size();
		for(Entry<Integer,String> entry : respPartsNameMap.entrySet()) {
			extra.put(entry.getValue(), (entry.getKey() < dtSize && data.get(entry.getKey()) != null)?data.get(entry.getKey()):null);
		}
		//System.out.println(extra);
        return extra;
	}
	
	public Detected detect(String text) {
		List data = _translate(text, "en", "auto");
		//System.out.println(data);
		String src = "";
		float confidence = 0.0f;
		Detected detected = null;
		try {
			List conf = (List) data.get(8);

			src = String.join(src, (String)((List) conf.get(0)).get(0));
			List conf2 = (List) conf.get(conf.size() - 2);

			confidence = ((Double) conf2.get(0)).floatValue();
			detected = new Detected(src, confidence, text);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return detected;
	}
	
	private String nvl(String input) {		
		if(input == null || input.trim().length() == 0)
			return "";
		else
			return input.trim();
	}

	public static void main(String[] args) throws Exception {
		Translator ts = new Translator();
		String text = "대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다.대피소에서 현재 판매 중인 품목 수도 17개에서 10개로 축소한 후, 단계적으로 폐지해 나갈 계획이다. 먼저 판매가 중지될 품목은 초코바, 초코파이, 캔커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생 문제 등을 고려해 금지된다. 환경부 자연공원과는 “2019년도부터 안전과 관련 없는 물품은 판매를 중지하고, 2020년부터 안전용품 판매도 점진적으로 축소하는 안을 검토 중”이라고 밝혔다. 물품 판매제를 폐지하는 대신 ‘응급구호물품’을 비치해 비상시 무상지급할 계획이다."; 
		//text="Найти en Компания Продукция Счетчики электрической энергии и системы учета Меркурий Торговое оборудование Меркурий LED осветительные системы ЛидерЛайт Средства отображения информации Дисплейные системы IP видеокамеры и системы видеонаблюдения ТВ Хелп Тахографы Меркурий Решения Проекты Поддержка Купить Партнерам Контакты Разработка и производство высокотехнологичного электронного оборудования Инкотекс производит более 4 миллионов счетчиков в год Инкотекс — один из ведущих производителей торгового оборудования с 1993 г. Модернизации системы уличного освещения в г. Туле, 15 000 осветителей Медиафасады производства Инкотекс в центре столицы, 2015 г. Одно из новых направлений развития Инкотекс — оборудование IP видеонаблюдения Непрерывная регистрацию информации о скорости и маршруте движения ТС Счетчики электрической энергии и системы учета Меркурий В каталог продукции Выпускается более 100 видов счетчиков: от простейших бытовых однофазных, до трехфазных промышленных, разработаны автоматизированные системы контроля и учета энергоресурсов (АСКУЭ) Торговое оборудование Меркурий В каталог продукции Широкий спектр торгового оборудования: контрольно-кассовой техники, чекопечатающих устройств, электронных весов и таксографов LED осветительные системы ЛидерЛайт В каталог продукции Светодиодные светильники практически для всех сфер применения: от бытовых ламп до магистрального освещения, в том числе с функцией управления. Средства отображения информации Дисплейные системы В каталог продукции Решения для наружной и внутренней рекламы, информационные системы для транспорта, медицины, спорта, образования. IP видеокамеры и системы видеонаблюдения ТВ Хелп В каталог продукции Сетевые IP-камеры для наружного и внутреннего наблюдения, в т. ч. роботизированные, сетевые видеорегистраторы, видеоинкодеры, специализированное ПО Тахографы Меркурий В каталог продукции Регистраторы скорости и маршрута движения транспортных средств Уважаемые коллеги Приветствую Вас на сайте Incotex Electronics Group. С момента создания первой компании в 1989 г. в течении 28 лет шло строительство компаний Инкотекс, и сегодня это многопрофильная группа, которая лидирует в РФ по четырём направлениям деятельности, насчитывает более 3000 человек, в том числе более 200 человек в разработке. Мы выпускаем только ту продукцию, которую разрабатываем внутри группы и надеюсь, что Вы сможете увидеть у нас много интересного, поскольку мы имеем более 200 патентов и поставляем электронику более чем в 30 стран Мира. Приглашаю к сотрудничеству заинтересованные лица и компании для разработки и производства новых оригинальных изделий. Желаю вам здоровья и удачи. Основатель группы к.т.н. Соколов Юрий Борисович События Securika Moscow 2019 18.03.2019 В период с 19 по 22 марта 2019 года, компания «ТВ ХЕЛП», разработчик и производитель электронного оборудования, примет участие в 25-й Международной выставке технических средств охраны и оборудования для обеспечения безопасности и противопожарной защиты Securika Moscow 2019. Международная энергетическая выставка Middle East Electricity 2019 (MEE) в Дубае (ОАЭ) 05.03.2019 Международная энергетическая выставка Middle East Electricity 2019 (MEE) в Дубае (ОАЭ). C Новым 2019 годом! 29.12.2018 «Incotex Electronics Group» поздравляет Вас с наступающим Новым годом и Рождеством! Все события 800+ типов продукции 4 завода в России 30+ стран экспорта 200+ патентовиз них более 60 международных 800+ типов продукции 4 завода в России 30+ стран экспорта 200+ патентовиз них более 60 международных Производство группы аттестовано по стандарту IQNET ISO 9001-2008 и стандартам Германии, Испании и Италии. Подробнее о компании Заводы и офисы компании Заводы Офисы Представительства Ключевые проекты Новая студия «ВЕСТИ» ВГТРК Россия. г. Москва Подробнее о проекте Модернизация освещения Сбербанка России Россия, г. Москва Более 150 000 светильников встраиваемого типа были установлены Существенное снижение потребления электроэнергии Снижение расходов на выключатели и проводку Подробнее о проекте Модернизация системы освещения объектов железнодорожного транспорта Казахстана Казахстан, г. Астана На ж/д путях установлены 12 000 уличных светодиодных светильников MAG2-300-460 В результате потребление электроэнергии сократилось более чем на 60% На ж/д путях установлены 6 500 светильников серий MAG3 и MAG4 различной мощности Работы были выполнены в рекордные сроки - 6 месяцев Подробнее о проекте Экраны на Государственной Третьяковской галерее Россия, г. Москва На здании Централь��ого дома художника были установлены медиа-фасады, демонстрирующие выставку шедевров живописи XX века Медиа-фасады выполнены на основе светодиодных модулей MLO-Beta 1600х1600 Шаг пикселя 40/20 мм, размер экранного поля 12х16 м Объемные световые буквы «Третьяковская галерея. Искусство ХХ века», располагающиеся на фасадах галереи в три яруса Подробнее о проекте Все проекты Партнеры Компания Продукция Решения Проекты Поддержка Купить Партнерам Контакты 105484, г. Москва, 16-я Парковая ул., д. 26, к. 1 Найти © 2010 — 2018 Группа компаний ИНКОТЕКС Сайт работает в тестовом режиме,";
		text="His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.";
		//System.out.println(text.substring(0,100));
		System.out.println(subStrAndTranslate(text,2000).toString());//15360
		System.out.println(subStrByteAndTranslate(text,5500).toString());//15360
//		System.out.println(text.length());
//		System.out.println(text.getBytes().length);
		
//		Translated td = ts.translate("커피, 생리대, 멘소래담, 에어파스, 압박붕대 등이다. 담요 대여 역시 위생", "en", "auto");
//		System.out.println(td);
//		Detected dd = ts.detect(text);
//		System.out.println(dd);
	}

	public static TransResult subStrAndTranslate(String text, int cutlen) {
		TransResult tr = new TransResult();
		if(!text.isEmpty()) {
			Translator ts = new Translator();
			text = text.trim();
			int textLength = text.length();
//			int textByteLength = text.getBytes().length;
//			tr.setByteLength(textByteLength);
			//System.out.println("textLength:"+textLength);
			if(textLength <= cutlen) {
				Translated td = ts.translate(text, "en", "auto");
				tr.setTransText(td.getText());
				tr.setLangCd(td.getSrc());
			}else {
				StringBuffer sbResult =  new StringBuffer();
				do {
//					StringBuffer sbStr1 = new StringBuffer(cutlen);
//					StringBuffer sbStr2 = new StringBuffer();
					
					String str1 = "";
					String str2 = "";
					
					str1 = text.substring(0, cutlen);
					text = text.substring(cutlen);
											
					//System.out.println("str1:"+str1 + " / " + str1.length());
					//System.out.println("text:"+text + " / " + text.length());
					Translated td = ts.translate(str1, "en", "auto");
					sbResult.append(td.getText()+" ");
					tr.setLangCd(td.getSrc());
								
					//System.out.println("sbStr1:"+sbStr1.toString() +"  " +sbStr1.toString().getBytes().length);
					//System.out.println("sbStr2:"+sbStr2.toString() +"  " +sbStr2.toString().getBytes().length);
					
					
					//System.out.println(text.getBytes().length);
					if(text.getBytes().length<cutlen) {
						Translated td2 = ts.translate(text.toString(), "en", "auto");
						sbResult.append(td2.getText()+" ");
						//System.out.println(sbStr2.toString() + "를 번역해서 추가");
					}
				}while(text.length()>cutlen);
				tr.setTransText(sbResult.toString());
				
				
			}
		}
		return tr;
	}
	
	public static TransResult subStrByteAndTranslate(String text, int cutlen) {
		TransResult tr = new TransResult();
		if(!text.isEmpty()) {
			Translator ts = new Translator();
			text = text.trim();
			int textByteLength = text.getBytes().length;
			tr.setByteLength(textByteLength);
			//System.out.println("textByteLength:"+textByteLength);
			if(textByteLength <= cutlen) {
				Translated td = ts.translate(text, "en", "auto");
				tr.setTransText(td.getText());
				tr.setLangCd(td.getSrc());
			}else {
				StringBuffer sbResult =  new StringBuffer();
				do {
					StringBuffer sbStr1 = new StringBuffer(cutlen);
					StringBuffer sbStr2 = new StringBuffer();
					int nCnt =0;
					for(char ch :text.toCharArray()) {
						nCnt += String.valueOf(ch).getBytes().length;
						if(nCnt <= cutlen) {
							sbStr1.append(ch);
							//System.out.println("sbStr1:"+sbStr1.toString().getBytes().length);
						}else {
							sbStr2.append(ch);
							//System.out.println("sbStr2:"+sbStr2.toString().getBytes().length);
						}
						
					}
					//System.out.println("sbStr1:"+sbStr1.toString().getBytes().length);
					Translated td = ts.translate(sbStr1.toString(), "en", "auto");
					sbResult.append(td.getText()+" ");
					tr.setLangCd(td.getSrc());
								
					//System.out.println("sbStr1:"+sbStr1.toString() +"  " +sbStr1.toString().getBytes().length);
					//System.out.println("sbStr2:"+sbStr2.toString() +"  " +sbStr2.toString().getBytes().length);
					
					text = sbStr2.toString();
					//System.out.println(text.getBytes().length);
					if(text.getBytes().length<cutlen) {
						Translated td2 = ts.translate(sbStr2.toString(), "en", "auto");
						sbResult.append(td2.getText()+" ");
						//System.out.println(sbStr2.toString() + "를 번역해서 추가");
					}
				}while(text.getBytes().length>cutlen);
				tr.setTransText(sbResult.toString());
				
				
			}
		}
		return tr;
	}
}
