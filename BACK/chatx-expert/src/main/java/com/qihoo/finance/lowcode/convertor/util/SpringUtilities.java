package com.qihoo.finance.lowcode.convertor.util;

import javax.swing.Spring;
import javax.swing.SpringLayout;
import java.awt.Component;
import java.awt.Container;

public class SpringUtilities {
    public static void makeCompactGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
        SpringLayout layout = (SpringLayout) parent.getLayout();
        // 设置元素的x坐标和宽度
        Spring x = Spring.constant(initialX);
        for (int c = 0; c < cols; c++) {
            // 取每一行该列的宽度，求最大值，计算得出该列的宽度
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width, getConstraintsForCell(r, c, parent, cols).getWidth());
            }
            // 设置该列每个元素的x坐标和宽度
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
                constraints.setX(x);
                constraints.setWidth(width);
            }
            // 下一列的x坐标 = 当前x + 当前列宽 + xPad
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)));
        }
        // 设置元素的y坐标和高度
        Spring y = Spring.constant(initialY);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < cols; c++) {
                height = Spring.max(height, getConstraintsForCell(r, c, parent, cols).getHeight());
            }
            for (int c = 0; c < cols; c++) {
                SpringLayout.Constraints constraints = getConstraintsForCell(r, c, parent, cols);
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)));
        }
        // 设置父组件的约束，子组件设置的initialX和initialY相当于设置WEST、NORTH，还需要手动设置SOUTH、EAST的间距
        SpringLayout.Constraints pConstraints = layout.getConstraints(parent);
        pConstraints.setConstraint(SpringLayout.EAST, x);
        pConstraints.setConstraint(SpringLayout.SOUTH, y);
    }

    private static SpringLayout.Constraints getConstraintsForCell(int row, int col, Container parent, int cols) {
        SpringLayout layout = (SpringLayout)parent.getLayout();
        Component c = parent.getComponent(row * cols + col);
        return layout.getConstraints(c);
    }
}
