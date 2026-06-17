package com.donald.weather_mcp_server.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;


@Component
public class OpenMeteoClient {

	private final RestClient geocoding = RestClient.create("https://geocoding-api.open-meteo.com");
	private final RestClient forecast = RestClient.create("https://api.open-meteo.com");

	public GeoResponse geocode(String name) {
		return geocoding.get()
				.uri(uri -> uri.path("/v1/search")
						.queryParam("name", name)
						.queryParam("count", 1)
						.build())
				.retrieve()
				.body(GeoResponse.class);
	}

	public ForecastResponse forecast(double latitude, double longitude, int days) {
		return forecast.get()
				.uri(uri -> uri.path("/v1/forecast")
						.queryParam("latitude", latitude)
						.queryParam("longitude", longitude)
						.queryParam("current", "temperature_2m,wind_speed_10m,weather_code")
						.queryParam("daily", "temperature_2m_max,temperature_2m_min,weather_code")
						.queryParam("forecast_days", days)
						.queryParam("timezone", "auto")
						.build())
				.retrieve()
				.body(ForecastResponse.class);
	}


	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GeoResponse(List<GeoCity> results) { }

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GeoCity(String name, double latitude, double longitude, String country) { }

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ForecastResponse(Current current, Daily daily) { }

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Current(String time, double temperature_2m, double wind_speed_10m, int weather_code) { }

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Daily(List<String> time,
						List<Double> temperature_2m_max,
						List<Double> temperature_2m_min,
						List<Integer> weather_code) { }
}
