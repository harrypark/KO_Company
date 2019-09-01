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

import org.apache.commons.text.StringEscapeUtils;

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
		List<String[]> strList = null;
		try {

			strList = Utils.formatJson(result);
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		//System.out.println("strList:"+strList);
		return strList;
		//return Utils.formatJson(result);
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

        if(data == null) {
        	return null;
        }

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
		//text="His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.His political future could rest on whether a sufficient number of Americans decide that the Trumpian tumult of his first term expresses their frustration with the Washington establishment and has delivered the wealth, peace and patriotic pride that he promised in the 2016 campaign.";
		//text="제품 NordLock 오리지널 와셔 X시리즈 와셔 스틸 건축용 와셔 휠 너트 콤비 볼트 와셔 맞춤형 솔루션 빠른 링크 기술 품질 CAD Downloads 토크 지침 다운로드 NordLock Elearning NordLock Shop Superbolt 텐셔너 플렉스너트 맞춤형 솔루션 EzFit HyFit Superbolt Tool 빠른 링크 기술 품질 다운로드 Expander System 너트 및 와셔 와셔와 볼트 화스너 오목 와셔 포함 볼트 화스너 관통 볼트 디자인 납작머리 화스너 멀티볼트 정렬 Stepped Pin 해양 핀 맞춤형 솔루션 빠른 링크 기술 First Fit Aftermarket 다운로드 Expander Web Shop Boltight 표준 볼트 텐셔너 심해 텐셔너 타이푼 텐셔너 Echometer 및 Echometer+ TSR 텐셔너 Xtra 범위 기초 텐셔너 유압 너트 클로저 시스템, 로터 및 케이싱 공구 액세서리 엔진 공구 세트 맞춤형 솔루션 빠른 링크 산업 기술 품질 다운로드 산업 건설 및 교량 공사 임업 및 농업 기계 제조 제조 및 공정 광산 및 채석 정유 산업 발전 철도 산업 조선 & 해양 철강, 단조 및 프레스 장비 운송 기타 산업 서비스 자료 다운로드 컨설팅 서비스 기술 지원 디지털 서비스 BOLTED 블로그 회사 소개 노드락그룹 평생 보증 연혁 채용 연락처 Expander Web Shop 0 Expander Web Shop Secure Bolting Solutions 완벽한 볼트 체결로 더욱 안전한 세상 만들기 Here is our story 도움이 필요하세요? Download Center BOLTED 블로그 Technical Support Lifetime Warranty 브랜드별 솔루션 살펴보기 NordLock 쐐기형 풀림 방지 기술은 심한 진동과 동적 부하에 노출된 볼트 체결부를 안정하게 고정합니다. Superbolt 기술을 사용하면 안전하지 않고 시간이 많이 소요되는 체결 방법이 사라집니다. Expander System은 러그 마모에 대한 간편하고 경제적인 솔루션을 제공합니다. Boltight 텐셔닝 공구는 여러 볼트를 동시에 조이는 빠르고 정확하고 안전한 공구입니다. 전 세계 산업에 제품 공급 NordLock Group은 모든 산업에서 복잡한 볼트 체결 과제를 해결하는 데 필요한 기술을 갖추고 있습니다. 또한 수십 년 동안 전 세계 여러 산업의 과제를 성공적으로 해결한 경험이 있습니다. 아래에서 관심있는 산업을 선택한 후 솔루션에 대해 자세히 알아보세요. 건설 및 교량 공사 건설 및 교량 공사 완벽한 체결 솔루션을 통해 건물과 교량 구조물 결합부의 안전을 보장합니다. 자세히 보기 임업 및 농업 임업 및 농업 영구적인 러그 마모 방지 솔루션을 통해 작업 중단 시간을 줄이며, 농업과 임업 생산성을 꾸준히 유지시켜 줍니다. 자세히 보기 기계 제조 기계 제조 볼트 체결부는 기계의 내구성만큼 강해야하며 더 가혹한 조건속에서도 높은 체결력을 유지해야 합니다. 자세히 보기 제조 및 공정 제조 및 공정 흔들리지 않는 볼트 장착으로 생산성을 극대화하며 작업 중지 시간 최소화합니다. 자세히 보기 광산 및 채석 광산 및 채석 중장비 사용의 까다로운 조건을 견딜 수 있는 견고한 기술이 있습니다. 자세히 보기 정유 산업 정유 산업 설치 및 교체가 신속한 체결 솔루션으로 가장 까다로운 조건에서도 안전을 보장합니다. 자세히 보기 발전 발전 모든 발전산업에서 안전한 볼트 체결은 필수적인 요소입니다. 자세히 보기 철도 산업 철도 산업 빠른 속도에서도 볼트 체결부는 안전하게 보호되어야 합니다. 자세히 보기 조선 & 해양 조선 & 해양 대형 화물선 및 탱커를 지지하는 작고 견고한 제품이 있습니다. 자세히 보기 철강, 단조 및 프레스 장비 철강, 단조 및 프레스 장비 산업의 엄격한 요구 사항을 준수하기 위해 특별 설계된 제품을 공급합니다. 자세히 보기 운송 운송 우주 위성에서 차량용 휠 잠금 솔루션까지 모든 운송 수단을 위한 체결 솔루션을 제공합니다. 자세히 보기 기타 산업 기타 산업 모든 산업에서 볼트 체결 문제 해결을 위한 솔루션 자세히 보기 건설 및 교량 공사 임업 및 농업 기계 제조 제조 및 공정 광산 및 채석 정유 산업 발전 철도 산업 조선 & 해양 철강, 단조 및 프레스 장비 운송 기타 산업 미래를 위한 통찰력 History of the Bolt Bolts are one of the most common elements used in construction and machine design. Yet, have you ever stopped to wonder where they actually came from? 자세히 보기 Introducing Torquelator by NordLock Torquelator by NordLock calculates preload and corresponding torque for bolted joints secured with NordLock washers. 자세히 보기 Bespoke bolt tensioners secure deepwater pipelines Boltight supplies tailormade bolt tensioning equipment for structural split repair clamps to Subsea Innovation in the UK. 자세히 보기 New horizons with the Expander System Boskalis has chosen Expander System as their supplier of pivot pins for one of their biggest dredgers – backhoe dredger Baldur. 자세히 보기 Fusion Power Swiss Plasma Center's problems with vertical, horizontal and axial vibrations are solved with the installation of Superbolt expansion bolts. 자세히 보기 Top 10 tips for secure bolting Achieving the safest, most secure bolted connection involves many factors. To help you achieve the best possible results we have summarized our top tips. 자세히 보기 The Challenge: Andritz Hydro AB helping hydropower look to the future The hydropower plant in Laxede on the river Luleälven was built more than 50 years ago. The turbines in two of the three units are now being refurbished and, for the first time, the runners will be replaced – a challenge taken on by Andritz Hydro AB. 자세히 보기 On site, offshore, under pressure Dutch company Heat Solutions on Site BV (HSOS) is a leading service provider in the metal sector. Their services include heat treatment and machining on site. 자세히 보기 볼트 산업에 대한 최신 동향 알기 뉴스레터에 등록하면 이메일을 통해 산업 뉴스, 기술 동향 및 볼트 관련 정보를 직접 받아볼 수 있습니다! 지금 등록하기 국가 Argentina Australia Austria Bahrain Belarus Belgium Belize Bolivia Bosnia and Herzegovina Brazil Brunei Bulgaria Canada Chile China Colombia Costa Rica Croatia Cyprus Czech Republic Denmark Dominican Republic Ecuador Egypt El Salvador Finland France Germany Greece Guatemala Honduras Hong Kong Hungary India Indonesia Iran Ireland Israel Italy Japan Kazakhstan Luxembourg Macedonia Malaysia Mexico Montenegro Mozambique Namibia Netherlands New Zealand Nicaragua Nigeria Norway Oman Panama Paraguay Peru Philippines Poland Portugal Qatar Romania Russia Saudi Arabia Serbia Singapore Slovakia Slovenia South Africa South Korea Spain Sweden Switzerland Taiwan Tanzania Thailand Trinidad Turkey Ukraine United Arab Emirates United Kingdom Uruguay United States Venezuela Vietnam Zambia Other 사용자 정보 취급 방식에 대한 자세한 내용은 개인 정보 취급 방침을 읽어보시기 바랍니다. 구독 해 주셔서 감사합니다! Exhibitions As a global company, we attend and exhibit at different events and conferences around the world. Come and meet the NordLock Group at the upcoming exhibitions. Power Gen Asia – 03.09.05.09.2019 Kuala Lumpur, Malaysia 자세히 보기 SPE Offshore Europe – 03.09.06.09.2019 Aberdeen, United Kingdom 자세히 보기 DSEI – 10.09.13.09.2019 Excel London, United Kingdom 자세히 보기 Husum Wind – 10.09.13.09.2019 Hamburg, Germany 자세히 보기 National Industrial Fastener Show (NIFS) – 17.09.  19.09.2019 Las Vegas, USA 자세히 보기 19th Mining and Minerals Exhibition – 18.09.21.09.2019 Jakarta, Indonesia 자세히 보기 Railway Interchange – 22.09.  25.09.2019 Minneapolis, Minnesota, USA 자세히 보기 Kormarine Expo – 22.10.25.10.2019 Busan, Korea 자세히 보기 PowerGen International – 19.11.  21.11.2019 New Orleans, USA 자세히 보기 Elmia Subcontractor – 12.11.15.11.2019 Jönköping, Sweden 자세히 보기 Mass Trans Innovation Japan – 27.11.29.11.2019 Tokyo, Japan 자세히 보기 안정적인 연결 볼트 체결을 위한 적합한 솔루션을 찾고 계십니까? 필요로 하는 제품 그 이상을 찾을 수 있습니다. 기술 지원 인증서, 데이터 시트 또는 매뉴얼을 찾고 계십니까? 지금 다운로드 볼트 체결 관련 문제를 해결할 수 있도록 도움을 드립니다. 연락처 We use cookies to make it easier for you to use our website. They allow us to recognize our registered users, count visitor numbers and find out how they navigate the site; helping us make changes so you can find what you’re looking for faster. Continue NordLock Group은 볼트 체결 솔루션에서 세계적인 선두 기업입니다. 인간의 삶과 고객 투자를 지키는 것이 우리의 사명입니다. 회사 소개 국가 선택 NordLock Korea Co. Ltd. (46721) 부산광역시 강서구 유통단지 1로 50 부산 티플렉스(218동 6,7호) 전화 0517962211 info@nordlock.co.kr   NordLock Group의  전세계 지사 현지 사무소 찾기 NordLock Superbolt Expander System Boltight Download Center Performance Services Technical Support About NordLock Group Contact Us Construction Forestry & Agriculture Machine Building Manufacturing & Processing Mining & Minerals Oil & Gas Power Generation Railway Shipbuilding & Marine Steel, Forging & Other Presses Transportation Miscellaneous © 2019 NordLock International AB Privacy Cookie Policy Legal Disclaimer 닫기 연락하기 국가* Argentina Australia Austria Bahrain Belarus Belgium Belize Bolivia Bosnia and Herzegovina Brazil Brunei Bulgaria Canada Chile China Colombia Costa Rica Croatia Cyprus Czech Republic Denmark Dominican Republic Ecuador Egypt El Salvador Finland France Germany Greece Guatemala Honduras Hong Kong Hungary India Indonesia Iran Ireland Israel Italy Japan Kazakhstan Luxembourg Macedonia Malaysia Mexico Montenegro Mozambique Namibia Netherlands New Zealand Nicaragua Nigeria Norway Oman Panama Paraguay Peru Philippines Poland Portugal Qatar Romania Russia Saudi Arabia Serbia Singapore Slovakia Slovenia South Africa South Korea Spain Sweden Switzerland Taiwan Tanzania Thailand Trinidad Turkey Ukraine United Arab Emirates United Kingdom Uruguay United States Venezuela Vietnam Zambia Other ► 현지 사무소 찾기 사용자 정보 취급 방식에 대한 자세한 내용은 개인 정보 취급 방침을 읽어보시기 바랍니다. 연락해 주셔서 감사합니다! Select your country Brazil / Brasil China / 中国 Czech Republic / Česká republika Denmark / Danmark Finland / Suomi France / Français Germany / Deutschland Italy / Italia Japan / 日本 The Netherlands / Nederland Korea / 대한민국 Norway / Norge Poland / Polska Switzerland Schweiz Suisse Svizzera Sweden / Sverige Spain / España United Kingdom United States Global (all other countries)";
		text="株式会社 エフ・アイ・ティー　&amp;amp;amp;amp;#8722;　厨房機器・食品機械・プロフェッショナルマシーンの専門商社〜世界の一流食品機械を紹介します".replaceAll("amp;","");
		System.out.println(text);
		//System.out.println(subStrAndTranslate(text,2000).toString());//15360
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
			text = StringEscapeUtils.unescapeHtml4(text.trim());
			text = StringEscapeUtils.unescapeHtml4(text).replaceAll("amp;","");
			//System.out.println("Tran"+text);
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
			text = StringEscapeUtils.unescapeHtml4(text.trim());
			text = StringEscapeUtils.unescapeHtml4(text);
			text = StringEscapeUtils.unescapeHtml4(text);
			text = StringEscapeUtils.unescapeHtml4(text).replaceAll("amp;","");
			//System.out.println("Tran"+text);
			int textByteLength = text.getBytes().length;
			tr.setByteLength(textByteLength);
			//System.out.println("textByteLength:"+textByteLength);
			if(textByteLength <= cutlen) {
				Translated td = ts.translate(text, "en", "auto");
				if(td != null) {
					tr.setTransText(td.getText());
					tr.setLangCd(td.getSrc());

				}
			}else {
				StringBuffer sbResult =  new StringBuffer();
				//int i=0;
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
					//System.out.println(i++ +"sbStr1:"+sbStr1.toString());
					Translated td = ts.translate(sbStr1.toString(), "en", "auto");

					sbResult.append(td.getText()+" ");
					tr.setLangCd(td.getSrc());
					//System.out.println(td.getSrc()+"/"+td.getText());

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
				//System.out.println("결과:"+sbResult.toString());


			}
		}
		return tr;
	}
}
