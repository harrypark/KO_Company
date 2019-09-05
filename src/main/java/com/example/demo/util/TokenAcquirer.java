package com.example.demo.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

@Getter
@Setter
@AllArgsConstructor
public class TokenAcquirer {
	private Session session;
	private String tkk = "0";
	private String host = "https://translate.google.com";

	public static Pattern RE_TKK = Pattern.compile("tkk:\\'(.+?)\\'", Pattern.DOTALL);
	public static Pattern RE_RAWTKK = Pattern.compile("tkk:\\'(.+?)\\'", Pattern.DOTALL);

	public TokenAcquirer() {
		this.session = Requests.session();
	}

	public TokenAcquirer(Session session, String host) {
		this.session = session;
		this.host = host.startsWith("http") ? host : "https://" + host;
	}

	public void update() {
		int now = (int) Math.floor(System.currentTimeMillis() / 3600000.0);
		if (tkk != null && Double.valueOf(tkk.split(",")[0]).intValue() == now)
			return;

		RawResponse r = null;
		try {
			r = session.get(host).send();
		}catch (Exception e) {
			System.out.println("TokenAcquirer.update Error");
		}
		String result = r.readToText();
		Matcher m = RE_TKK.matcher(result);
		if (m.find()) {
			tkk = m.group(1);
			return;
		}
	}

	public String acquire(String text) {
		int size = text.length();
		int[] a = new int[size];

		// Convert text to ints
		char[] chs = text.toCharArray();
		for (int i = 0; i < chs.length; i++) {
			int val = (int) chs[i];
			a[i] = val;
		}
		String bs = (!"0".equals(tkk)) ? tkk : "";
		String[] d = bs.split("\\.");
		int b = (d.length > 1) ? Integer.parseInt(d[0]) : 0;
		List<Integer> e = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			int l = a[i];
			if (l < 128)
				e.add(l);
			else {
				if (l < 2048)
					e.add(l >> 6 | 192);
				else {
					if ((l & 64512) == 55296 && i + 1 < size && (a[i + 1] & 64512) == 56320) {
						i += 1;
						l = 65536 + ((l & 1023) << 10) + (a[i] & 1023); // This bracket is important
						e.add(l >> 18 | 240);
						e.add(l >> 12 & 63 | 128);
					} else {
						e.add(l >> 12 | 224);
					}
					e.add(l >> 6 & 63 | 128);
				}
				e.add(l & 63 | 128);
			}
		}
		long na = b;
		for (long value : e) {
			na += value;
			na = _xr(na, "+=a^+6".toCharArray());
		}
		na = _xr(na, "+-3^+b+-f".toCharArray());
		na ^= (d.length > 1) ? Long.parseLong(d[1]) : 0;
		if (na < 0)
			na = (na & 2147483647) + 2147483648l;
		na %= 1000000;
		return String.format("%d.%d", na, na ^ b);
	}

	private long _xr(long a, char[] b) {
		int size_b = b.length;
		int c = 0;
		while (c < (size_b - 2)) {
			char d = b[c + 2];
			long nd = ((int) 'a' <= (int) d) ? (int) d - 87 : Integer.parseInt(String.valueOf(d));
			nd = ('+' == b[c + 1]) ? a >>> nd : (a << nd);
			a = ('+' == b[c]) ? ((a + nd) & 4294967295l) : (a ^ nd);
			c += 3;
		}
		return a;
	}

	public String get(String text) {
		update();
		return acquire(text);
	}

	public static void main(String[] args) {
		TokenAcquirer acq = new TokenAcquirer();
		String result = acq.get("text");
		System.out.println(result);
	}
}
