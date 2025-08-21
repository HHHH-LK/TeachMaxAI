package com.aiproject.smartcampus.test.dataEvaluate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
        import java.util.stream.Collectors;

public class EvaluateRAG {

    private static final String ENHANCED = "增强型Java教学RAG系统";
    private static final String BASELINE = "传统Java教学RAG系统（基线）";
    private static final DecimalFormat PCT = new DecimalFormat("0.0%");
    private static final DecimalFormat DF1 = new DecimalFormat("0.0");
    private static final DecimalFormat DF2 = new DecimalFormat("0.00");
    // 默认路径（可用 args[0] 覆盖）
    private static final String DEFAULT_PATH = "/Users/lk_hhh/Documents/ss/TeacherMaxAI/java_rag_ab_result.json";

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String pathStr = args != null && args.length > 0 ? args[0] : DEFAULT_PATH;
        InputStream in = null;

        try {
            if (pathStr != null) {
                Path p = Paths.get(pathStr);
                if (Files.exists(p)) {
                    System.out.println("从路径读取: " + p.toAbsolutePath());
                    in = Files.newInputStream(p);
                }
            }
            if (in == null) {
                System.out.println("未从路径读取到文件，回退到 classpath: java_rag_ab_result.json");
                in = EvaluateRAG.class.getClassLoader().getResourceAsStream("java_rag_ab_result.json");
                if (in == null) {
                    System.err.println("找不到文件: " + pathStr + " 且 classpath 中无 java_rag_ab_result.json");
                    return;
                }
            }

            JsonNode root = mapper.readTree(in);

            // 1) 系统级指标
            System.out.println("==== 系统级指标（来自 systemMetrics） ====");
            JsonNode sysMetrics = root.path("systemMetrics");
            for (Iterator<String> it = sysMetrics.fieldNames(); it.hasNext(); ) {
                String sysName = it.next();
                JsonNode m = sysMetrics.get(sysName);
                double rt = m.path("avgRetrievalTime").asDouble(0);
                double gt = m.path("avgGenerationTime").asDouble(0);
                double p  = m.path("avgPrecision").asDouble(0);
                double r  = m.path("avgRecall").asDouble(0);
                double f1 = m.path("avgF1Score").asDouble(0);
                System.out.printf("- %s: avgRetrieval=%.2f ms, avgGeneration=%.2f ms, Precision=%.3f, Recall=%.3f, F1=%.3f%n",
                        sysName, rt, gt, p, r, f1);
            }
            System.out.println();

            // 2) 用例级分析（增强型系统）
            JsonNode cases = root.path("testCaseResults");
            if (!cases.isArray() || cases.size() == 0) {
                System.out.println("未找到 testCaseResults，用例级分析跳过。");
                return;
            }

            int total = cases.size();
            int top1 = 0, top5 = 0, retrieved = 0, answered = 0, notFoundCount = 0, errorCount = 0;

            List<Integer> times = new ArrayList<>();
            List<CaseTime> caseTimes = new ArrayList<>();
            Map<String, Integer> docFreq = new HashMap<>();
            int totalDocs = 0;

            List<String> notFoundQueries = new ArrayList<>();
            List<String> errorQueries = new ArrayList<>();

            for (JsonNode c : cases) {
                String query = c.path("query").asText("");
                JsonNode enh = c.path("systemResponses").path(ENHANCED);

                int timeMs = enh.path("retrievalTimeMs").asInt(0);
                times.add(timeMs);
                caseTimes.add(new CaseTime(query, timeMs));

                String answer = enh.path("answer").asText("");
                boolean notFound = containsNotFound(answer);
                boolean error = containsError(answer);

                if (notFound) {
                    notFoundCount++;
                    notFoundQueries.add(query);
                } else if (error) {
                    errorCount++;
                    errorQueries.add(query);
                } else if (!answer.trim().isEmpty()) {
                    answered++;
                }

                JsonNode docs = enh.path("retrievedDocuments");
                List<String> ids = new ArrayList<>();
                if (docs.isArray()) {
                    for (JsonNode d : docs) {
                        String id = d.path("id").asText("");
                        if (!id.isEmpty()) {
                            ids.add(id);
                            // 修复点：使用 Integer::sum 或 lambda
                            docFreq.merge(id, 1, Integer::sum);
                            totalDocs++;
                        }
                    }
                }

                if (!ids.isEmpty()) {
                    retrieved++;
                    if (ids.get(0).startsWith("java_")) top1++;
                    int end = Math.min(5, ids.size());
                    boolean hitTop5 = ids.subList(0, end).stream().anyMatch(i -> i.startsWith("java_"));
                    if (hitTop5) top5++;
                }
            }

            Collections.sort(times);
            double avg = times.stream().mapToInt(Integer::intValue).average().orElse(0);
            int min = times.get(0);
            int max = times.get(times.size() - 1);
            double median = median(times);
            double p90 = percentile(times, 90);
            double p95 = percentile(times, 95);

            List<CaseTime> fastest = caseTimes.stream()
                    .sorted(Comparator.comparingInt(ct -> ct.timeMs))
                    .limit(3).collect(Collectors.toList());
            List<CaseTime> slowest = caseTimes.stream()
                    .sorted((a, b) -> Integer.compare(b.timeMs, a.timeMs))
                    .limit(3).collect(Collectors.toList());

            List<Map.Entry<String, Integer>> docTop = docFreq.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .collect(Collectors.toList());

            System.out.println("==== 用例级指标（增强型Java教学RAG系统） ====");
            System.out.printf("总用例数: %d%n", total);
            System.out.printf("Top-1: %s (%d/%d)%n", PCT.format(top1 / (double) total), top1, total);
            System.out.printf("Top-5: %s (%d/%d)%n", PCT.format(top5 / (double) total), top5, total);
            System.out.printf("检索覆盖率(有任意检索结果): %s (%d/%d)%n", PCT.format(retrieved / (double) total), retrieved, total);
            System.out.printf("答案覆盖率(非空且非\"未找到/错误\"): %s (%d/%d)%n", PCT.format(answered / (double) total), answered, total);
            System.out.printf("未找到: %d, 错误: %d%n", notFoundCount, errorCount);
            System.out.println();

            System.out.println("— 检索耗时统计（ms） —");
            System.out.printf("Min=%d, Median=%.0f, Avg=%.2f, P90=%.0f, P95=%.0f, Max=%d%n",
                    min, median, avg, p90, p95, max);
            System.out.println();

            double avgDocsAll = total == 0 ? 0 : (totalDocs * 1.0 / total);
            double avgDocsSuccess = retrieved == 0 ? 0 : (totalDocs * 1.0 / retrieved);
            System.out.println("— 文档数量统计 —");
            System.out.printf("总检索文档数=%d, 平均(所有用例)=%.2f, 平均(有检索结果的用例)=%.2f%n",
                    totalDocs, avgDocsAll, avgDocsSuccess);
            System.out.println();

            System.out.println("— 文档使用频次 Top —");
            for (Map.Entry<String, Integer> e : docTop) {
                System.out.printf("%s: %d%n", e.getKey(), e.getValue());
            }
            System.out.println();

            System.out.println("— 最快 3 条 —");
            for (CaseTime ct : fastest) {
                System.out.printf("%6d ms | %s%n", ct.timeMs, ct.query);
            }
            System.out.println();

            System.out.println("— 最慢 3 条 —");
            for (CaseTime ct : slowest) {
                System.out.printf("%6d ms | %s%n", ct.timeMs, ct.query);
            }
            System.out.println();

            System.out.println("==== 基线 vs 增强（摘自 systemMetrics） ====");
            JsonNode base = sysMetrics.path(BASELINE);
            JsonNode enh = sysMetrics.path(ENHANCED);
            System.out.printf("基线:  avgRetrieval=%.2f ms, F1=%.3f%n",
                    base.path("avgRetrievalTime").asDouble(0), base.path("avgF1Score").asDouble(0));
            System.out.printf("增强:  avgRetrieval=%.2f ms, F1=%.3f%n",
                    enh.path("avgRetrievalTime").asDouble(0), enh.path("avgF1Score").asDouble(0));

        } finally {
            if (in != null) try { in.close(); } catch (Exception ignore) {}
        }
    }

    private static boolean containsNotFound(String answer) {
        if (answer == null) return false;
        return answer.contains("未找到");
    }

    private static boolean containsError(String answer) {
        if (answer == null) return false;
        String a = answer.toLowerCase(Locale.ROOT);
        return a.contains("unavailable") || a.contains("失败") || a.contains("error");
    }

    private static double median(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        int n = values.size();
        if (n % 2 == 1) return values.get(n / 2);
        return (values.get(n / 2 - 1) + values.get(n / 2)) / 2.0;
    }

    // 最近排名法（Nearest Rank）
    private static double percentile(List<Integer> values, double p) {
        if (values == null || values.isEmpty()) return 0;
        if (p <= 0) return values.get(0);
        if (p >= 100) return values.get(values.size() - 1);
        int n = values.size();
        int rank = (int) Math.ceil((p / 100.0) * n);
        rank = Math.max(1, Math.min(rank, n));
        return values.get(rank - 1);
    }

    private static class CaseTime {
        String query;
        int timeMs;

        CaseTime(String query, int timeMs) {
            this.query = query;
            this.timeMs = timeMs;
        }
    }
}
