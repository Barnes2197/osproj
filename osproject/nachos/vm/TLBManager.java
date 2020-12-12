package nachos.vm;

import nachos.machine.Machine;
import nachos.machine.Lib;
import nachos.machine.TranslationEntry;

public class TLBManager 
{
	//Not really sure what to do here, so I'm just going to set the entry to being invalid wherever necessary
	public void removeEntry(TranslationEntry entry)
	{
		if (!entry.valid)
			return;
		int index = 0;
		for(int i = 0; i < Machine.processor().getTLBSize(); i++)
			if(!Machine.processor().readTLBEntry(i).valid)
			{
				index = i;
				break;
			}
		entry.valid = false;
		Machine.processor().writeTLBEntry(index, entry);
	}
	
	public void addEntry(TranslationEntry entry)
	{
		int index = -1;
		for(int i = 0; i < Machine.processor().getTLBSize(); i++)
			if(!Machine.processor().readTLBEntry(i).valid)
			{
				index = i;
				break;
			}
		if(index == -1)
			index = Lib.random(Machine.processor().getTLBSize());
		
		sendToPageTable(index);
		
		Machine.processor().writeTLBEntry(index, entry);
	}
	
	public TranslationEntry find(int vpn, boolean write)
	{
		for(int i = 0; i < Machine.processor().getTLBSize(); i++)
		{
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			if(entry.valid && entry.vpn == vpn)
			{
				if (entry.readOnly && write)
					return null;
				if (entry.dirty || write)
					entry.dirty = true;
				else
					entry.dirty = false;
				entry.used = true;
				Machine.processor().writeTLBEntry(i, entry);
				return entry;
			}
		}
		return null;
    }
    
    public void findAndInvalidate(int vpn){
        TranslationEntry entry = null;
        int index = -1;
        for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
			entry = Machine.processor().readTLBEntry(i);
			if (entry.valid && entry.vpn == vpn)
                index = i;
                break;
        }
        if(entry == null || !entry.valid) {
            return;
        }
		Machine.processor().writeTLBEntry(index, entry);
    }
	
	public void sendToPageTable(int index)
	{
		TranslationEntry entry = Machine.processor().readTLBEntry(index);
		if(!entry.valid)
			return;
		entry.valid = false;
		Machine.processor().writeTLBEntry(index, entry);
	}

	public void clear(){
		for(int i = 0; i < Machine.processor().getTLBSize(); i++){
			entry = Machine.processor().readTLBEntry(i);
			sendToPageTable(i);
			entry.valid = false;
			Machine.processor().writeTLBEntry(i, entry);
		}
	}
}