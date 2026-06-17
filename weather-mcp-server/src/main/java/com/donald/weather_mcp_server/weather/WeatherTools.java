package com.donald.weather_mcp_server.weather;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class WeatherTools {

	private final OpenMeteoClient client;

	public WeatherTools(OpenMeteoClient client) {
		this.client = client;
	}

	@McpTool(name = "geocodeCity",
			description = "Look up the latitude, longitude and country for a city name. "
					+ "Call this first to get coordinates before requesting current weather or a forecast.")
	public GeoResult geocodeCity(
			@McpToolParam(description = "City name, e.g. 'Nairobi' or 'Nairobi, Kenya'", required = true) String city) {

		OpenMeteoClient.GeoResponse response = client.geocode(city);
		if (response == null || response.results() == null || response.results().isEmpty()) {
			throw new IllegalStateException("No location found for '" + city + "'.");
		}
		OpenMeteoClient.GeoCity c = response.results().get(0);
		return new GeoResult(c.name(), c.country(), c.latitude(), c.longitude());
	}

	@McpTool(name = "getCurrentWeather",
			description = "Get the CURRENT weather for a location given its latitude and longitude. "
					+ "Obtain coordinates from geocodeCity first.")
	public CurrentWeather getCurrentWeather(
			@McpToolParam(description = "Latitude in decimal degrees", required = true) double latitude,
			@McpToolParam(description = "Longitude in decimal degrees", required = true) double longitude) {

		OpenMeteoClient.ForecastResponse f = client.forecast(latitude, longitude, 1);
		OpenMeteoClient.Current cur = f.current();
		return new CurrentWeather(
				cur.temperature_2m(),
				cur.wind_speed_10m(),
				WmoWeatherCode.describe(cur.weather_code()),
				cur.time());
	}

	@McpTool(name = "getForecast",
			description = "Get a daily weather forecast for a location given its latitude, longitude "
					+ "and the number of days (1-16). Obtain coordinates from geocodeCity first.")
	public ForecastResult getForecast(
			@McpToolParam(description = "Latitude in decimal degrees", required = true) double latitude,
			@McpToolParam(description = "Longitude in decimal degrees", required = true) double longitude,
			@McpToolParam(description = "Number of days to forecast, 1-16", required = true) int days) {

		int clamped = Math.max(1, Math.min(days, 16));
		OpenMeteoClient.ForecastResponse f = client.forecast(latitude, longitude, clamped);
		OpenMeteoClient.Daily d = f.daily();

		List<DayForecast> daily = java.util.stream.IntStream.range(0, d.time().size())
				.mapToObj(i -> new DayForecast(
						d.time().get(i),
						d.temperature_2m_max().get(i),
						d.temperature_2m_min().get(i),
						WmoWeatherCode.describe(d.weather_code().get(i))))
				.toList();

		return new ForecastResult(daily);
	}


	public record GeoResult(String name, String country, double latitude, double longitude) { }

	public record CurrentWeather(double temperatureC, double windSpeedKmh, String conditions, String time) { }

	public record DayForecast(String date, double maxTempC, double minTempC, String conditions) { }

	public record ForecastResult(List<DayForecast> days) { }
}
