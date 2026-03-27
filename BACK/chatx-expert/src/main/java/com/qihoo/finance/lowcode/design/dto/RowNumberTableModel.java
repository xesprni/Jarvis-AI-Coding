package com.qihoo.finance.lowcode.design.dto;

import java.util.List;

import javax.swing.table.DefaultTableModel;

@SuppressWarnings({"unchecked", "rawtypes"})
public class RowNumberTableModel extends DefaultTableModel {
	
    private List rowDatas;
    private RowOrderChangeListener rowOrderChangeListener;

    public RowNumberTableModel(Object[][] data, String[] columns) {
    	super(data, columns);
    }
    
    public <T> RowNumberTableModel(Object[][] data, String[] columns, List<T> rowDatas) {
    	this(data, columns);
    	if (rowDatas != null) {
    		this.rowDatas = rowDatas;
    	}
    }
    
    public void setRowOrderChangeListener(RowOrderChangeListener rowOrderChangeListener) {
    	this.rowOrderChangeListener = rowOrderChangeListener;
    }
    
    @SuppressWarnings("unchecked")
	public <T> T getRowData(int rowIndex) {
    	if (rowDatas.size() > rowIndex) {
    		return (T)rowDatas.get(rowIndex);
    	}
    	return null;
    }
    
    @SuppressWarnings("unchecked")
	public <T> List<T> getRowDatas() {
		return rowDatas;
	}
    
    public void swapRowData(int from, int to) {
    	Object fromData = rowDatas.get(from);
    	Object toData = rowDatas.get(to);
    	rowDatas.set(from, toData);
    	rowDatas.set(to, fromData);
    	if (rowOrderChangeListener != null) {
    		rowOrderChangeListener.afterRowOrderChange(this, from, to);
    	}
    }

	@Override
	public void addRow(Object[] data) {
		super.addRow(data);
		if (this.rowDatas != null) {
			rowDatas.add(null);
		}
	}
	
	public void addRow(Object[] data, Object rowData) {
		super.addRow(data);
		if (this.rowDatas != null) {
			rowDatas.add(rowData);
		}
	}

	@Override
	public void insertRow(int row, Object[] data) {
		super.insertRow(row, data);
		if (this.rowDatas != null) {
			rowDatas.add(row, null);
		}
	}

	public void insertRow(int row, Object[] data, Object rowData) {
		super.insertRow(row, data);
		rowDatas.add(row, rowData);
	}

	@Override
	public void removeRow(int row) {
		super.removeRow(row);
		if (rowDatas != null) {
			rowDatas.remove(row);
		}
	}

	@Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return rowIndex + 1;
        } else {
        	return super.getValueAt(rowIndex, columnIndex - 1);
        }
    }

    @Override
	public int getColumnCount() {
		return super.getColumnCount() + 1;
	}

	@Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "#";
        } else {
        	return super.getColumnName(column - 1);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
    	if (columnIndex == 0) {
    		return;
    	}
    	super.setValueAt(value, rowIndex, columnIndex - 1);
    }

    @FunctionalInterface
    public interface RowOrderChangeListener {
    	
    	void afterRowOrderChange(RowNumberTableModel model, int from, int to);
    	
    }
}
