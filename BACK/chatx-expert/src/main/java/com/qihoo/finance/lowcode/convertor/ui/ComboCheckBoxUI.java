package com.qihoo.finance.lowcode.convertor.ui;

import com.intellij.ui.components.JBScrollPane;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ComboCheckBoxUI extends MetalComboBoxUI {

    private boolean isMultiSel = false;
    public int maxWidth = 300;

    public ComboCheckBoxUI() {
    }

    public ComboCheckBoxUI(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public static ComponentUI createUI(JComponent c) {
        return new ComboCheckBoxUI();
    }

    @Override
    protected ComboPopup createPopup() {
        ComboCheckPopUp popUp = new ComboCheckPopUp(comboBox, maxWidth);
        popUp.getAccessibleContext().setAccessibleParent(comboBox);
        return popUp;
    }

    public class ComboCheckPopUp extends BasicComboPopup {

        private int width = -1;
        private int maxWidth = 300;

        public ComboCheckPopUp(JComboBox cBox, int maxWidth) {
            super(cBox);
            this.maxWidth = maxWidth;
        }

        @Override
        protected JScrollPane createScroller() {
            return new JBScrollPane(
                    list,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }

        @Override
        protected MouseListener createListMouseListener() {
            return new CheckBoxListMouseHandler();
        }

        @Override
        protected KeyListener createKeyListener() {
            return new CheckBoxKeyHandler();
        }

        @Override
        public void show() {

            Dimension popupSize = comboBox.getSize();
            Insets insets = getInsets();
            popupSize.setSize(popupSize.width - (insets.right + insets.left),
                    getPopupHeightForRowCount(comboBox.getMaximumRowCount()));

            int maxWidthOfCell = calcPreferredWidth();
            width = maxWidthOfCell;

            if (comboBox.getItemCount() > comboBox.getMaximumRowCount()) {
                width += scroller.getVerticalScrollBar().getPreferredSize().width;
            }

            if (width > this.maxWidth) {
                width = this.maxWidth;
            }

            if (width < this.comboBox.getWidth()) {
                width = this.comboBox.getWidth();
            }

            if (maxWidthOfCell > width) {
                popupSize.height += scroller.getHorizontalScrollBar().getPreferredSize().height;
            }

            Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height, width, popupSize.height);
            scroller.setMaximumSize(popupBounds.getSize());
            scroller.setPreferredSize(popupBounds.getSize());
            scroller.setMinimumSize(popupBounds.getSize());
            list.invalidate();
            syncListSelectionWithComboBoxSelection();
            list.ensureIndexIsVisible(list.getSelectedIndex());
            setLightWeightPopupEnabled(comboBox.isLightWeightPopupEnabled());
            show(comboBox, popupBounds.x, popupBounds.y);
        }

        private int calcPreferredWidth() {
            int prefferedWidth = 0;
            ListCellRenderer renderer = list.getCellRenderer();
            for (int index = 0, count = list.getModel().getSize(); index < count; index++) {
                Object element = list.getModel().getElementAt(index);
                Component comp = renderer.getListCellRendererComponent(list, element, index, false,
                        false);
                Dimension dim = comp.getPreferredSize();
                if (dim.width > prefferedWidth) {
                    prefferedWidth = dim.width;
                }
            }
            return prefferedWidth;
        }

        void syncListSelectionWithComboBoxSelection() {
            int selectedIndex = comboBox.getSelectedIndex();
            if (selectedIndex == -1) {
                list.clearSelection();
            } else {
                list.setSelectedIndex(selectedIndex);
            }
        }

        public void setPopupWidth(int width) {
            this.width = width;
        }

        protected class CheckBoxKeyHandler extends KeyAdapter {

            @Override
            public void keyPressed(KeyEvent e) {
                isMultiSel = e.isControlDown();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                isMultiSel = e.isControlDown();
            }
        }

        protected class CheckBoxListMouseHandler extends MouseAdapter {

            @Override
            public void mousePressed(MouseEvent anEvent) {
                int index = list.getSelectedIndex();
                ComboCheckBoxEntry item = (ComboCheckBoxEntry) list.getModel().getElementAt(index);
                boolean checked = !item.getChecked();
                int size = list.getModel().getSize();
                item.setChecked(checked);
                updateListBoxSelectionForEvent(anEvent, false);
                Rectangle rect = list.getCellBounds(0, size - 1);
                list.repaint(rect);
            }

            @Override
            public void mouseReleased(MouseEvent anEvent) {

            }
        }
    }
}
