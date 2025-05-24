package com.aiproject.smartcampus.handler.chathandler.handlerImpl;

import com.aiproject.smartcampus.commons.utils.JsonUtils;
import com.aiproject.smartcampus.commons.utils.PromptUtils;
import com.aiproject.smartcampus.exception.MemoryExpection;
import com.aiproject.smartcampus.exception.RagExpection;
import com.aiproject.smartcampus.handler.chathandler.ChatHandler;
import com.aiproject.smartcampus.pojo.bo.ToolList;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerResponse;
import com.aiproject.smartcampus.pojo.bo.handlerentity.ChatHandlerquery;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.searchapi.SearchApiWebSearchEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.aiproject.smartcampus.commons.utils.PromptUtils.buildSystemPrompt;
import static com.aiproject.smartcampus.commons.utils.SearchUtils.fetchWebPageContent;
import static com.aiproject.smartcampus.contest.CommonContest.TOOL_SCAN_NAME;

@Service
@Slf4j
@RequiredArgsConstructor
public class FunctionCallingHandler extends ChatHandler {

    private final ToolList toolList;
    private final ChatLanguageModel chatLanguageModel;
    private final SearchApiWebSearchEngine searchEngine;

    private final ExecutorService webCrawlExecutor = Executors.newFixedThreadPool(5);

    // 优化配置参数
    private static final int MAX_SEARCH_RESULTS = 8; // 增加搜索结果数量
    private static final int MAX_CONTENT_LENGTH = 2000; // 增加内容长度
    private static final int CRAWL_TIMEOUT_SECONDS = 15; // 增加抓取超时时间
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final double MIN_RELEVANCE_SCORE = 0.6; // 最小相关性分数

    @Override
    public void chatHandle(ChatHandlerquery handlerquery, ChatHandlerResponse handlerResponse) {
        if (!handlerResponse.getIsSuccess()) {
            log.error("FunctionCalling前置步骤失败");
            throw new RagExpection("FunctionCalling前置步骤失败");
        }

        log.info("执行工具调用中...");
        String userMessage = handlerquery.getQueryContent();

        List<ToolSpecification> tools = toolList.getTools();
        ChatResponse chatResponse = chatLanguageModel.doChat(ChatRequest.builder()
                .messages(SystemMessage.systemMessage(buildSystemPrompt()), UserMessage.from(userMessage))
                .parameters(ChatRequestParameters.builder()
                        .toolSpecifications(tools)
                        .build())
                .build()
        );

        AtomicReference<String> toolResultRef = new AtomicReference<>("");
        List<ToolExecutionRequest> toolRequests = chatResponse.aiMessage().toolExecutionRequests();

        if (toolRequests != null && !toolRequests.isEmpty()) {
            for (ToolExecutionRequest req : toolRequests) {
                try {
                    if ("SearchEngine".equalsIgnoreCase(req.name())) {
                        String searchResult = executeOptimizedSearchEngine(req, userMessage);
                        toolResultRef.set(searchResult);
                    } else {
                        String customToolResult = executeCustomTool(req);
                        toolResultRef.set(customToolResult);
                    }
                } catch (Exception e) {
                    log.error("调用工具 {} 失败", req.name(), e);
                    throw new MemoryExpection("工具调用失败: " + e.getMessage());
                }
            }
        } else {
            log.warn("未触发任何工具调用");
        }

        log.info("工具调用结束");
        processFinalResult(handlerquery, handlerResponse, toolResultRef.get());
    }

    /**
     * 优化的搜索引擎执行方法
     */
    private String executeOptimizedSearchEngine(ToolExecutionRequest req, String userMessage) {
        log.info("执行优化联网搜索中...");
        try {
            JsonObject arguments = JsonParser.parseString(req.arguments()).getAsJsonObject();
            String rawQuery = arguments.get("query").getAsString();
            // 1. 多层次查询优化
            List<String> optimizedQueries = generateMultipleQueries(rawQuery, userMessage);
            // 2. 执行多次搜索并合并结果
            Set<WebSearchOrganicResult> allResults = new LinkedHashSet<>();
            for (String query : optimizedQueries) {
                try {
                    WebSearchResults searchResults = searchEngine.search(query);
                    allResults.addAll(searchResults.results());
                    // 避免请求过于频繁
                    Thread.sleep(200);
                } catch (Exception e) {
                    log.warn("查询 {} 失败: {}", query, e.getMessage());
                }
            }
            if (allResults.isEmpty()) {
                return "搜索未找到相关结果，请尝试调整搜索关键词";
            }
            // 3. 智能筛选和排序结果
            List<WebSearchOrganicResult> filteredResults = filterAndRankResults(
                    new ArrayList<>(allResults), userMessage, rawQuery);
            // 4. 并发抓取并分析内容
            return crawlAndAnalyzeContent(filteredResults, rawQuery, userMessage);
        } catch (Exception e) {
            log.error("联网搜索失败", e);
            return "搜索过程中发生错误：" + e.getMessage() + "，请稍后重试";
        }
    }

