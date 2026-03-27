package com.qihoo.finance.lowcode.editor.util;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.diff.Diff;
import com.intellij.util.diff.FilesTooBigForDiffException;
import com.intellij.util.text.TextRanges;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatxStringUtil {

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("[\r\n|\n]");

    public static String[] splitLines(@NotNull CharSequence text) {
        String tmp = text.toString();
        return tmp.lines().toArray(String[]::new);
    }

    /**
     * 获取文本结尾的空白字符
     * @param text
     * @return
     */
    @NotNull
    public static String trailingWhitespace(@NotNull String text) {
        if (text.isEmpty())
            return "";
        int endOffset = text.length();
        while (endOffset > 0) {
            char ch = text.charAt(endOffset - 1);
            if (ch == '\n' || !Character.isWhitespace(ch)) {
                break;
            }
            endOffset--;
        }
        return text.substring(endOffset);
    }

    public static int trailingWhitespaceLength(@NotNull String text) {
        if (text.isEmpty())
            return 0;
        int length = text.length();
        int endOffset = length;
        while (endOffset > 0) {
            char ch = text.charAt(endOffset - 1);
            if (ch != ' ' && ch != '\t')
                break;
            endOffset--;
        }
        return length - endOffset;
    }

    @NotNull
    public static String leadingWhitespace(@NotNull String text) {
        if (text.isEmpty())
            return "";
        return text.substring(0, leadingWhitespaceLength(text));
    }

    public static int leadingWhitespaceLength(@NotNull String text) {
        int length = text.length();
        int offset = 0;
        while (offset < length) {
            char ch = text.charAt(offset);
            if (ch == '\n' || !Character.isWhitespace(ch))
                break;
            offset++;
        }
        return offset;
    }

    public static int leadingWhitespaceLength(@NotNull String text, int tabWidth) {
        int whitespaceLength = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n' || !Character.isWhitespace(ch)) {
                break;
            }
            if (ch == '\t') {
                whitespaceLength += tabWidth;
            } else {
                whitespaceLength++;
            }
        }
        return whitespaceLength;
    }

    /** 下一个非空行的缩进 */
    public static int nextNotBlankLineWhitespaceLength(List<String> lines, int lineIndex, int tabwidth) {
        int whitespaceLength = 0;
        for (int i = lineIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (StringUtils.isNotBlank(line)) {
                whitespaceLength = leadingWhitespaceLength(line, tabwidth);
                break;
            }
        }
        return whitespaceLength;
    }

    public static boolean endsWithNewLine(String str) {
        int length = str.length();
        for (int i = length - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '\n') {
                return true;
            } else if (Character.isWhitespace(c)) {
                continue;
            } else {
                return false;
            }
        }
        return false;
    }

    @NotNull
    public static String stripLeading(@NotNull String text) {
        if (text.isEmpty())
            return "";
        int length = leadingWhitespaceLength(text);
        return (length == 0) ? text : text.substring(length);
    }

    public static int findOverlapLength(@NotNull String withTrailing, @NotNull String withLeading) {
        if (withTrailing.isEmpty() || withLeading.isEmpty())
            return 0;
        int trailingLength = withTrailing.length();
        for (int i = 0; i <= trailingLength; i++) {
            if (withLeading.startsWith(withTrailing.substring(i)))
                return trailingLength - i;
        }
        return 0;
    }

    /**
     * 找出第一个字符串结尾与第二个字符串开头重叠的行数
     * @param withTrailing 放补全提示的行
     * @param withLeading 放编辑器的行
     * @return 重叠的行数
     */
    public static int findOverlappingLines(@NotNull List<String> withTrailing, @NotNull List<String> withLeading, int tabWidth) {
        if (withTrailing.isEmpty() || withLeading.isEmpty())
            return 0;
        int trailingSize = withTrailing.size();
        int leadingSize = withLeading.size();
        int maxLines = Math.min(trailingSize, leadingSize);
        int overlapping = 0;
        for (int i = maxLines; i >= 1; i--) {
            List<String> lines = withTrailing.subList(trailingSize - i, trailingSize);
            if (linesMatch(withLeading.subList(0, i), lines, tabWidth, true)) {
                overlapping = i;
                break;
            }
        }
        return overlapping;
    }

    @Nullable
    public static List<Pair<Integer, String>> createDiffInlays(@NotNull String editorLineSuffix
            , @NotNull String completionLine) {
        String commonPrefix = findCommonPrefix(completionLine, editorLineSuffix);
        String editorAdjusted = editorLineSuffix.substring(commonPrefix.length());
        String completionAdjusted = completionLine.substring(commonPrefix.length());
        int[] editorChars = editorAdjusted.chars().toArray();
        int[] completionChars = completionAdjusted.chars().toArray();
        patchCharPairs(completionChars);
        int patchDelta = commonPrefix.length();
        try {
            Diff.Change changelist = Diff.buildChanges(editorChars, completionChars);
            if (changelist == null)
                return null;
            LinkedList<Pair<Integer, String>> result = new LinkedList<>();
            ArrayList<Diff.Change> changes = changelist.toList();
            for (Diff.Change change : changes) {
                if (change.inserted > 0) {
                    result.add(Pair.create(Integer.valueOf(change.line0 + patchDelta)
                            , unpatchCharPairs(completionChars, change.line1, change.inserted)));
                }
            }
            return result;
        } catch (FilesTooBigForDiffException e) {
            return null;
        }
    }

    private static String findCommonPrefix(@NotNull String data, @NotNull String reference) {
        int maxSize = Math.min(data.length(), reference.length());
        int first = 0;
        for (int i = 0; i < maxSize && data.charAt(i) == reference.charAt(i); i++) {
            first++;
        }
        return data.substring(0, first);
    }

    /**
     * 从offset位置开始，找出text的maxLines行数据，如果后面没有maxLines行数据，则有多少返回多少
     * @param text
     * @param offset
     * @param maxLines
     * @return
     */
    public static List<String> getNextLines(@NotNull String text, int offset, int maxLines) {
        LinkedList<String> lines = new LinkedList<>();
        int done = 0;
        int index = offset;
        while (done < maxLines) {
            int lineEnd = text.indexOf('\n', index);
            if (lineEnd == -1) {
                // 如果找不到换行符，说明已经到了text的最后一行
                if (text.length() > index) {
                    lines.add(text.substring(index));
                }
                break;
            }
            lines.add(text.substring(index, lineEnd));
            index = lineEnd + 1;
            done++;
        }
        return lines;
    }

    public static boolean isSpaceOrTab(char c, boolean withNewline) {
        return (c == ' ' || c == '\t' || (withNewline && c == '\n'));
    }

    public static boolean isSpacesOrTabs(CharSequence text, boolean withNewlines) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!isSpaceOrTab(c, withNewlines))
                return false;
        }
        return true;
    }

    public static boolean linesMatch(@NotNull Iterable<String> a, @NotNull Iterable<String> b, int tabWidth, boolean trimEnd) {
        Iterator<String> itA = a.iterator();
        Iterator<String> itB = b.iterator();
        while (itA.hasNext() && itB.hasNext()) {
            String itemA = itA.next().replaceAll("\t", StringUtils.repeat(" ", tabWidth));
            String itemB = itB.next().replaceAll("\t", StringUtils.repeat(" ", tabWidth));
            boolean match = trimEnd ? itemA.stripTrailing().equals(itemB.stripTrailing()) : itemA.equals(itemB);
            if (!match) {
                return false;
            }
        }
        return (!itA.hasNext() && !itB.hasNext());
    }

    /**
     * 将字符串中的括号屏蔽掉，避免与正常代码中的括号做了diff
     * @param chars
     * @return
     */
    static int[] patchCharPairs(int[] chars) {
        int parenChar = 65536;
        TextRanges stringRanges = findStringRanges(chars);
        for (int i = 0; i < chars.length; i++) {
            int c = chars[i];
            if ((c == '(' || c == ')') && isInRange(stringRanges, i)) {
                // 屏蔽掉常量字符串中的括号
                chars[i] = parenChar + ((c == ')') ? 1 : 0);
            } else if (c == '(') {
                int closeIndex = firstMatchingPair(chars, i + 1, ')', '(', stringRanges);
                if (closeIndex != -1) {
                    // 如果后续的代码括号成对出现，则屏蔽掉这个（
                    chars[i] = parenChar;
                    chars[closeIndex] = parenChar + 1;
                }
            }
        }
        return chars;
    }

    /**
     * 判断内容是否有成对的字符，如（），{}，[]
     * @param chars 文本内容
     * @param startIndex 开始检查的位置
     * @param pairClose 关闭字符
     * @param pairOpen 打开字符
     * @param excludedRanges 排除的范围（一般会排除字符串内的这些字符）
     * @return
     */
    private static int firstMatchingPair(int[] chars, int startIndex, char pairClose, char pairOpen, TextRanges excludedRanges) {
        int openCount = 0;
        for (int i = startIndex; i < chars.length; i++) {
            if (!isInRange(excludedRanges, i)) {
                int c = chars[i];
                if (c == pairOpen) {
                    openCount++;
                } else if (c == pairClose) {
                    if (openCount == 0)
                        return i;
                    openCount--;
                }
            }
        }
        return -1;
    }

    private static boolean isInRange(TextRanges ranges, int i) {
        for (TextRange range : ranges) {
            if (range.contains(i))
                return true;
            if (i > range.getEndOffset()) {
                //TODO lzh 这里是不是要删掉，单个range的end offset应该不能代表所有的range
                break;
            }
        }
        return false;
    }

    /**
     * 寻找代码中的字符串范围（单引号或者双引号括起来的部分）
     * @param chars
     * @return
     */
    private static TextRanges findStringRanges(int[] chars) {
        TextRanges ranges = new TextRanges();
        int singleQuotedStart = -1;
        int doubleQuotedStart = -1;
        for (int i = 0; i < chars.length; i++) {
            int c = chars[i];
            if (c == '"' && singleQuotedStart == -1) {
                if (doubleQuotedStart == -1) {
                    doubleQuotedStart = i;
                } else {
                    ranges.union(new ProperTextRange(doubleQuotedStart, i));
                    doubleQuotedStart = -1;
                }
            } else if (c == '\'' && doubleQuotedStart == -1) {
                if (singleQuotedStart == -1) {
                    singleQuotedStart = i;
                } else {
                    ranges.union(new ProperTextRange(singleQuotedStart, i));
                    singleQuotedStart = -1;
                }
            }
        }
        return ranges;
    }

    static String unpatchCharPairs(int[] patchedData, int offset, int count) {
        final int parenChar = 65536;
        final int braceChar = 65538;
        final int bracketChar = 65540;
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            int c = patchedData[offset + i];
            switch (c) {
                case parenChar: // (
                    result[i] = 40;
                    break;
                case parenChar + 1: // )
                    result[i] = 41;
                    break;
                case braceChar:
                    result[i] = 123;
                    break;
                case 65539:
                    result[i] = 125;
                    break;
                case bracketChar:
                    result[i] = 91;
                    break;
                case 65541:
                    result[i] = 93;
                    break;
                default:
                    result[i] = c;
                    break;
            }
        }
        return new String(result, 0, count);
    }

    /**
     * 统计某个子串出现的次数，排除字符串中出现的次数
     * @param text
     * @param substring
     * @return
     */
    public static int countOccurrenceExcludeInString(String text, String substring) {
        int count = 0;
        boolean isInQuotes = false;
        int substringLength = substring.length();
        for (int i = 0; i < text.length() - substringLength + 1; i++) {
            String currentSubstring = text.substring(i, i + substringLength);
            if (currentSubstring.equals(substring) && !isInQuotes) {
                count++;
            }
            if (text.charAt(i) == '\"') {
                isInQuotes = !isInQuotes;
            }
        }
        return count;
    }

    public static int getReplaceStart(int offset, String widthSpacePrefix, int needDelWidth, int tabWidth) {
        for (int i = widthSpacePrefix.length() - 1; i >= 0 && needDelWidth > 0; i--) {
            char c = widthSpacePrefix.charAt(i);
            if (c == ' ') {
                --needDelWidth;
                --offset;
            } else if (c == '\t') {
                needDelWidth -= tabWidth;
                --offset;
            } else {
                break;
            }
        }
        return offset;
    }

    public static String replaceLeadingTabs(String line, int tabWidth) {
        if (StringUtils.isEmpty(line)) {
            return line;
        }
        int tabCount = countLeadingTabs(line, '\t', 0, line.length(), true);
        if (tabCount > 0) {
            String tabSpaces = StringUtil.repeatSymbol(' ', tabWidth);
            for (int i = 0; i < tabCount; i++)
                line = line.replaceFirst("\t", tabSpaces);
        }
        return line;
    }

    static int countLeadingTabs(@NotNull CharSequence text, char c, int start, int end, boolean stopAtOtherChar) {
        boolean forward = (start <= end);
        start = forward ? Math.max(0, start) : Math.min(text.length(), start);
        end = forward ? Math.min(text.length(), end) : Math.max(0, end);
        int count = 0;
        int i;
        for (i = forward ? start : (start - 1); forward == ((i < end)); i += forward ? 1 : -1) {
            if (text.charAt(i) == c) {
                count++;
            } else if (text.charAt(i) != ' ') {
                if (text.charAt(i) != '\n')
                    if (stopAtOtherChar)
                        break;
            }
        }
        return count;
    }
}
