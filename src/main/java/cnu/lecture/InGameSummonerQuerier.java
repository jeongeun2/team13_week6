package cnu.lecture;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by tchi on 2016. 4. 25..
 */
public class InGameSummonerQuerier {
	private final String apiKey;
	private final GameParticipantListener listener;

	public InGameSummonerQuerier(String apiKey, GameParticipantListener listener) {
		this.apiKey = apiKey;
		this.listener = listener;
	}

	public String queryGameKey(String summonerName) throws IOException {

		HttpClient client = HttpClientBuilder.create().build();

		String summonerId = getSummonerId(summonerName, client);

		InGameInfo gameInfo = getInGameInfo(client, summonerId);

		getInGameParticipants(gameInfo);

		return gameInfo.getObservers().getEncryptionKey();
	}

	protected String getSummonerId(String summonerName, HttpClient client)
			throws UnsupportedEncodingException, IOException, ClientProtocolException {

		HttpUriRequest summonerRequest = buildApiHttpRequest(summonerName);
		HttpResponse summonerResponse = client.execute(summonerRequest);

		HashMap<String, SummonerInfo> entries = new Gson().fromJson(
				new JsonReader(new InputStreamReader(summonerResponse.getEntity().getContent())),
				new TypeToken<HashMap<String, SummonerInfo>>() {
				}.getType());

		String summonerId = entries.get(summonerName).getId();
		return summonerId;
	}

	protected InGameInfo getInGameInfo(HttpClient client, String summonerId)
			throws IOException, ClientProtocolException {
		HttpUriRequest inGameRequest = buildObserverHttpRequest(summonerId);
		HttpResponse inGameResponse = client.execute(inGameRequest);

		InGameInfo gameInfo = new Gson().fromJson(
				new JsonReader(new InputStreamReader(inGameResponse.getEntity().getContent())), InGameInfo.class);
		return gameInfo;
	}

	protected void getInGameParticipants(InGameInfo gameInfo) {
		Arrays.asList(gameInfo.getParticipants()).forEach((InGameInfo.Participant participant) -> {
			listener.player(participant.getSummonerName());
		});
	}

	private HttpUriRequest buildApiHttpRequest(String summonerName) throws UnsupportedEncodingException {
		String url = mergeWithApiKey(
				new StringBuilder().append("https://kr.api.pvp.net/api/lol/kr/v1.4/summoner/by-name/")
						.append(URLEncoder.encode(summonerName, "UTF-8"))).toString();
		return new HttpGet(url);
	}

	private HttpUriRequest buildObserverHttpRequest(String id) {
		String url = mergeWithApiKey(new StringBuilder()
				.append("https://kr.api.pvp.net/observer-mode/rest/consumer/getSpectatorGameInfo/KR/").append(id))
						.toString();
		return new HttpGet(url);
	}

	private StringBuilder mergeWithApiKey(StringBuilder builder) {
		return builder.append("?api_key=").append(apiKey);
	}
}
