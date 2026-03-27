package com.qihoo.finance.lowcode.aiquestion.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

public class MarkdownParser {
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();


    public static String parseMarkdown(String markdown) {
        final String prefix = "<!DOCTYPE html>  \n" +
                "<html lang=\"zh-CN\">  \n" +
                "<head>  \n" +
                "<meta charset=\"UTF-8\">  \n" +
                "<style>  \n" +
                "    body {  \n" +
                "        font-family: \"Microsoft YaHei\", sans-serif; \n" +
                "        font-size: 10px; \n" +
                "    }  \n" +
                "</style>  " +
                "<body>";
        final String suffix = "</body>\n" + "</html>";
        String render = RENDERER.render(PARSER.parse(markdown));
        render = render
                .replaceAll("<p>", "<span>")
                .replaceAll("</p>", "</span><br>")
                .replaceAll("\n", "<br>")
                .replaceAll("<br><li>", "<li>")
                .replaceAll("<br></li>", "</li>")
                .replaceAll("<li><br>", "<li>")
                .replaceAll("</li><br>", "</li>")

                .replaceAll("<ul><br>", "<ul>")
                .replaceAll("</ul><br>", "</ul>")
                .replaceAll("<br><ul>", "<ul>")
                .replaceAll("<br></ul>", "</ul>")
                .replaceAll("<br><span>", "<span>")
                .replaceAll("<br></span>", "</span>")
                .replaceAll("<br><h2>", "<h2>")
                .replaceAll("<br><h3>", "<h3>")
                .replaceAll("<br></h2>", "</h2>")
                .replaceAll("<br></h3>", "</h3>")
//        ;
                .replaceAll("<img ", "<img width=\"250\" high=\"250\" onload=\"if(this.width >= 250){this.width = 250}\" ");
//        if (render.endsWith("\n")) {
//            render = render.substring(0, render.length() - 1);
//        }

        return prefix + render + suffix;
    }
}

