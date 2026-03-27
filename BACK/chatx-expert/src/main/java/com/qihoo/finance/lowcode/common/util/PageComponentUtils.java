package com.qihoo.finance.lowcode.common.util;

import com.qihoo.finance.lowcode.common.entity.base.PageDTO;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * CusEditorComponent
 *
 * @author fengjinfu-jk
 * date 2023/10/11
 * @version 1.0.0
 * @apiNote CusEditorComponent
 */
public class PageComponentUtils {

    public static JPanel pagePanel(PageDTO<?> resultData, int page, int pageSize, BiConsumer<Integer, Integer> pageAction) {
        JPanel pagePanel = new JPanel(new FlowLayout());

        long count = resultData.getTotal();
        long totalPage = count % pageSize == 0 ? count / pageSize : ((count / pageSize) + 1);

        JLabel showPage = new JLabel(String.valueOf(page));
        JLabel showCurrentCount = new JLabel(String.format("当前页 %s 条", resultData.getRows().size()));
        JLabel showCount = new JLabel(String.format("  ( 共 %s 条, 每页最多展示 %s 条 )", count, pageSize));

        JButton prePage = new JButton(Icons.scaleToWidth(Icons.PRE_PAGE, 16));
        prePage.setBorderPainted(false);
        prePage.setContentAreaFilled(false);

        int prePageNum = Math.max(1, (page - 1));
        prePage.addActionListener(e -> {
            if (prePageNum == page) return;
            pageAction.accept(prePageNum, pageSize);
        });

        JButton nextPage = new JButton(Icons.scaleToWidth(Icons.NEXT_PAGE, 16));
        nextPage.setBorderPainted(false);
        nextPage.setContentAreaFilled(false);

        int nextPageNum = Math.min((int) totalPage, (page + 1));
        nextPage.addActionListener(e -> {
            if (nextPageNum == page) return;
            pageAction.accept(nextPageNum, pageSize);
        });

        pagePanel.add(prePage, FlowLayout.LEFT);
        pagePanel.add(showPage);
        pagePanel.add(nextPage);
        pagePanel.add(showCurrentCount);
        pagePanel.add(showCount);

        return pagePanel;
    }
}
