package com.qihoo.finance.lowcode.design.listener;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import com.qihoo.finance.lowcode.design.dto.RowNumberTableModel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class DragDropRowTableUI extends BasicTableUI {
	private boolean draggingRow = false;
	private int startDragPoint;
	private int dyOffset;
	private int oldRowIndex = -1;
	private int newRowIndex = -1;

	@Setter
	private DragDropConfig dragDropConfig;

	@Override
	protected MouseInputListener createMouseInputListener() {
		return new DragDropRowMouseInputHandler();
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		super.paint(g, c);
		if (draggingRow) {
			g.setColor(table.getParent().getBackground());
			Rectangle cellRect = table.getCellRect(table.getSelectedRow(), 0, false);
			g.copyArea(cellRect.x, cellRect.y, table.getWidth(), table.getRowHeight(), cellRect.x, dyOffset);
			if (dyOffset < 0) {
				g.fillRect(cellRect.x, cellRect.y + (table.getRowHeight() + dyOffset), table.getWidth(),
						dyOffset * -1);
			} else {
				g.fillRect(cellRect.x, cellRect.y, table.getWidth(), dyOffset);
			}
		}
	}

	class DragDropRowMouseInputHandler extends MouseInputHandler {
		public void mousePressed(MouseEvent e) {
			// 鼠标按下获得起点位置
			super.mousePressed(e);
			startDragPoint = (int) e.getPoint().getY();
			oldRowIndex = table.getSelectedRow();
		}

		public void mouseDragged(MouseEvent e) {
			super.mouseDragged(e);
			// 拖拽选中的行，稍微难点就在鼠标拖拽这一事件里面
			int fromRow = table.getSelectedRow();
			if (fromRow >= 0) {
				draggingRow = true;
			}
			int rowHeight = table.getRowHeight();
			// 获取选中行中间的Y坐标
			int middleOfSelectedRow = (rowHeight * fromRow) + (rowHeight / 2);
			int toRow = -1;
			int yMousePoint = (int) e.getPoint().getY();
			if (yMousePoint < (middleOfSelectedRow - rowHeight)) {
				// Move row up
				toRow = fromRow - 1;
			} else if (yMousePoint > (middleOfSelectedRow + rowHeight)) {
				// Move row down
				toRow = fromRow + 1;
			}
			// 数据调换
			boolean canDrag = true;
			if (dragDropConfig != null && toRow >= 0 && toRow < table.getRowCount()) {
				canDrag = dragDropConfig.canDrap(table.getModel(), fromRow, toRow);
			}
			if (canDrag) {
				if (toRow >= 0 && toRow < table.getRowCount()) {
					cancelEditing();
					TableModel model = table.getModel();
					for (int i = 0; i < model.getColumnCount(); i++) {
						Object fromValue = model.getValueAt(fromRow, i);
						Object toValue = model.getValueAt(toRow, i);
						model.setValueAt(toValue, fromRow, i);
						model.setValueAt(fromValue, toRow, i);
					}
					if (model instanceof RowNumberTableModel) {
						((RowNumberTableModel)model).swapRowData(fromRow, toRow);
					}
					table.setRowSelectionInterval(toRow, toRow);
					newRowIndex = toRow;
					startDragPoint = yMousePoint;
				}
			} else {
				startDragPoint = yMousePoint;
			}
			dyOffset = (startDragPoint - yMousePoint) * -1;
			table.repaint();
		}

		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(e);
			draggingRow = false;
			if (newRowIndex >= 0 && newRowIndex < table.getRowCount() && newRowIndex != oldRowIndex) {
				table.setRowSelectionInterval(newRowIndex, newRowIndex);
			}
			// 对表格的重新刷新。。。
			table.repaint();
			newRowIndex = -1;
			oldRowIndex = -1;
		}

		private void cancelEditing() {
			int row = table.getEditingRow();
			int column = table.getEditingColumn();
			if (row != -1 && column != -1) {
				TableCellEditor cellEditor = table.getCellEditor(row, column);
				if (cellEditor != null) {
					cellEditor.cancelCellEditing();
				}
			}
		}
	}

	@FunctionalInterface
	public interface DragDropConfig {

		boolean canDrap(TableModel model, int fromRow, int toRow);
	}

}
