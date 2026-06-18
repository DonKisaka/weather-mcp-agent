# Weather MCP Agent

A Spring AI + Claude agent that answers natural-language weather questions by calling tools on its own **Model Context Protocol (MCP) server**, which wraps the free, keyless [Open-Meteo](https://open-meteo.com) API.

This is a learning project demonstrating the MCP **server** and **client** sides of Spring AI, using the **Streamable HTTP** transport.

```
POST /chat "What's the weather in Nairobi tomorrow?"
   -> Claude picks tools
      -> MCP client (HTTP) -> MCP server :8081
         -> geocodeCity("Nairobi") -> getForecast(lat, lon, 2)
            -> Open-Meteo
      <- natural-language answer
```

## Modules

This is a multi-module Maven project.

| Module              | Port | Role                                                                 |
|---------------------|------|----------------------------------------------------------------------|
| `weather-mcp-server`| 8081 | Streamable HTTP MCP server exposing 3 weather tools over Open-Meteo. |
| `weather-agent`     | 8080 | MCP client + Claude `ChatClient`, exposes a `POST /chat` REST API.   |

### MCP tools (`weather-mcp-server`)

Declared with `@McpTool` / `@McpToolParam` and auto-registered by the MCP server annotation scanner.

| Tool                | Arguments                          | Returns                                  |
|---------------------|------------------------------------|------------------------------------------|
| `geocodeCity`       | `city`                             | name, country, latitude, longitude       |
| `getCurrentWeather` | `latitude`, `longitude`            | temperature, wind, conditions (now)      |
| `getForecast`       | `latitude`, `longitude`, `days`    | daily high/low + conditions for N days   |

`getCurrentWeather` and `getForecast` take coordinates, so the agent first calls
`geocodeCity` to turn a city name into latitude/longitude — exercising multi-tool chaining.

## Tech stack

- Java 25
- Spring Boot 4.1.0
- Spring AI 2.0.0 (`spring-ai-starter-mcp-server-webmvc`, `spring-ai-starter-mcp-client`, `spring-ai-starter-model-anthropic`)
- Anthropic Claude Haiku (`claude-haiku-4-5-20251001`)
- Open-Meteo (no API key required)

## Prerequisites

- JDK 25
- An Anthropic API key

Set the key as an environment variable (read by the agent's `application.properties`):

```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

## Running

Start the MCP server **first** (the agent discovers its tools at startup), then the agent.

```bash
# Terminal 1 - MCP server on :8081
./mvnw -pl weather-mcp-server spring-boot:run

# Terminal 2 - agent on :8080
./mvnw -pl weather-agent spring-boot:run
```

## Usage

`POST /chat` with a JSON body:

```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"What is the weather in Nairobi tomorrow?"}'
```

Response:

```json
{ "answer": "Tomorrow in Nairobi: around 24°C, partly cloudy ..." }
```

### Conversation memory

The agent keeps a sliding window of the last 500 messages per conversation.
Pass a `conversationId` to keep separate threads; reuse it for follow-up questions.

```json
{ "question": "What's the forecast for Nakuru this week?", "conversationId": "donald-1" }
```
```json
{ "question": "And how about the weekend specifically?", "conversationId": "donald-1" }
```

If `conversationId` is omitted, all requests share a single `"default"` thread.

### Example questions

- `What's the weather like in Nairobi right now?`
- `Give me the 5-day forecast for Mombasa.`
- `Is it warmer in Nairobi or Kisumu today?`
- `Compare today's weather in London, Tokyo, and New York.`
- `I'm flying to Cape Town tomorrow — should I pack a jacket?`

## Configuration

### `weather-mcp-server/src/main/resources/application.properties`

```properties
server.port=8081
spring.ai.mcp.server.name=weather-mcp-server
spring.ai.mcp.server.version=0.0.1
spring.ai.mcp.server.protocol=STREAMABLE
```

### `weather-agent/src/main/resources/application.properties`

```properties
server.port=8080
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.model=claude-haiku-4-5-20251001
spring.ai.mcp.client.streamable-http.connections.weather.url=http://localhost:8081
spring.ai.mcp.client.toolcallback.enabled=true
```

## Notes

- **No `ToolCallbackProvider` bean on the server** — `@McpTool`-annotated beans are
  picked up automatically by the MCP server annotation scanner (enabled by default).
- **Conversation id is required by the memory advisor.** In Spring AI 2.0,
  `MessageChatMemoryAdvisor` does not default it; every `ChatClient` call passes
  `ChatMemory.CONVERSATION_ID` in the advisor params.
