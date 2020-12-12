package nachos.vm;
import java.util.*;
import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import nachos.vm.TLBManager;
import nachos.ag.VMGrader;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
        super();
        
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	super.initialize(args);
	coreMap = new Page[Machine.processor().getNumPhysPages()];
	tlbManager = new TLBManager();
	memoryManager = new SecondChancer();
    }

    /**
     * Test this kernel.
     */	
    public void selfTest() {
        super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
	super.run();
    }
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
    
    public static TranslationEntry getPageEntry(PageItem pageItem)
    {
    	if(!invertedPageTable.contains(pageItem))
    		return null;
    	
    	int ppn = invertedPageTable.get(pageItem);
    	
    	Page page = coreMap[ppn];
    	
    	if(page == null || !page.entry.valid)
    		return null;
    	
    	return page.entry;
    }

    public SwapFile getSwapFile(){
        if(!swapfile){
            swapfile = new SwapFile();
        }

        return swapfile;
    }
    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;
    public static Page[] coreMap;
    public static TLBManager tlbManager;
    public static Hashtable<PageItem, Integer> invertedPageTable = new Hashtable<PageItem, Integer>();
    private static final char dbgVM = 'v';
    public static SecondChancer memoryManager;
    public static SwapFile swapfile;
}