package nachos.vm;

public class PageItem{
	int pid;
	int vpn;
	
	PageItem(int pid, int vpn)
	{
		this.pid = pid;
		this.vpn = vpn;
	}
	
	public boolean equals(Object o)
	{
		if (!(o instanceof PageItem))
			return false;
		else
		{
			if (((PageItem) o).pid == pid && ((PageItem) o).vpn == vpn)
				return true;
			else
				return false;
		}
	}
	
	public int hashCode()
	{
		return pid^2 + vpn^2;
	}

}