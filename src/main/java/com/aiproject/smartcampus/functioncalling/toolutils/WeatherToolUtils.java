package com.aiproject.smartcampus.functioncalling.toolutils;

import com.aiproject.smartcampus.pojo.bo.WeatherInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 天气枚举及查询实现
 */
@Getter
@RequiredArgsConstructor
public enum WeatherToolUtils {
    SUNNY("晴"), CLOUDY("多云"), RAINY("雨"), SNOWY("雪"), FOGGY("雾"), WINDY("风"), STORM("雷暴"), UNKNOWN("未知");

    private final String description;

    private static final Logger log = LoggerFactory.getLogger(WeatherToolUtils.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TIMEZONE = "Asia/Shanghai";
    private static final String TEMP_UNIT = "celsius";
    private static final String WIND_UNIT = "kmh";

    /**
     * @program: lecture-langchain-20250525
     * @description: 获取当前天气工具类
     * @author: lk
     * @create: 2025-05-16 16:15
     **/

    public static WeatherInfo getWeatherInfo(String city) throws Exception {
        // 1. 地理编码：获取经纬度
        String geoUrl = String.format("https://geocoding-api.open-meteo.com/v1/search?" + "name=%s&count=1&language=zh", URLEncoder.encode(city, StandardCharsets.UTF_8));
        HttpRequest geoReq = HttpRequest.newBuilder().uri(URI.create(geoUrl)).timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<String> geoResp = CLIENT.send(geoReq, HttpResponse.BodyHandlers.ofString());
        if (geoResp.statusCode() != 200) {
            throw new RuntimeException("地理编码失败，状态码：" + geoResp.statusCode());
        }
        JsonNode geoJson = MAPPER.readTree(geoResp.body());
        JsonNode results = geoJson.path("results");
        if (!results.isArray() || results.isEmpty()) {
            throw new RuntimeException("未找到城市：" + city);
        }
        JsonNode loc = results.get(0);
        double lat = loc.path("latitude").asDouble();
        double lon = loc.path("longitude").asDouble();
        log.info("解析到城市 {} 的坐标：lat={}, lon={}", city, lat, lon);
        // 2. 天气查询
        String weatherUrl = String.format("https://api.open-meteo.com/v1/forecast?" + "latitude=%f&longitude=%f&current_weather=true" + "&daily=temperature_2m_min,temperature_2m_max" + "&temperature_unit=%s&wind_speed_unit=%s&timezone=%s", lat, lon, TEMP_UNIT, WIND_UNIT, TIMEZONE);
        HttpRequest weatherReq = HttpRequest.newBuilder().uri(URI.create(weatherUrl)).timeout(Duration.ofSeconds(5)).GET().build();
        HttpResponse<String> weatherResp = CLIENT.send(weatherReq, HttpResponse.BodyHandlers.ofString());
        if (weatherResp.statusCode() != 200) {
            throw new RuntimeException("天气查询失败，状态码：" + weatherResp.statusCode());
        }
        JsonNode root = MAPPER.readTree(weatherResp.body());
        JsonNode current = root.path("current_weather");
        JsonNode daily = root.path("daily");
        // 3.解析并封装
        int code = current.path("weathercode").asInt();
        WeatherToolUtils cond = mapWeatherCode(code);
        int minT = daily.path("temperature_2m_min").get(0).asInt();
        int maxT = daily.path("temperature_2m_max").get(0).asInt();
        double windSpeed = current.path("windspeed").asDouble();
        String windDesc = String.format("微风，约%s级", (int) Math.round(windSpeed));

        return new WeatherInfo(cond, minT, maxT, windDesc);
    }
    // WMO weathercode 映射
    private static WeatherToolUtils mapWeatherCode(int code) {
        if (code == 0) {
            return SUNNY;
        }
        if (code <= 3) {
            return CLOUDY;
        }
        if (code == 45 || code == 48) {
            return FOGGY;
        }
        if ((code >= 51 && code <= 57) || (code >= 61 && code <= 67) || (code >= 80 && code <= 82)) {
            return RAINY;
        }
        if ((code >= 71 && code <= 75) || code == 77 || code == 85 || code == 86) {
            return SNOWY;
        }
        if (code == 95 || code == 96 || code == 99) {
            return STORM;
        }
        return UNKNOWN;
    }
}