    /**
     * 生成多个优化查询
     */
    private List<String> generateMultipleQueries(String rawQuery, String userMessage) {
        String queryOptimizePrompt = String.format("""
                根据用户问题和原始查询，生成3-5个不同角度的搜索查询，提高搜索精确度：
              
                用户问题：%s
                原始查询：%s
                
                要求：
                1. 第一个查询应该是最直接的关键词组合
                2. 第二个查询应该包含更具体的术语
                3. 第三个查询应该考虑相关概念和同义词
                4. 如果是技术问题，包含专业术语
                5. 如果是时效性问题，加上时间限定词
                
                请返回JSON格式：{"queries": ["查询1", "查询2", "查询3"]}
                """, userMessage, rawQuery);
        try {
            String response = chatLanguageModel.chat(queryOptimizePrompt).trim();
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            List<String> queries = new ArrayList<>();
            json.getAsJsonArray("queries").forEach(element ->
                    queries.add(element.getAsString()));
            // 确保包含原始查询
            if (!queries.contains(rawQuery)) {
                queries.add(0, rawQuery);
            }
            log.info("生成的查询列表: {}", queries);
            return queries;

        } catch (Exception e) {
            log.warn("生成多查询失败，使用原始查询: {}", rawQuery);
            return Arrays.asList(rawQuery);
        }
    }

    /**
     * 智能筛选和排序搜索结果
     */
    private List<WebSearchOrganicResult> filterAndRankResults(
            List<WebSearchOrganicResult> results, String userMessage, String query) {

        // 计算每个结果的相关性分数
        List<ScoredResult> scoredResults = results.stream()
                .map(result -> new ScoredResult(result, calculateRelevanceScore(result, userMessage, query)))
                .filter(scored -> scored.score > MIN_RELEVANCE_SCORE)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(MAX_SEARCH_RESULTS)
                .collect(Collectors.toList());

        log.info("筛选后的结果数量: {}", scoredResults.size());

        return scoredResults.stream()
                .map(scored -> scored.result)
                .collect(Collectors.toList());
    }

    /**
     * 计算搜索结果相关性分数
     */
    private double calculateRelevanceScore(WebSearchOrganicResult result, String userMessage, String query) {
        double score = 0.0;

        String title = result.title() != null ? result.title().toLowerCase() : "";
        String content = result.content() != null ? result.content().toLowerCase() : "";
        String url = result.url() != null ? result.url().toString().toLowerCase() : "";

        String[] queryTerms = query.toLowerCase().split("\\s+");
        String[] userTerms = userMessage.toLowerCase().split("\\s+");

        // 标题匹配权重最高
        for (String term : queryTerms) {
            if (title.contains(term)) score += 0.4;
            if (content.contains(term)) score += 0.2;
        }

        // 用户问题相关性
        for (String term : userTerms) {
            if (title.contains(term)) score += 0.3;
            if (content.contains(term)) score += 0.1;
        }

        // URL权威性加分
        if (url.contains("gov") || url.contains("edu") || url.contains("org")) {
            score += 0.2;
        }

        // 内容长度合理性
        if (content.length() > 100 && content.length() < 500) {
            score += 0.1;
        }

        return Math.min(score, 1.0);
    }

