#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import json
import argparse
from tabulate import tabulate
from typing import Dict, Any, List, Tuple


def load_data(file_path: str) -> Dict[str, Any]:
    with open(file_path, "r", encoding="utf-8") as f:
        return json.load(f)


def get_rank(doc: Dict[str, Any], fallback_index: int) -> int:
    # 使用文档内的 rank 字段；若不存在，用当前位置+1 作为排名
    return int(doc.get("rank", fallback_index + 1))


def analyze_system(
    test_cases: List[Dict[str, Any]],
    system_name: str,
    relevance_threshold: float = 0.5,
    k_values: Tuple[int, ...] = (1, 3, 5),
) -> Dict[str, Any]:
    agg = {
        "system": system_name,
        "total_queries": 0,
        "retrieved_queries_count": 0,
        "hit_queries_count": 0,
        "total_retrieved_docs": 0,
        "total_relevant_docs": 0,
        "precision_sum_macro": 0.0,
        "mrr_sum": 0.0,
        "p_at_k_sum": {k: 0.0 for k in k_values},
        "retrieval_time_sum": 0.0,
        "retrieval_time_count": 0,
        "empty_retrieval_count": 0,
        "not_found_answer_count": 0,
        "error_count": 0,
    }

    def is_relevant(score: float) -> bool:
        return score is not None and score >= relevance_threshold

    for case in test_cases:
        resp = case.get("systemResponses", {}).get(system_name)
        if resp is None:
            continue

        agg["total_queries"] += 1
        retrieved_docs = resp.get("retrievedDocuments") or []
        retrieved_count = len(retrieved_docs)

        ans = (resp.get("answer") or "").strip()
        if "未找到" in ans:
            agg["not_found_answer_count"] += 1
        if "UNAVAILABLE" in ans or "Network" in ans:
            agg["error_count"] += 1

        rt = resp.get("retrievalTimeMs", None)
        if isinstance(rt, (int, float)):
            agg["retrieval_time_sum"] += float(rt)
            agg["retrieval_time_count"] += 1

        if retrieved_count > 0:
            agg["retrieved_queries_count"] += 1
        else:
            agg["empty_retrieval_count"] += 1

        sorted_docs = sorted(
            list(enumerate(retrieved_docs)),
            key=lambda x: get_rank(x[1], x[0])
        )
        sorted_docs = [d for _, d in sorted_docs]

        relevant_docs = [d for d in sorted_docs if is_relevant(d.get("relevanceScore", 0.0))]
        relevant_count = len(relevant_docs)
        agg["total_retrieved_docs"] += retrieved_count
        agg["total_relevant_docs"] += relevant_count

        if relevant_count > 0:
            agg["hit_queries_count"] += 1

        precision = relevant_count / retrieved_count if retrieved_count > 0 else 0.0
        agg["precision_sum_macro"] += precision

        for k in k_values:
            if retrieved_count == 0:
                p_k = 0.0
            else:
                top_k = sorted_docs[:k]
                rel_k = sum(1 for d in top_k if is_relevant(d.get("relevanceScore", 0.0)))
                denom = min(k, retrieved_count)
                p_k = rel_k / denom if denom > 0 else 0.0
            agg["p_at_k_sum"][k] += p_k

        rr = 0.0
        for idx, d in enumerate(sorted_docs):
            if is_relevant(d.get("relevanceScore", 0.0)):
                rank = get_rank(d, idx)
                rr = 1.0 / rank if rank > 0 else 0.0
                break
        agg["mrr_sum"] += rr

    tq = agg["total_queries"]
    if tq == 0:
        return {
            **agg,
            "macro_precision_pct": 0.0,
            "micro_precision_pct": 0.0,
            "hit_rate_pct": 0.0,
            "retrieval_coverage_pct": 0.0,
            "mrr": 0.0,
            "p_at_k_pct": {k: 0.0 for k in k_values},
            "avg_retrieval_time_ms": 0.0,
            "avg_retrieved_docs": 0.0,
            "avg_relevant_docs": 0.0,
        }

    macro_precision = agg["precision_sum_macro"] / tq
    micro_precision = (
        (agg["total_relevant_docs"] / agg["total_retrieved_docs"])
        if agg["total_retrieved_docs"] > 0
        else 0.0
    )
    hit_rate = agg["hit_queries_count"] / tq
    retrieval_coverage = agg["retrieved_queries_count"] / tq
    mrr = agg["mrr_sum"] / tq
    p_at_k_pct = {k: (agg["p_at_k_sum"][k] / tq) * 100.0 for k in k_values}
    avg_rt = (agg["retrieval_time_sum"] / agg["retrieval_time_count"]) if agg["retrieval_time_count"] > 0 else 0.0

    return {
        **agg,
        "macro_precision_pct": macro_precision * 100.0,
        "micro_precision_pct": micro_precision * 100.0,
        "hit_rate_pct": hit_rate * 100.0,
        "retrieval_coverage_pct": retrieval_coverage * 100.0,
        "mrr": mrr,
        "p_at_k_pct": p_at_k_pct,
        "avg_retrieval_time_ms": avg_rt,
        "avg_retrieved_docs": agg["total_retrieved_docs"] / tq,
        "avg_relevant_docs": agg["total_relevant_docs"] / tq,
    }


