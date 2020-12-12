package nachos.vm;

import nachos.machine.*;

public class LazyLoader 
{
	public LazyLoader(Coff coff)
	{
		this.coff = coff;
		
		int numPages = 0;
		for(int i = 0; i < coff.getNumSections(); i++)
			numPages += coff.getSection(i).getLength(); 
		
		codePages = new CodePage[numPages];
		
		for(int i = 0; i < coff.getNumSections(); i++)
			for(int j = 0; j < coff.getSection(i).getLength(); j++)
				codePages[coff.getSection(i).getFirstVPN() + j] = new CodePage(i, j);
	}
	
	public TranslationEntry loadCodePage(int vpn, int ppn)
	{
		TranslationEntry entry = new TranslationEntry(vpn, ppn, true, coff.getSection(codePages[vpn].getSectionNum()).isReadOnly(), false, false);
		coff.getSection(codePages[vpn].getSectionNum()).loadPage(codePages[vpn].getOffset(), ppn);
		return entry;
	}
	
	public TranslationEntry loadStackPage(int vpn, int ppn)
	{
		byte[] data = Machine.processor().getMemory();
		int start = Processor.makeAddress(ppn, 0);
		for(int i = start; i < start + Processor.pageSize; i++)
			data[i] = 0;
		return new TranslationEntry(vpn, ppn, true, false, false, false);
	}
	
	public TranslationEntry load(PageItem pageItem, int ppn)
	{
		TranslationEntry entry;
		SwapPage swapPage = VMKernel.getSwapFile().getSwapPage(pageItem);
		if (swapPage != null)
		{
			entry = swapPage.entry;
			entry.ppn = ppn;
			entry.valid = true;
			entry.used = false;
			entry.dirty = false;
		}
		else
		{
			if(pageItem.vpn >= 0 && pageItem.vpn < codePages.length)
				entry = loadCodePage(pageItem.vpn, ppn);
			else
				entry = loadStackPage(pageItem.vpn, ppn);
		}
	}
	private Coff coff;
	private CodePage[] codePages;
	
}
