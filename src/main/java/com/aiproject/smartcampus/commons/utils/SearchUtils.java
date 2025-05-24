package com.aiproject.smartcampus.commons.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * @program: SmartCampus
 * @description: 搜索工具类
 * @author: lk
 * @create: 2025-05-24 18:42
 **/

public class SearchUtils {

    // 网页抓取工具方法（使用 Jsoup）
    public static String fetchWebPageContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(30_000) // 10秒超时
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36") // 模拟浏览器
                    .get();

            // 根据常见网页结构提取正文（可针对目标网站优化）
            Elements contentElements = doc.select("div.article-content, article, p, div.content");
            return contentElements.text().replaceAll("\\s+", " "); // 合并空格

        } catch (IOException e) {
            throw new RuntimeException("网页抓取失败: " + url, e);
        }
    }

}
