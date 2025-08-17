import { marked } from "marked";
import hljs from "highlight.js";
import "highlight.js/styles/github.css";

import katex from "katex";
import "katex/dist/katex.min.css";

// Emoji 替换
function renderEmoji(text) {
    return text
        .replace(/:smile:/g, "😄")
        .replace(/:heart:/g, "❤️")
        .replace(/:fire:/g, "🔥")
        .replace(/:star:/g, "⭐");
}

// 数学公式渲染
function renderMath(text) {
    // 块级公式 $$
    text = text.replace(/\$\$([\s\S]+?)\$\$/g, (match, formula) => {
        try {
            return katex.renderToString(formula, {
                throwOnError: false,
                displayMode: true,
            });
        } catch (err) {
            console.error("KaTeX 块级渲染错误:", err);
            return match;
        }
    });

    // 行内公式 $
    text = text.replace(/\$([^\$]+)\$/g, (match, formula) => {
        try {
            return katex.renderToString(formula, {
                throwOnError: false,
                displayMode: false,
            });
        } catch (err) {
            console.error("KaTeX 行内渲染错误:", err);
            return match;
        }
    });

    return text;
}

export default function renderMarkdown(content) {
    marked.setOptions({
        highlight(code, lang) {
            try {
                if (lang && hljs.getLanguage(lang)) {
                    return hljs.highlight(code, { language: lang }).value;
                }
                return hljs.highlightAuto(code).value;
            } catch (err) {
                console.error("代码高亮错误:", err);
                return code;
            }
        },
        gfm: true, // 支持 GFM（表格、todo list）
        breaks: true, // 支持换行
    });

    // 1. Markdown 转 HTML
    let html = marked.parse(content);

    // 2. Emoji 渲染
    html = renderEmoji(html);

    // 3. 数学公式渲染
    html = renderMath(html);

    return html;
}
