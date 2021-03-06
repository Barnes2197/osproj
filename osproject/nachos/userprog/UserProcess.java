package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;
import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
        public UserProcess() {
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        for (int i=0; i<numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
            
        openFiles = new ArrayList<OpenFile>();
        links = new HashMap<String, Integer>();
        children = new LinkedList<UserProcess>();
        childStatus = new HashMap<Integer, Integer>();
        processID = numOfProcesses;
        numOfProcesses++;
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
        
        byte[] memory = Machine.processor().getMemory();
        
        int amountRead = 0;
        
        if(vaddr >= 0 && vaddr < pageTable.length*pageSize)
        {
            int freeBytes = data.length - offset;
            
            if((data.length>0)&&(length > 0)&&(data.length>=length))
            {

                int vpn, physAddr, amountToRead;
                
                for(int i = 0; i <= length; i++)
                {
                    if(amountRead >= length)
                        break;
                    
                    if(freeBytes <= 0)
                        break;
                    
                    amountToRead = Math.min(freeBytes, Math.min(pageSize, length-amountRead));
                    vpn = Processor.pageFromAddress(vaddr+i);
                    
                    if(!pageTable[vpn].valid)
                        break;
                    
                    physAddr = Processor.makeAddress(pageTable[vpn].ppn,0);
                    
                    if(physAddr < 0 || physAddr >= Machine.processor().getMemory().length)
                        break;
                    
                    System.arraycopy(memory, physAddr, data, offset+amountRead, amountToRead);
                    amountRead += amountToRead;
                    freeBytes -= amountToRead;
                }
            }	
        }
        
        return amountRead;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
        
        byte[] memory = Machine.processor().getMemory();
        
        int amountWrite = 0;
        
        if(vaddr >= 0 && vaddr < pageTable.length*pageSize)
        {
            int freeBytes = data.length - offset;
            
            if((data.length>0)&&(length > 0)&&(data.length>=length))
            {

                int vpn, physAddr, amountToWrite;
                
                for(int i = 0; i <= length; i++)
                {
                    if(amountWrite >= length)
                        break;
                    
                    if(freeBytes <= 0)
                        break;
                    
                    amountToWrite = Math.min(freeBytes, Math.min(pageSize, length-amountWrite));
                    vpn = Processor.pageFromAddress(vaddr+i);
                    
                    if(!pageTable[vpn].valid || pageTable[vpn].readOnly)
                        break;
                    
                    physAddr = Processor.makeAddress(pageTable[vpn].ppn,0);
                    
                    if(physAddr < 0 || physAddr >= Machine.processor().getMemory().length)
                        break;
                    
                    System.arraycopy(memory, offset+amountWrite, data, physAddr, amountToWrite);
                    amountWrite += amountToWrite;
                    freeBytes -= amountToWrite;
                    pageTable[vpn].dirty = true;
                    pageTable[vpn].used = true;
                }
            }	
        }
        return amountWrite;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
            if (numPages > Machine.processor().getNumPhysPages()) {
                coff.close();
                Lib.debug(dbgProcess, "\tinsufficient physical memory");
                return false;
            }
            
            
            // load sections
            allocatedPages = ((UserKernel)Kernel.kernel).allocatePages(numPages);
            if(allocatedPages == null || allocatedPages.size() == 0)
            {
                return false;
            }
            
            pageTable = new TranslationEntry[numPages];
            for (int i = 0; i < numPages; i++)
            {
                pageTable[i] = new TranslationEntry(i, allocatedPages.get(i), true, false, false, false);
            }
            
            for (int s=0; s<coff.getNumSections(); s++) {
                CoffSection section = coff.getSection(s);
                
                Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");
        
                for (int i=0; i<section.getLength(); i++) {
                int vpn = section.getFirstVPN()+i;
                if(section.isReadOnly())
                    pageTable[vpn].readOnly = true;
                section.loadPage(i, pageTable[vpn].ppn);
                }
            }
            
            return true;
        }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	((UserKernel) Kernel.kernel).freePages(allocatedPages);
    	coff.close();
    }   

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }


    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
        return handleHalt();
    case syscallExit:
        return handleExit(a0);
    case syscallExec:
        return handleExec(a0,a1,a2);
    case syscallJoin:
        return handleJoin(a0, a1);
    case syscallCreate:
        return handleCreate(a0);
    case syscallOpen:
        return handleOpen(a0);
    case syscallRead:
        return handleRead(a0,a1,a2);
    case syscallWrite:
        return handleClose(a0);
    case syscallUnlink:
        return handleUnlink(a0);
	default:
        Lib.debug(dbgProcess, "Unknown syscall " + syscall);
        handleExit(-1);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
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
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    public int handleCreate(int name){
        String fileName = readVirtualMemoryString(name, max_length_of_file);
        if (fileName == null || fileName.length() == 0) {
            return -1;
        }
        int size = openFiles.size();
        for(int i = 0; i < size; ++i)
        {
            if(openFiles.get(i) != null && openFiles.get(i).getName().equals(fileName))
            {
                return i;
            }
        }

        OpenFile file = ThreadedKernel.fileSystem.open(fileName, true);

        openFiles.add(file);
        return openFiles.size() - 1;
    }

    public int handleOpen(int name){
        String fileName = readVirtualMemoryString(name, max_length_of_file);
        if (fileName == null || fileName.length() == 0) {
            return -1;
        }
        int size = openFiles.size();
        OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);

        if(links.containsKey(fileName))
        {
            links.put(fileName, links.get(fileName) + 1);
        }
        else 
        {
            links.put(fileName, 1);
        }
        
        openFiles.add(file);
        return openFiles.size() - 1;
    }
    
    public int handleRead(int fileIndex, int buffer, int count){
        try {
            OpenFile file = openFiles.get(fileIndex);
            byte[] bytes = new byte[count];

            int numBytesRead = file.read(bytes, 0, count);
            int numBytesWritten = writeVirtualMemory(buffer, bytes, 0, numBytesRead);

            if (numBytesRead != numBytesWritten){
                return -1;
            }

            return numBytesRead;
        } catch(IndexOutOfBoundsException e)
        {
            return -1;
        }

        

    }

    public int handleWrite(int fileIndex, int buffer, int count) {
        try {
            OpenFile file = openFiles.get(fileIndex);
            byte[] bytes = new byte[count];

            int numBytesToWrite= writeVirtualMemory(buffer, bytes, 0, count);

            if (count != numBytesToWrite){
                return -1;
            }

            return file.read(bytes, 0, numBytesToWrite);
        } catch(IndexOutOfBoundsException e)
        {
            return -1;
        }

        
    }

    public int handleClose(int fileIndex) {
        try {
            OpenFile file = openFiles.get(fileIndex);
            if(links.containsKey(file.getName()) && links.get(file.getName()) > 0)
            {
                links.put(file.getName(), links.get(file.getName()) - 1);
            }
            else{
                return -1;
            }
            openFiles.remove(fileIndex);
            file.close();

            return 0;
        } catch(IndexOutOfBoundsException e)
        {
            return -1;
        }
        
    }

    public int handleUnlink(final int name)
    {
        String fileName = readVirtualMemoryString(name, max_length_of_file);
        if (fileName == null || fileName.length() == 0) {
            return -1;
        }
        int size = openFiles.size();
        OpenFile file = ThreadedKernel.fileSystem.open(fileName, false);
        openFiles.remove(file);
        if(!ThreadedKernel.fileSystem.remove(fileName)) {
            return -1;
        }
        return 0;
    }

    private int handleExec(int virtualAddress, int arg1, int argVir)
    {
    	if (virtualAddress < 0 || arg1 < 0 || argVir < 0)
    	{
    		return -1;
    	}
    	String fileName = readVirtualMemoryString(virtualAddress, 256);
    	
    	if (fileName == null)
    	{
    		return -1;
    	}
    	
    	if (fileName.contains(".coff") != true)
    	{
    		return -1;
    	}
    	
    	String [] holder = new String [arg1];
    	
    	for (int i = 0; i < arg1; i++)
    	{
    		byte [] buff = new byte[4];
    		int memoryRead = readVirtualMemory(argVir + i * 4, buff);
    		
    		if (memoryRead != 4)
    		{
    			return -1;
    		}
    		
    		int argumentVirtualAddr = Lib.bytesToInt(buff, 0);
    		
    		String argument = readVirtualMemoryString(argumentVirtualAddr, 256);
    		
    		if(argument == null)
    		{
    			return -1;
    		}
    		holder[i] = argument;
    	}
    	
    	UserProcess childProcess = UserProcess.newUserProcess();
    	
    	if(childProcess.execute(fileName, holder) == false)
    	{
    		return -1;
    	}
    	
    	childProcess.parentProcess = this;
    	this.children.add(childProcess);
    	
    	return childProcess.processID;
    }

    private int handleExit(int status)
    {
    	if (parentProcess != null)
    	{
    		lock.acquire();
    		parentProcess.childStatus.put(processID, status);
    		lock.release();
    	}
    	
    	unloadSections();
    	
    	for(UserProcess process: children)
    	{
    		process.parentProcess = null;
    	}
    	
    	if (processID == 0)
    	{
    		Kernel.kernel.terminate();
    	}
    	else
    	{
    		UThread.finish();
    	}
    	return 0;
    }

    private int handleJoin(int processID, int statusVAddr)
    {
    	if (processID < 0 || statusVAddr < 0)
    	{
    		return -1;
		}
		
    	UserProcess child = null;
    	
    	for (int i = 0; i < children.size(); i++)
    	{
    		if(children.get(i).processID == processID);
    		{
				child = children.get(i);
    		}
    	}
    	
    	
    	if (child == null)
    	{
    		return -1;
		}
		
    	child.thread.join();
    	
    	child.parentProcess = null;
    	children.remove(child);
    	lock.acquire();
    	Integer status = childStatus.get(child.processID);
    	lock.release();
    	
    	if (status == null)
    	{
    		return 0;
    	}
    	else
    	{
    		byte [] buff = new byte[4];
    		buff = Lib.bytesFromInt(status);
    		int count = writeVirtualMemory(statusVAddr, buff);
    		
    		if(count == 4)
    		{
    			return 1;
    		}
    		else{
    			return 0;
    		}
    	}
    	
    }
    
    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    private List<Integer> allocatedPages;
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
    private static final int max_length_of_file = 256;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private ArrayList<OpenFile> openFiles;
    private HashMap<String, Integer> links;

    protected int processID;
    private UserProcess parentProcess;
    private LinkedList<UserProcess> children;
    private Lock lock;
    private UThread thread;
    private int count = 0;
    protected HashMap <Integer, Integer> childStatus;
    public static int numOfProcesses = 0;
}
