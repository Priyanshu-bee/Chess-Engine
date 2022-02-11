import java.util.*;

public class piece
{
    int x; int y; int value; int hist;
    String name; int color; int index; 
    public piece(String n,int a,int b,int c,int i)
    {
        name = n;
        x=a; y=b;
        color = c;
        index = i;
        if(n.compareTo("rook")==0) value = 150;
        else if(n.compareTo("knight")==0) value = 90;
        else if(n.compareTo("bishop")==0) value = 90;
        else if(n.compareTo("queen")==0) value = 270;
        else if(n.compareTo("king")==0) value = 810;
        else value = 30;
    }

    public void move(int a, int b, Stack<int[]> en_passant, int s)
    {
        if(s==1) {en_passant.pop(); hist--;}
        else if(name.compareTo("pawn")==0 && b-y==2-4*color) en_passant.push(new int[]{a,b});
        else {en_passant.push(new int[]{-1}); hist++;}
        x=a; y=b;
    }

    public static int[] cord(String p)
    {
        int a; int b;
        //Castling Notation
        if(p.compareTo("O-O-O")==0) p = "o3" ;
        else if(p.compareTo("O-O")==0) p = "o6" ;
        else if(p.charAt(1)=='=') p = ""+p.charAt(0)+p.charAt(2);
        //Pawn Promotion Notation
        if(p.charAt(1)=='R') b = 10;
        else if(p.charAt(1)=='N') b = 11;
        else if(p.charAt(1)=='B') b = 12;
        else if(p.charAt(1)=='Q') b = 13;
        else b = Character.getNumericValue(p.charAt(1))-1;
        //Standard Move Notation
        if(p.charAt(0)=='a') a = 0;
        else if(p.charAt(0)=='b') a = 1;
        else if(p.charAt(0)=='c') a = 2;
        else if(p.charAt(0)=='d') a = 3;
        else if(p.charAt(0)=='e') a = 4;
        else if(p.charAt(0)=='f') a = 5;
        else if(p.charAt(0)=='g') a = 6;
        else if(p.charAt(0)=='h') a = 7;
        else if(p.charAt(0)=='o') a = 8;
        else a = 9;
        return new int[]{a,b};
    }
    
    public static String position(int[] a)
    {
        String out; String k;
        //Pawn Promotion Notation
        if(a[1]==10) k = "=R";
        else if(a[1]==11) k = "=N";
        else if(a[1]==12) k = "=B";
        else if(a[1]==13) k = "=Q";
        else k = ""+(a[1]+1);
        //Standard Move Notation
        if(a[0]==0) out = ""+'a'+k;
        else if(a[0]==1) out = ""+'b'+k;
        else if(a[0]==2) out = ""+'c'+k;
        else if(a[0]==3) out = ""+'d'+k;
        else if(a[0]==4) out = ""+'e'+k;
        else if(a[0]==5) out = ""+'f'+k;
        else if(a[0]==6) out = ""+'g'+k;
        else if(a[0]==7) out = ""+'h'+k;
        else if(a[0]==8) out = ""+'o'+k; 
        else out = "Captured";
        //Castling Notation
        if(out.compareTo("o3")==0) return "O-O-O";
        else if(out.compareTo("o6")==0) return "O-O";
        return out;
    }
}