    /**
     * 优化的内容抓取和分析
     */
    private String crawlAndAnalyzeContent(List<WebSearchOrganicResult> results, String query, String userMessage) {
        List<CompletableFuture<EnhancedWebPageInfo>> futures = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            final int index = i;
            WebSearchOrganicResult result = results.get(i);

            CompletableFuture<EnhancedWebPageInfo> future = CompletableFuture.supplyAsync(() -> {
                try {
                    String url = result.url().toString();
                    String pageContent = fetchWebPageContent(url);

                    if (pageContent != null && !pageContent.trim().isEmpty()) {
                        String cleanContent = cleanHtmlContent(pageContent);
                        String summary = generateEnhancedSummary(cleanContent, query, userMessage);
                        double contentRelevance = evaluateContentRelevance(cleanContent, userMessage);
                        return new EnhancedWebPageInfo(
                                index + 1, result.title(), result.content(), url,
                                summary, true, contentRelevance);
                    }
                } catch (Exception e) {
                    log.warn("抓取链接失败: {}, 错误: {}", result.url(), e.getMessage());
                }
                return new EnhancedWebPageInfo(
                        index + 1, result.title(), result.content(),
                        result.url().toString(), "", false, 0.0);
            }, webCrawlExecutor);

            futures.add(future);
        }

        // 收集和整理结果
        List<EnhancedWebPageInfo> webPageInfos = new ArrayList<>();
        for (CompletableFuture<EnhancedWebPageInfo> future : futures) {
            try {
                EnhancedWebPageInfo info = future.get(CRAWL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                webPageInfos.add(info);
            } catch (Exception e) {
                log.warn("获取网页信息超时或失败: {}", e.getMessage());
            }
        }

        // 按相关性重新排序
        webPageInfos.sort((a, b) -> Double.compare(b.relevanceScore, a.relevanceScore));

        return formatSearchResults(webPageInfos, query, userMessage);
    }

    /**
     * 生成增强的内容摘要
     */
    private String generateEnhancedSummary(String content, String query, String userMessage) {
        if (content == null || content.trim().isEmpty()) {
            return "无法获取网页内容";
        }

        try {
            String summaryPrompt = String.format("""
                    作为专业的信息提取专家，请根据以下内容回答用户问题：
                    
                    用户问题：%s
                    搜索查询：%s
                    网页内容：%s
                    
                    要求：
                    1. 直接回答用户问题的核心要点
                    2. 提取最相关和最准确的信息
                    3. 如果内容不能完全回答问题，明确说明
                    4. 保持简洁但信息完整（300字内）
                    5. 突出重要的数据、时间、地点等关键信息
                    
                    摘要：
                    """, userMessage, query, content);

            return chatLanguageModel.chat(summaryPrompt).trim();
        } catch (Exception e) {
            log.warn("生成增强摘要失败: {}", e.getMessage());
            return content.length() > 300 ? content.substring(0, 300) + "..." : content;
        }
    }

    /**
     * 评估内容相关性
     */
    private double evaluateContentRelevance(String content, String userMessage) {
        try {
            String relevancePrompt = String.format("""
                    请评估以下网页内容对用户问题的相关性，返回0-1之间的分数：
                    
                    用户问题：%s
                    网页内容：%s
                    
                    评估标准：
                    - 1.0: 完全相关，直接回答问题
                    - 0.8: 高度相关，包含大部分答案
                    - 0.6: 中等相关，包含部分有用信息
                    - 0.4: 低度相关，仅有少量相关信息
                    - 0.2: 几乎无关
                    - 0.0: 完全无关
                    
                    请只返回数字分数：
                    """, userMessage, content.length() > 1000 ? content.substring(0, 1000) : content);

            String scoreStr = chatLanguageModel.chat(relevancePrompt).trim();
            return Double.parseDouble(scoreStr.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.5; // 默认中等相关性
        }
    }

    /**
     * 格式化搜索结果
     */
    private String formatSearchResults(List<EnhancedWebPageInfo> webPageInfos, String query, String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 搜索结果汇总 ===\n\n");

        // 添加搜索概况
        long validResults = webPageInfos.stream().filter(info -> info.hasContent).count();
        sb.append(String.format("搜索查询：%s\n", query));
        sb.append(String.format("有效结果：%d/%d\n\n", validResults, webPageInfos.size()));

        // 按相关性分组显示
        List<EnhancedWebPageInfo> highRelevance = webPageInfos.stream()
                .filter(info -> info.relevanceScore >= 0.7)
                .collect(Collectors.toList());

        List<EnhancedWebPageInfo> mediumRelevance = webPageInfos.stream()
                .filter(info -> info.relevanceScore >= 0.4 && info.relevanceScore < 0.7)
                .collect(Collectors.toList());

        if (!highRelevance.isEmpty()) {
            sb.append("【高相关性结果】\n");
            for (EnhancedWebPageInfo info : highRelevance) {
                sb.append(formatEnhancedWebPageInfo(info));
            }
        }

        if (!mediumRelevance.isEmpty()) {
            sb.append("\n【中等相关性结果】\n");
            for (EnhancedWebPageInfo info : mediumRelevance) {
                sb.append(formatEnhancedWebPageInfo(info));
            }
        }

        return sb.toString();
    }

    /**
     * 格式化增强网页信息
     */
    private String formatEnhancedWebPageInfo(EnhancedWebPageInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d. 【相关性: %.1f】%s\n",
                info.index, info.relevanceScore, info.title));
        sb.append(String.format("   链接: %s\n", info.url));

