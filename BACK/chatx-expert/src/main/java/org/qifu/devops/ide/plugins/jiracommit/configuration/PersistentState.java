package org.qifu.devops.ide.plugins.jiracommit.configuration;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//import javax.lang.model.element.Element;

//import javax.swing.text.Element;

import javax.lang.model.element.Element;

@State(name ="SearchSetting",storages = {@Storage("SearchSetting.xml")})
public class PersistentState implements PersistentStateComponent<Element> {
    private  String path;

    private PersistentState(){

    }
    public static PersistentState getInstance(){
        return ServiceManager.getService(PersistentState.class);
    }


    @Nullable
    @Override
    public Element getState() {
//        Element element = new Element("");
        return null;
    }

    @Override
    public void loadState(@NotNull Element state) {

    }
}
