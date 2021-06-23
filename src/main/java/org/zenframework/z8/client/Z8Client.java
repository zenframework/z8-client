package org.zenframework.z8.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;

import org.zenframework.z8.client.json.JsonObject;
import org.zenframework.z8.client.json.JsonTokener;
import org.zenframework.z8.client.util.MD5;

public class Z8Client {

	private static final String REQUEST = "request.json";

	private static final Properties ALIASES = new Properties();

	static {
		try {
			ALIASES.load(Z8Client.class.getClassLoader().getResourceAsStream("META-INF/z8-client/aliases"));
		} catch (IOException e) {
			System.out.println("Could not load aliases: " + e.getMessage());
		}
	}

	private final URL url;

	private String session;

	public Z8Client(String url) throws IOException {
		this.url = new URL(url + (url.endsWith("/") ? "" : "/") + REQUEST);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.out.println("Usage: java -jar z8-client-1.0.jar <url> <action> <request>");
			System.out.println("Actions:");
			System.out.println("\t- job: run job");
			System.out.println("Known requests:");
			for (Object key : ALIASES.keySet())
				System.out.println("\t- " + key + ": " + ALIASES.get(key));
			System.out.println("Example: java -jar z8-client-1.0.jar http://Admin:pwd@localhost:9080/ job gen");
			System.exit(1);
		}

		String url = args[0];
		String action = args[1];
		String request = args[2];

		Z8Client client = new Z8Client(url);

		client.auth();

		if ("job".equals(action))
			client.job(unalias(request));
		else
			throw new RuntimeException("Unknown action: " + action);
	}

	public void job(String request) throws IOException {
		System.out.println("Job: " + request);

		Map<String, String> params = getParameters(request);
		JsonObject response = request(params);

		params.put("server", response.getString("server"));
		params.put("job", response.getString("id"));

		while (!response.getBoolean("done")) {
			response = request(params);

			if (!response.getBoolean("success"))
				throw new RuntimeException("Job failed: " + response);

			System.out.println("Progress: " + response.getInt("worked") + " / " + response.getInt("total"));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}

		System.out.println("Job done");
	}

	public void auth() throws IOException {
		// Запрос авторизации
		String[] user = getUser(url.getUserInfo());
		Map<String, String> params = new HashMap<String, String>();
		params.put("request", "login");
		params.put("login", user[0]);
		params.put("password", MD5.hex(user[1]));
		JsonObject response = request(params);
		session = response.getString("session");
		if (session == null)
			throw new RuntimeException("Authentication error: " + response);
		System.out.println("Session: " + session);
	}

	// Метод отправляет запрос и печатает ответ в указанный PrintStream
	public JsonObject request(Map<String, String> params) throws IOException {
		// Подготовка соединения
		URLConnection con = url.openConnection();
		HttpURLConnection http = (HttpURLConnection) con;
		http.setRequestMethod("POST");
		http.setDoOutput(true);

		// Формирование и отправка запроса
		StringJoiner req = new StringJoiner("&");
		for (Map.Entry<String, String> entry : params.entrySet())
			req.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
		byte[] buf = req.toString().getBytes(StandardCharsets.UTF_8);
		//http.setInstanceFollowRedirects(false);
		http.setFixedLengthStreamingMode(buf.length);
		http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		http.connect();
		OutputStream os = http.getOutputStream();
		try {
			os.write(buf);
		} finally {
			os.close();
		}

		// Получение ответа
		InputStream in = http.getInputStream();
		try {
			return new JsonObject(new JsonTokener(in));
		} finally {
			in.close();
		}
	}

	private Map<String, String> getParameters(String request) throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("request", request);
		params.put("session", session);
		return params;
	}

	private static String unalias(String alias) {
		String result = ALIASES.getProperty(alias);
		return result != null ? result : alias;
	}

	private static String[] getUser(String userInfo) {
		int pos = userInfo.indexOf(':');
		return new String[] { pos >= 0 ? userInfo.substring(0, pos) : userInfo, pos >= 0 ? userInfo.substring(pos + 1) : "" };
	}

}