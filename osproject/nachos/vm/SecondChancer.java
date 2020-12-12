package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;


public class SecondChancer extends MemoryManager{
    public SecondChancer(){
        numPhysPages = Machine.processor().getNumPhysPages();
        pagesInMemory = new LinkedList<Integer>();
        freePages = new LinkedList<Integer>();
        for(int i = 0; i < numPhysPages; i++){
            freePages.add(i);
        }
    }
    public TranslationEntry swapIn(PageItem item, LazyLoader lazy){
        int ppn = findNextPage();
        swapOut(ppn);
        TranslationEntry entry = lazy.load(item, ppn);
        VMKernel.tlbManager.addEntry(entry);
        VMKernel.invertedPageTable.put(item, ppn);
        VMKernel.coreMap[ppn] = new Page(item, entry);        
        return entry;
    }

    private int findNextPage(){
        System.out.println(freePages.isEmpty());
        if(!freePages.isEmpty()){
            return freePages.removeFirst();
        }
        int ppn = pagesInMemory.removeFirst();
        System.out.println(ppn);
        Page page = VMKernel.coreMap[ppn];
        System.out.println(page);
        while(page.entry.used){
            page.entry.used = false;
            pagesInMemory.add(ppn);
            ppn = pagesInMemory.removeFirst();
            page = VMKernel.coreMap[ppn];
        }

        return ppn;
    }

    public void swapOut(int ppn){
        Page page = VMKernel.coreMap[ppn];
        if(page != null && page.entry.valid){
            page.entry.valid = false;
            VMKernel.invertedPageTable.remove(page, page.item);
            TranslationEntry entry = VMKernel.tlbManager.find(page.item.vpn, page.entry.readOnly);
            VMKernel.tlbManager.removeEntry(entry);
        }
    }

    protected void removePage(int ppn){
        pagesInMemory.remove(new Integer(ppn));
        freePages.add(ppn);
    }

    










    private int numPhysPages;
    private LinkedList<Integer> pagesInMemory;
    private LinkedList<Integer> freePages;
}