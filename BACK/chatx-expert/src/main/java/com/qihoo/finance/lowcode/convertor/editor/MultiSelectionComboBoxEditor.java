package com.qihoo.finance.lowcode.convertor.editor;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiSelectionComboBoxEditor extends BasicComboBoxEditor {
    private final JTextField editorComponent;
    private final List<String> selectedItems;

    public MultiSelectionComboBoxEditor() {
        editorComponent = new JTextField();
        selectedItems = new ArrayList<>();

        editorComponent.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSelectedItems();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSelectedItems();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSelectedItems();
            }
        });
    }

    private void updateSelectedItems() {
        String text = editorComponent.getText();
        selectedItems.clear();
        selectedItems.addAll(Arrays.asList(text.split(",")));

//        editor.firePropertyChange("selectedItem", null, selectedItems);
    }

    @Override
    public Component getEditorComponent() {
        return editorComponent;
    }

    @Override
    public Object getItem() {
        return selectedItems;
    }

    @Override
    public void setItem(Object anObject) {
        if (anObject instanceof List) {
            selectedItems.clear();
            selectedItems.addAll((List<String>) anObject);
            editorComponent.setText(String.join(",", selectedItems));
        }
    }
}