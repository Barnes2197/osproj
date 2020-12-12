package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	super();
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
	super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	//super.restoreState();
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
        lazyBoi = new LazyLoader(coff);
        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
     //super.unloadSections();
        VMKernel.tlbManager.clear();
        for (int i = 0; i < numPages; i++) {
            PageItem item = new PageItem(processID, i);
            Integer ppn = VMKernel.invertedPageTable.remove(item);
            if (ppn != null) {
                VMKernel.memoryManager.removePage(ppn);
                VMKernel.coreMap[ppn].entry.valid = false;
            }
    }
    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
        case Processor.exceptionTLBMiss:
            int vpn = Processor.pageFromAddress(processor.readRegister(Processor.regBadVAddr));
            PageItem pageItem = new PageItem(processID, vpn);
            int ppn;
            TranslationEntry entry;
            if(VMKernel.invertedPageTable.contains(pageItem))
            {
                ppn = VMKernel.invertedPageTable.get(pageItem);
                entry = VMKernel.getPageEntry(pageItem);
                if(entry == null)
                    super.handleExit(-1);
            }
            else
            {
                lock.acquire();
                numPageFaults++;
                entry = VMKernel.memoryManager.swapIn(pageItem, lazyBoi);
                lock.release();
                if(entry == null)
                    super.handleExit(-1);
            }
            VMKernel.tlbManager.addEntry(entry);
            break;
        default:
            System.out.println("Found unknown syscall " + cause);
            super.handleException(cause);
            break;
        }
    }
    
    private static Lock lock = new Lock();
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
    private static int numPageFaults = 0;
    private LazyLoader lazyBoi;
}