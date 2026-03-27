package com.qihoo.finance.lowcode.editor.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class LanguageInfoManager {

    private static final Key<Language> CACHE_KEY = Key.create("chatx.languageInfo");

    @NotNull
    public static Language findLanguageMapping(@NotNull PsiFile file) {
        Language cachedLanguageInfo = (Language)CACHE_KEY.get((UserDataHolder)file);
        if (cachedLanguageInfo != null) {
            return cachedLanguageInfo;
        }
        CACHE_KEY.set((UserDataHolder)file, file.getLanguage());
        return file.getLanguage();
    }

    @NotNull
    public static Language findFallback(@NotNull VirtualFile file) {
        Language language = Language.ANY;
        FileType fileType = file.getFileType();
        if (fileType instanceof LanguageFileType) {
            language = ((LanguageFileType)fileType).getLanguage();
        }
        return language;
    }
}
