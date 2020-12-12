package nachos.vm;

public class CodePage
{
	public CodePage(int sectionNum, int offset)
	{
		this.sectionNum = sectionNum;
		this.offset = offset;
	}
	
	public int getSectionNum()
	{
		return sectionNum;		
	}
	
	public int getOffset()
	{
		return offset;		
	}
	private int sectionNum;
	private int offset;
}