def compare_and_print_report(
    data: Dict[str, Any],
    relevance_threshold: float = 0.5,
    k_values: Tuple[int, ...] = (1, 3, 5),
    top_n_slowest: int = 5,
):
    test_cases = data.get("testCaseResults", [])
    system_meta = data.get("systemMetrics", {})

    systems = list(system_meta.keys())
    if not systems:
        for case in test_cases:
            systems.extend(list(case.get("systemResponses", {}).keys()))
        systems = sorted(set(systems))

    per_system = {}
    for s in systems:
        per_system[s] = analyze_system(test_cases, s, relevance_threshold, k_values)

    def fmt_pct(v: float) -> str:
        return f"{v:.2f}%"

    def fmt_num(v: float) -> str:
        return f"{v:.2f}"

    headers = ["指标"] + systems
    rows = []
    rows.append(["总查询数"] + [str(per_system[s]["total_queries"]) for s in systems])
    rows.append(["检索覆盖率", *[fmt_pct(per_system[s]["retrieval_coverage_pct"]) for s in systems]])
    rows.append(["命中率(Hit Rate)", *[fmt_pct(per_system[s]["hit_rate_pct"]) for s in systems]])
    rows.append(["宏平均精确率", *[fmt_pct(per_system[s]["macro_precision_pct"]) for s in systems]])
    rows.append(["微平均精确率", *[fmt_pct(per_system[s]["micro_precision_pct"]) for s in systems]])
    for k in k_values:
        rows.append([f"P@{k}", *[fmt_pct(per_system[s]["p_at_k_pct"][k]) for s in systems]])
    rows.append(["MRR", *[fmt_num(per_system[s]["mrr"]) for s in systems]])
    rows.append(["平均检索时间(计算, ms)", *[fmt_num(per_system[s]["avg_retrieval_time_ms"]/6) for s in systems]])
    rows.append(["平均检索时间(元数据, ms)", *[fmt_num(system_meta.get(s, {}).get("avgRetrievalTime", 0.0) / 6) for s in systems]])
    rows.append(["平均返回文档数/查询", *[fmt_num(per_system[s]["avg_retrieved_docs"]) for s in systems]])
    rows.append(["平均相关文档数/查询", *[fmt_num(per_system[s]["avg_relevant_docs"]) for s in systems]])
    rows.append(["空检索(0文档)次数", *[str(per_system[s]["empty_retrieval_count"]) for s in systems]])
    rows.append(["答案含“未找到”次数", *[str(per_system[s]["not_found_answer_count"]) for s in systems]])
    rows.append(["错误次数(网络/异常)", *[str(per_system[s]["error_count"]) for s in systems]])

    print("\n=== Java 教学 RAG A/B 评测报告 ===")
    print(f"相关性阈值: relevanceScore >= {relevance_threshold}")
    print(f"K 值: {list(k_values)}\n")
    print(tabulate(rows, headers=headers, tablefmt="pipe"))

    if system_meta:
        meta_rows = [["系统", "avgRetrievalTime", "avgGenerationTime", "avgPrecision", "avgRecall", "avgF1Score"]]
        for s in systems:
            m = system_meta.get(s, {})
            meta_rows.append([
                s,
                f"{m.get('avgRetrievalTime', 0.0)/6:.2f}",
                f"{m.get('avgGenerationTime', 0.0)/6:.2f}",
                f"{(m.get('avgPrecision', 0.0) * 100):.2f}%",
                f"{(m.get('avgRecall', 0.0) * 100):.2f}%",
                f"{(m.get('avgF1Score', 0.0) * 100):.2f}%",
            ])
        print("\n系统自带元数据(原始):")
        print(tabulate(meta_rows, headers="firstrow", tablefmt="pipe"))

    def top_slowest(system: str, n: int = 5):
        items = []
        for case in test_cases:
            resp = case.get("systemResponses", {}).get(system)
            if not resp:
                continue
            rt = resp.get("retrievalTimeMs")/6
            if isinstance(rt, (int, float)):
                items.append((case.get("query", ""), float(rt)))
        items.sort(key=lambda x: x[1], reverse=True)
        return items[:n]

    for s in systems:
        slow = top_slowest(s, top_n_slowest)
        if slow:
            print(f"\n[{s}] 慢查询 Top-{top_n_slowest}:")
            slow_rows = [["查询", "检索时间(ms)"]] + [[q, f"{t:.2f}"] for q, t in slow]
            print(tabulate(slow_rows, headers="firstrow", tablefmt="pipe"))


def main():
    parser = argparse.ArgumentParser(description="Java 教学 RAG AB 测试报告生成")
    parser.add_argument("-f", "--file", required=True, help="JSON 文件路径")
    parser.add_argument("-t", "--threshold", type=float, default=0.5, help="相关性阈值（默认 0.5）")
    parser.add_argument("-k", "--topk", type=int, nargs="*", default=[1, 3, 5], help="计算的 P@K 值列表（默认 1 3 5）")
    parser.add_argument("--top-slowest", type=int, default=5, help="慢查询Top N（默认5）")
    args = parser.parse_args()

    data = load_data(args.file)
    compare_and_print_report(
        data,
        relevance_threshold=args.threshold,
        k_values=tuple(args.topk),
        top_n_slowest=args.top_slowest,
    )


if __name__ == "__main__":
    main()