        if (info.hasContent && !info.summary.isEmpty()) {
            sb.append(String.format("   核心内容: %s\n", info.summary));
        } else {
            sb.append(String.format("   描述: %s\n", info.description));
        }
        sb.append("\n");
        return sb.toString();
    }

    private String cleanHtmlContent(String content) {
        if (content == null) return "";
        String cleaned = HTML_TAG_PATTERN.matcher(content).replaceAll(" ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        return cleaned.length() > MAX_CONTENT_LENGTH ?
                cleaned.substring(0, MAX_CONTENT_LENGTH) + "..." : cleaned;
    }

    private String executeCustomTool(ToolExecutionRequest req) throws Exception {
        Class<?> cls = Class.forName(TOOL_SCAN_NAME + req.name());
        Runnable tool = (Runnable) JsonUtils.toJsonObject(req.arguments().toString(), cls);
        tool.run();
        return (String) cls.getMethod("getResult").invoke(tool);
    }

    private void processFinalResult(ChatHandlerquery handlerquery, ChatHandlerResponse handlerResponse, String toolResult) {
        String memoryContent = handlerquery.getMemoryContent();
        String ragContent = handlerquery.getRagContent();
        String userMessage = handlerquery.getQueryContent();

        if (toolResult == null || toolResult.trim().isEmpty()) {
            String answer = chatLanguageModel.chat(
                    PromptUtils.buildAnswerPrompt(memoryContent, ragContent, userMessage)
            );
            handlerResponse.setIsSuccess(true);
            handlerResponse.setChatAnswer(answer);
            return;
        }

        String canPrompt = PromptUtils.buildFuncCanPrompt(toolResult, memoryContent, ragContent, userMessage);
        String canResult = chatLanguageModel.chat(canPrompt).trim();

        if ("CAN".equalsIgnoreCase(canResult)) {
            String answerPrompt = PromptUtils.buildAnswerPrompt(toolResult, memoryContent + ragContent, userMessage);
            String finalAnswer = chatLanguageModel.chat(answerPrompt);
            handlerResponse.setIsSuccess(true);
            handlerResponse.setChatAnswer(finalAnswer);
        } else {
            String answer = chatLanguageModel.chat(PromptUtils.buildAnswerPrompt(null, toolResult, userMessage));
            handlerResponse.setIsSuccess(true);
            handlerResponse.setChatAnswer(answer);
        }
    }

    // 内部类
    private static class ScoredResult {
        final WebSearchOrganicResult result;
        final double score;

        ScoredResult(WebSearchOrganicResult result, double score) {
            this.result = result;
            this.score = score;
        }
    }

    private static class EnhancedWebPageInfo {
        final int index;
        final String title;
        final String description;
        final String url;
        final String summary;
        final boolean hasContent;
        final double relevanceScore;

        EnhancedWebPageInfo(int index, String title, String description, String url,
                            String summary, boolean hasContent, double relevanceScore) {
            this.index = index;
            this.title = title != null ? title : "无标题";
            this.description = description != null ? description : "无描述";
            this.url = url != null ? url : "";
            this.summary = summary != null ? summary : "";
            this.hasContent = hasContent;
            this.relevanceScore = relevanceScore;
        }
    }
}