package nachos.vm;

import nachos.machine.TranslationEntry;

public abstract class MemoryManager{
    public MemoryManager(){}
    protected abstract void removePage(int ppn);
    public abstract TranslationEntry swapIn(PageItem item, LazyLoader lazy);
    public abstract void swapOut(int ppn);
}