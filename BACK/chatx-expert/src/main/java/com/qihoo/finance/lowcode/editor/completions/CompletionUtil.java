package com.qihoo.finance.lowcode.editor.completions;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.qihoo.finance.lowcode.editor.lang.CommonLanguageSupport;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import com.qihoo.finance.lowcode.editor.request.LineInfo;
import com.qihoo.finance.lowcode.editor.util.ChatxStringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class CompletionUtil {

    static final Pattern parenEndPattern = Pattern.compile("^\\s*\\) [{:;]*$");
    static final Pattern endWithParenEndPattern = Pattern.compile("\\s*\\) [{:;]*$");

    static List<ChatxInlayList> createEditorCompletions(@NotNull EditorRequest request
            , @NotNull List<ChatxCompletion> items) {
        return items.stream()
                .map(item -> createEditorCompletion(request, item, true))
                .collect(Collectors.toList());
    }

    /**
     * 创建编辑器的InlayList
     * @param request 补全请求
     * @param chatxCompletion 补全响应的内容
     * @param dropLinePrefix
     * @return IDE的InlayList
     */
    @Nullable
    public static ChatxInlayList createEditorCompletion(@NotNull EditorRequest request
            , @NotNull ChatxCompletion chatxCompletion, boolean dropLinePrefix) {
        List<String> lines = new ArrayList<>(chatxCompletion.getCompletion());
        if (lines.isEmpty() || StringUtil.join(lines, "\n").matches("^\\s*$")) {
            log.debug("ignoring empty completion: " + request);
            return null;
        }
        // 删除提示的行与编辑器后面的行重叠的行
        dropOverlappingTrailingLines(lines, request.getDocumentContent(), request.getOffset(), request.getTabWidth());
        if (lines.isEmpty()) {
            return null;
        }
        // 缓存的结果在调用该方法之前，已经把补全结果去除了编辑器相对缓存新增的部分，不需要再处理首行缩进的问题
        if (chatxCompletion.isCached()) {
            return new DefaultInlayList(chatxCompletion, createReplacementRange(request, request.getOffset())
                    , StringUtil.join(lines, "\n"), createEditorInlays(request, request.getOffset(), lines));
        }
        // 处理首行缩进
        int replaceStart = request.getOffset();
        String editorWhitespacePrefix = request.getLineInfo().getWhitespaceBeforeCursor();
        int editorSpaceWidth = ChatxStringUtil.leadingWhitespaceLength(editorWhitespacePrefix, request.getTabWidth());
        if (editorSpaceWidth > 0) {
            String firstLine = lines.get(0);
            int completionSpaceWidth = ChatxStringUtil.leadingWhitespaceLength(firstLine, request.getTabWidth());
            if (dropLinePrefix) {
                if (editorSpaceWidth <= completionSpaceWidth) {
                    // first line在前面步骤已经做了tab转空格处理，所以这里可以直接用substring
                    lines.set(0, firstLine.substring(editorSpaceWidth));
                } else {
                    lines.set(0, firstLine.substring(completionSpaceWidth));
                    if (StringUtils.isBlank(request.getLineInfo().getLine())) {
                        replaceStart = ChatxStringUtil.getReplaceStart(request.getOffset(), editorWhitespacePrefix
                                , editorSpaceWidth - completionSpaceWidth, request.getTabWidth());
                    }
                }
            } else {
                int needDelSpace = Math.min(editorSpaceWidth, completionSpaceWidth);
                if (needDelSpace > 0) {
                    lines.set(0, firstLine.substring(needDelSpace));
                }
            }

        }
        return new DefaultInlayList(chatxCompletion, createReplacementRange(request, replaceStart)
                , StringUtil.join(lines, "\n"), createEditorInlays(request, replaceStart, lines));
    }

    @NotNull
    private static String createReplacementText(@NotNull LineInfo lineInfo, List<String> lines) {
        String text = lines.get(0);
        // 如果行不为空时，需要删除提示内容光标前已有缩进部分
        if (!lineInfo.isBlankLine()) {
            String ws = lineInfo.getWhitespaceBeforeCursor();
            if (text.startsWith(ws)) {
                lines.set(0, text.substring(ws.length()));
            }
        }
        return StringUtils.join(lines, "\n");
    }

    /**
     * 创建要替换的范围对象
     * @param request
     * @param startOffset
     * @return
     */
    @NotNull
    private static TextRange createReplacementRange(@NotNull EditorRequest request, int startOffset) {
        LineInfo lineInfo = request.getLineInfo();
        int endOffset = isReplaceLineSuffix(request)
                ? (request.getOffset() + ChatxStringUtil.leadingWhitespaceLength(lineInfo.getLineSuffix()))
                : request.getOffset();
        return TextRange.create(startOffset, endOffset);
    }

    /**
     * 判断是否要替换光标后的内容，以下情况可以替换：
     * 1. 光标后都是空白字符
     * 2. 其他可以删除的情况，例如; {前的空格
     * @param request
     * @return
     */
    private static boolean isReplaceLineSuffix(@NotNull EditorRequest request) {
        String lineSuffix = request.getLineInfo().getLineSuffix();
        return (ChatxStringUtil.isSpacesOrTabs(lineSuffix, false)
                || CommonLanguageSupport.isValidMiddleOfTheLinePosition(lineSuffix));
    }

    @NotNull
    private static List<ChatxEditorInlay> createEditorInlays(@NotNull EditorRequest request, int offset
            , @NotNull List<String> lines) {
        List<ChatxEditorInlay> inlays = new ArrayList<>();
        if (lines.size() > 1 && request.getLineInfo().getLineSuffix().isBlank() && lines.get(0).isEmpty()) {
            // 光标所在行为空行，且提示的行首行为空，直接去除第一行，并作为block类型的Inlay
            inlays.add(new DefaultChatxEditorInlay(ChatxCompletionType.Block, offset, lines.subList(1, lines.size())));
        } else {
            String editorLinePrefix = request.getLineInfo().getLinePrefix();
            String editorLineSuffix = request.getLineInfo().getLineSuffix();
            String completionLine = lines.get(0);
            // 行中补全且补全首行为空时，取第二行做diff
            if (StringUtils.isNotBlank(editorLineSuffix) && StringUtils.isBlank(completionLine) && lines.size() > 1) {
                completionLine = lines.get(1);
                if (StringUtils.isBlank(completionLine)) {
                    return inlays;
                }
                lines = lines.subList(1, lines.size());
                // 去除补全内容的开头空白字符(除了.之外，其他情况保留1个字符)
                if (StringUtils.isNotBlank(editorLinePrefix) && Character.isWhitespace(completionLine.charAt(0))) {
                    completionLine = completionLine.stripLeading();
                    char c1 = editorLinePrefix.charAt(editorLinePrefix.length() - 1);
                    char c2 = completionLine.charAt(0);
                    if (c1 != ' ' && c1 != '.' && c2 != '.') {
                        completionLine = " " + completionLine;
                    }
                }
            }
            if (StringUtils.isBlank(editorLineSuffix) ||
                    (parenEndPattern.matcher(editorLineSuffix).find() && !endWithParenEndPattern.matcher(completionLine).find())) {
                inlays.add(new DefaultChatxEditorInlay(ChatxCompletionType.Inline, offset, Collections.singletonList(completionLine)));
            } else {
                List<Pair<Integer, String>> diffs = ChatxStringUtil.createDiffInlays(editorLineSuffix, completionLine);
                if (diffs != null && !diffs.isEmpty()) {
                    for (Pair<Integer, String> diff : diffs) {
                        Integer delta = diff.getFirst();
                        String text = diff.second;
                        inlays.add(new DefaultChatxEditorInlay(ChatxCompletionType.Inline, offset + delta
                                , List.of(text)));
                    }
                }
            }
            if (lines.size() > 1) {
                inlays.add(new DefaultChatxEditorInlay(ChatxCompletionType.Block, offset + editorLineSuffix.length()
                        , lines.subList(1, lines.size())));
            }
        }
        return inlays;
    }

    /**
     * 删除补全的行与编辑器行的重叠部分
     * @param lines 补全的行
     * @param editorContent 编辑器文本
     * @param offset 光标在的位置
     * @return 是否删除了重叠的行
     */
    private static boolean dropOverlappingTrailingLines(@NotNull List<String> lines, @NotNull String editorContent
            , int offset, int tabWidth) {
        if (offset < editorContent.length() && editorContent.charAt(offset) == '\n')
            offset++;
        if (offset >= editorContent.length())
            return false;
        List<String> editorLines = ChatxStringUtil.getNextLines(editorContent, offset, lines.size());
        int overlap = ChatxStringUtil.findOverlappingLines(lines, editorLines, tabWidth);
        for (int i = 0; i < overlap; i++)
            lines.remove(lines.size() - 1);
        return (overlap >= 1);
    }

    /**
     * 判断是否要调整光标所在行的空白前缀
     * @param completionLines
     * @param lineInfo
     * @return
     */
    private static boolean isNeedAdjustWhitespace(@NotNull List<String> completionLines, @NotNull LineInfo lineInfo
            , int tabWidth) {
        String editorWhitespacePrefix = lineInfo.getWhitespaceBeforeCursor();
        if (completionLines.isEmpty() || editorWhitespacePrefix.isEmpty()) {
            return false;
        }
        String firstLine = completionLines.get(0);
        int completionSpaceWidth = ChatxStringUtil.leadingWhitespaceLength(firstLine, tabWidth);
        int editorSpaceWidth = ChatxStringUtil.leadingWhitespaceLength(editorWhitespacePrefix, tabWidth);
        // 编辑器空白字符的宽度大于补全结果的空白字符宽度，则需要调整
        return editorSpaceWidth > completionSpaceWidth;
    }


    /**
     * 从上一次的补全提示中，去除prefix开头后，得到新的补全提示。
     * @param apiChoice 最新一次的补全请求结果
     * @param prefix 本次补全请求相对于上一次补全请求新增的部分（用户新输入的内容，开头的\t已经转换为了对应缩进的空格）
     * @return
     */
    @Nullable
    public static ChatxCompletion apiChoiceWithoutPrefix(@NotNull ChatxCompletion apiChoice, @NotNull String prefix) {
        if (prefix.isEmpty()) {
            return apiChoice;
        }
        boolean ignoreFirstWhiteSpace = ChatxStringUtil.leadingWhitespace(prefix).isEmpty();
        List<String> completion = apiChoice.getCompletion();
        String remainingPrefix = prefix;
        for (int i = 0, completionSize = completion.size(); i < completionSize; i++) {
            String line = completion.get(i);
            int prefixLineEnd = remainingPrefix.indexOf('\n');
            String prefixLine = remainingPrefix.substring(0, (prefixLineEnd == -1) ? remainingPrefix.length() : prefixLineEnd);
            if (ignoreFirstWhiteSpace && i == 0) {
                // 如果新输入的内容不以空白字符开始，补全提示的首行开头包含空白字符，则需要去除（例如提示的是" + i", 但是用户输入的是"+ i"）
                String trimmedLine = ChatxStringUtil.stripLeading(line);
                boolean ok = (prefixLineEnd == -1) ? trimmedLine.startsWith(prefixLine) : trimmedLine.equals(prefixLine);
                if (!ok) {
                    return null;
                }
            } else {
                // 如果用户输入的内容以空白字符开头，或者当前不是用户输入的第一行，则直接比较两行内容
                boolean ok = (prefixLineEnd == -1) ? line.startsWith(prefixLine) : line.equals(prefixLine);
                if (!ok) {
                    return null;
                }
            }
            if (prefixLineEnd == -1) {
                // 比较完用户输入的最后一行后，把缓存中的补全提示相当于用户输入剩余的部分作为新的补全提示
                ArrayList<String> newCompletions = new ArrayList<>(completionSize);
                int droppedWhitespace = (ignoreFirstWhiteSpace && i == 0) ? ChatxStringUtil.leadingWhitespace(line).length() : 0;
                newCompletions.add(line.substring(droppedWhitespace + prefixLine.length()));
                if (i + 1 < completionSize) {
                    // 添加补全提示比用户输入多的行部分
                    newCompletions.addAll(completion.subList(i + 1, completionSize));
                }
                if (newCompletions.size() == 1 && StringUtils.isBlank(newCompletions.get(0))) {
                    return null;
                }
                return apiChoice.withCompletion(newCompletions);
            }
            // 取用户输入的第二(x)行，继续与补全提示的第二行比较
            remainingPrefix = remainingPrefix.substring(prefixLineEnd + 1);
        }
        return null;
    }

    public static String TrimEndJustSpace(String s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isSpaceChar(s.charAt(i))) {
            i--;
        }
        return s.substring(0, i + 1);
    }

    public static String TrimEndSpaceTab(String s) {
        int i = s.length() - 1;
        while (i >= 0 && (Character.isSpaceChar(s.charAt(i)) || '\t' == s.charAt(i)) ) {
            i--;
        }
        return s.substring(0, i + 1);
    }

    public static String removeNoNeedNewLine(String s) {
        String[] lines = s.split("\n", -1);
        List<String> resultLines = new ArrayList<>(lines.length);
        boolean needRemoveNewLine = true;
        // 最后一个换行不能去除
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i];
            if (line.matches("\\s*")) {
                if (needRemoveNewLine) {
                    continue;
                }
                resultLines.add(line);
                needRemoveNewLine = true;
                continue;
            }
            resultLines.add(line);
            needRemoveNewLine = false;
        }
        resultLines.add(lines[lines.length - 1]);
        return StringUtils.join(resultLines, "\n");
    }

    /**
     * 纠正补全后的语法问题
     */
    @RequiresEdt
    public static void correctSyntax(Editor editor) {
        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        int tabWidth = editor.getSettings().getTabSize(editor.getProject());
        LineInfo lineInfo = LineInfo.create(document, offset, tabWidth);
        String linePrefix = lineInfo.getLinePrefix().trim();
        String lineSuffix = lineInfo.getLineSuffix().trim();
        if (StringUtils.isBlank(lineSuffix)) {
            return;
        }
        if (linePrefix.endsWith(";") && lineSuffix.matches("[])\"; ]+")) {
            // 纠正分号结尾
            document.deleteString(offset, lineInfo.getLineEndOffset());
        } else if (linePrefix.endsWith("{") && lineSuffix.matches("[ );{]+")) {
            // 纠正{结尾
            document.deleteString(offset, lineInfo.getLineEndOffset());
        } else if (countMatches(lineSuffix, "\"", true) == 1) {
            // 纠正补全后的引号
            int quoteCount = countMatches(linePrefix, "\"", true);
            if (quoteCount > 0 && quoteCount % 2 == 0 && lineSuffix.startsWith("\"")) {
                String lineSuffixNoTrim = lineInfo.getLineSuffix();
                String needDelete = lineSuffixNoTrim.substring(0, lineSuffixNoTrim.indexOf("\"") + 1);
                    document.deleteString(offset, offset + needDelete.length());
            }
        }
    }

    public static int countMatches(String str, String sub, boolean notEscape) {
        if (StringUtils.isNotEmpty(str) && StringUtils.isNotEmpty(sub)) {
            int count = 0;

            for(int idx = 0; (idx = str.indexOf(sub, idx)) != -1; idx += sub.length()) {
                if (notEscape && idx > 0 && str.charAt(idx - 1) == '\\') {
                    continue;
                }
                ++count;
            }
            return count;
        } else {
            return 0;
        }
    }

    /**
     * 优化补全内容
     * @param completion
     * @return
     */
    public static String optimizeCompletion(String completion, EditorRequest request) {
        if (StringUtils.isBlank(completion)) {
            return null;
        }
        completion = completion.replaceAll("\\s+$", " ");
        if (completion.endsWith(")")) {
            // 处理括号不匹配场景
            int leftBracketCount = ChatxStringUtil.countOccurrenceExcludeInString(completion, "(");
            int rightBracketCount = ChatxStringUtil.countOccurrenceExcludeInString(completion, ")");
            int diff = rightBracketCount - leftBracketCount;
            int offset = completion.length() - 1;
            while (diff > 0 && offset >= 0) {
                char c = completion.charAt(offset);
                --offset;
                if (Character.isWhitespace(c)) {
                    continue;
                }
                if (c == ')') {
                    --diff;
                    continue;
                }
                break;
            }
            if (offset < 0) {
                return null;
            }
            completion = completion.substring(0, offset + 1);
            if (StringUtils.isBlank(completion)) {
                return null;
            }
        } else if (completion.endsWith("{")
                && request.getLineInfo().getLineSuffix().trim().matches("[) {]+")) {
            // 处理用户代码包含){，补全代码也包含
            String lineSuffixTrim = request.getLineInfo().getLineSuffix().trim();
            int offset1 = completion.length() - 1;
            int offset2 = lineSuffixTrim.length() - 1;
            while (offset1 >= 0 && offset2 >= 0) {
                char c1 = completion.charAt(offset1);
                if (Character.isWhitespace(c1)) {
                    --offset1;
                    continue;
                }
                char c2 = lineSuffixTrim.charAt(offset2);
                if (Character.isWhitespace(c2)) {
                    --offset2;
                    continue;
                }
                if (c1 == c2) {
                    --offset1;
                    --offset2;
                    continue;
                } else if (c1 == ')' || c1 == '{') {
                    --offset1;
                    continue;
                } else {
                    break;
                }
            }
            if (offset2 < 0) {
                if (offset1 < 0) {
                    return null;
                }
                completion = completion.substring(0, offset1 + 1);
                if (StringUtils.isBlank(completion)) {
                    return null;
                }
            }
        }
        return completion;
    }


    /**
     * 减少补全提示的行，解决跨方法提示，提示太多干扰的问题
     */
    public static List<String> reduceCompletionLine(@NotNull EditorRequest request, List<String> lines) {
        int i = 0;
        int nextLineIndent = request.getLineInfo().getNextLineIndent();
        boolean foundFirstOneIndent = false;
        if (StringUtils.isNotBlank(request.getLineInfo().getLine())) {
            // 光标行不为空行时，补全第一行缩进不代表该行语句的缩进，需要从第二行缩进开始比较
            foundFirstOneIndent = true;
            i = 1;
        }
        for (; i < lines.size(); i++) {
            if (StringUtils.isBlank(lines.get(i))) {
                continue;
            }
            int lineWhiteLength = ChatxStringUtil.leadingWhitespaceLength(lines.get(i), request.getTabWidth());
            if (nextLineIndent > 0) {
                // 将缩进小于下一行缩进的内容去掉
                if (lineWhiteLength < nextLineIndent) {
                    break;
                }
                // 缩进等于下一行，但是下一行是}时，也需要去除
                if (lineWhiteLength == nextLineIndent && "}".equals(request.getLineInfo().getNextLine().trim())) {
                    break;
                }
            }
            if (lineWhiteLength <= request.getTabWidth()) {
                // 一个缩进的}出现，需要删除这行之后的内容(解决跨方法提示)
                if ("}".equals(lines.get(i).trim())) {
                    ++i;
                    break;
                }
                // 第二次出现一个缩进的行，该行及之后的内容都丢弃（解决提示多行属性定义）
                if (foundFirstOneIndent) {
                    break;
                }
                foundFirstOneIndent = true;
            }
        }
        return lines.subList(0, i);
    }
}
