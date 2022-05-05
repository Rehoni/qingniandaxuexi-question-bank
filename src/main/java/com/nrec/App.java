package com.nrec;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.lang.Console;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;

import java.util.*;

/**
 * Hello world!
 */
public class App {
    private final static String LIST_URL = "http://www.mxqe.com/e/tags/index.php?page={}&tagname=青年大学习答案&line=10&tempid=24";
    private final static String URL = "http://www.mxqe.com";
    private final static String FILE_PATH = "answers.txt";

    public static void main(String[] args) {
        //开启计时
        TimeInterval timer = DateUtil.timer();
        Console.log("开启计时，方法执行中...");
        writeFile();
        Console.log("结束计时，用时：{} 毫秒。", timer.interval());
    }

    private static void writeFile() {
        Map<String, String> urlMap = getUrlList();
        FileWriter writer = new FileWriter(FILE_PATH);
        StrBuilder strBuilder = StrBuilder.create();
        urlMap.forEach(
                (title, url) -> strBuilder.append("\n").append(title).append(downloadQuestionAndAnswer(url))
        );
        writer.write(strBuilder.toString());
    }

    private static String downloadQuestionAndAnswer(String url) {
        String content = HttpUtil.get(url);
        // 获取答案开头和结尾
        String quesAndAnswer = ReUtil.get("<strong>题目和答案如下：<\\/strong>(.*?)<\\/p>\n<\\/div>", content, 1);
        // 清理无用的 span
        quesAndAnswer = ReUtil.delAll("<span style=\"display:none\">(.*?)<\\/span>", quesAndAnswer);
        // 替换 <p> 和 </p>
        if (StrUtil.isNotBlank(quesAndAnswer)) {
            quesAndAnswer = quesAndAnswer
                    .replaceAll("<strong>", "")
                    .replaceAll("</strong>", "\n")
                    .replaceAll("<br />", "\n")
                    .replaceAll("<br/>", "\n")
                    .replaceAll("&nbsp;", "\n")
                    .replaceAll("<p>", "")
                    .replaceAll("</p>", "\n");
        }
        return quesAndAnswer;
    }

    private static Map<String, String> getUrlList() {
        // 请求列表页，第 0 页，默认列表数量 10 条。
        String pageUrl = StrUtil.format(LIST_URL, 0);
        String listContent = HttpUtil.get(pageUrl);
        Map<String, String> map = new LinkedHashMap<>(getContentPageAndTitleMap(listContent));
        // 使用正则表达式获取总数
        String strCount = ReUtil.get("<a title=\"总数\">&nbsp;<b>(.*?)<\\/b>", listContent, 1);
        int count = Integer.parseInt(strCount);
        int curPage = 2, pageSize = 10;
        // 翻页
        while (((curPage - 1) * pageSize) < count) {
            pageUrl = StrUtil.format(LIST_URL, curPage - 1);
            listContent = HttpUtil.get(pageUrl);
            map.putAll(getContentPageAndTitleMap(listContent));
            curPage++;
        }
        return map;
    }

    private static Map<String, String> getContentPageAndTitleMap(String listContent) {
        Map<String, String> map = new LinkedHashMap<>();
        // 使用正则获取所有网页链接和标题
        List<String> pageContentUrl =
                ReUtil.findAll("<dt>\n<a href=\"(.*?)\" target=\"_blank\">(.*?)<\\/a>\n<p>", listContent, 1);
        List<String> pageTitle =
                ReUtil.findAll("<dt>\n<a href=\"(.*?)\" target=\"_blank\">(.*?)<\\/a>\n<p>", listContent, 2);
        int size = Math.min(pageContentUrl.size(), pageTitle.size());
        for (int i = 0; i < size; i++) {
            // 在这里给相对路径带上前缀 URL
            map.put(pageTitle.get(i), URL + pageContentUrl.get(i));
        }
        return map;
    }
}
