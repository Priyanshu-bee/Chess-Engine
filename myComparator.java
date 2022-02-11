import java.util.Comparator;

public class myComparator implements Comparator<pack>
{
    public int compare(pack a, pack b)
    {
        if(a.val<b.val) return 1;
        if(a.val>b.val) return -1;
        return 0;
    }
}