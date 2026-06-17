package com.donald.weather_mcp_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WeatherMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeatherMcpServerApplication.class, args);
	}

	// Tools are declared with @McpTool on WeatherTools and auto-registered by the
	// MCP server annotation scanner (enabled by default). No ToolCallbackProvider needed.

}
