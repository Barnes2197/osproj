package nachos.vm;
import nachos.machine.*;
public class Page{
	PageItem item;
	TranslationEntry entry;
	public Page(PageItem item, TranslationEntry entry)
	{
		this.item = item;
		this.entry = entry;
	}

}