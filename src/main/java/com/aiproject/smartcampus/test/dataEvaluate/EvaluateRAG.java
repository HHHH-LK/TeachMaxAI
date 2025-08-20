package com.aiproject.smartcampus.test.dataEvaluate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

public class EvaluateRAG {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = EvaluateRAG.class.getClassLoader()
                .getResourceAsStream("java_rag_ab_result.json")) {
            JsonNode root = mapper.readTree(in);
            JsonNode list = root.get("testCaseResults");

            int total = 0, top1 = 0, top5 = 0;
            List<Integer> times = new ArrayList<>();

            for (JsonNode node : list) {
                total++;
                JsonNode enh = node.get("systemResponses").get("增强型Java教学RAG系统");
                JsonNode docs = enh.get("retrievedDocuments");
                List<String> ids = new ArrayList<>();
                docs.forEach(d -> ids.add(d.get("id").asText()));

                if (!ids.isEmpty() && ids.get(0).startsWith("java_")) top1++;
                if (ids.subList(0, Math.min(5, ids.size()))
                        .stream().anyMatch(id -> id.startsWith("java_"))) top5++;
                times.add(enh.get("retrievalTimeMs").asInt());
            }

            double avg = times.stream().mapToInt(Integer::intValue).average().orElse(0);
            System.out.printf("Top-1: %.1f%%%n", top1 * 100.0 / total);
            System.out.printf("Top-5: %.1f%%%n", top5 * 100.0 / total);
            System.out.printf("Avg: %.1f ms%n", avg);
        }
    }
}