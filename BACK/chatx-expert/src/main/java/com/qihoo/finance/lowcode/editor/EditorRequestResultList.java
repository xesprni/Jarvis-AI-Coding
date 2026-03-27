package com.qihoo.finance.lowcode.editor;

import com.qihoo.finance.lowcode.editor.completions.ChatxInlayList;
import com.qihoo.finance.lowcode.editor.request.EditorRequest;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class EditorRequestResultList {

    private final EditorRequest request;

    final Object inlayLock = new Object();
    final ObjectLinkedOpenHashSet<ChatxInlayList> inlayLists = new ObjectLinkedOpenHashSet<>();
    int index = 0;
    int maxShownIndex = -1;
    boolean hasOnDemandCompletions;

    public void resetInlays() {
        synchronized (this.inlayLock) {
            this.inlayLists.clear();
        }
    }

    public void addInlays(@NotNull ChatxInlayList inlays) {
        synchronized (inlayLock) {
            this.inlayLists.add(inlays);
            this.maxShownIndex = Math.max(0, this.maxShownIndex);
        }
    }

    @Nullable
    public ChatxInlayList getCurrentCompletion() {
        synchronized (inlayLock) {
            return getAtIndexLocked(this.inlayLists, this.index);
        }
    }


    @Nullable
    public List<ChatxInlayList> getAllShownCompletion() {
        synchronized (inlayLock) {
            return inlayLists.stream().limit((this.maxShownIndex + 1)).collect(Collectors.toList());
        }
    }

    public boolean hasCurrent() {
        synchronized (inlayLock) {
            return (index >= 0 && !inlayLists.isEmpty());
        }
    }

    public boolean hasPrev() {
        synchronized (inlayLock) {
            return (inlayLists.size() > 1);
        }
    }

    public boolean hasNext() {
        synchronized (this.inlayLock) {
            return (this.inlayLists.size() > 1);
        }
    }

    @Nullable
    public ChatxInlayList getPrevCompletion() {
        synchronized (this.inlayLock) {
            int size = this.inlayLists.size();
            if (size <= 1) {
                this.index = 0;
                return null;
            }
            this.index--;
            if (this.index < 0)
                this.index = size - 1;
            return getAtIndexLocked(this.inlayLists, this.index);
        }
    }

    @Nullable
    public ChatxInlayList getNextCompletion() {
        synchronized (this.inlayLock) {
            int size = this.inlayLists.size();
            if (size <= 1) {
                this.index = 0;
                return null;
            }
            this.index++;
            if (this.index >= size)
                this.index = 0;
            this.maxShownIndex = Math.max(this.maxShownIndex, this.index);
            return getAtIndexLocked((ObjectSortedSet<ChatxInlayList>)this.inlayLists, this.index);
        }
    }

    public boolean hasOnDemandCompletions() {
        synchronized (this.inlayLock) {
            return (this.hasOnDemandCompletions || this.inlayLists.size() > 1);
        }
    }

    public void setHasOnDemandCompletions() {
        synchronized (this.inlayLock) {
            this.hasOnDemandCompletions = true;
        }
    }

    @Nullable
    private static ChatxInlayList getAtIndexLocked(@NotNull ObjectSortedSet<ChatxInlayList> inlays, int index) {
        return inlays.stream().skip(index).findFirst().orElse(null);
    }
}
