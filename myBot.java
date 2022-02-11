import java.io.InputStreamReader;
import java.util.*;

@SuppressWarnings("unchecked")

public class myBot
{
    int opp; int you;
    board cb; int count;
    int pc; int[] mv;
    double al;
    public static void main(String[] args)
    {
        myBot me = new myBot();
        Scanner scanner = new Scanner(new InputStreamReader(System.in));

        //Player picks his choice of Color of Pieces
        System.out.println("Choose The Color of Pieces");
        String op = scanner.nextLine();
        if(op.compareTo("white")==0) me.you = 1; 
        else if(op.compareTo("black")==0) me.opp = 1;
        else{
            System.out.println("Invalid color!");
            return;
        }

        //Player Picks the level of AI opponent
        int lvl=-1;
        System.out.println("\nChoose The Level of Computer Opponent");
        String c = scanner.nextLine();
        if(c.compareTo("very easy")==0) lvl=0;
        else if(c.compareTo("easy")==0) lvl=1;
        else if(c.compareTo("medium")==0) lvl=2;
        else if(c.compareTo("hard")==0) lvl=3;
        else if(c.compareTo("very hard")==0) lvl=4;
        else{
            System.out.println("Invalid Value of Level!");
            return;
        }

        //Game Begins
        String kcr = null; String comment; int loser = me.opp;
        if(args.length!=0) comment = args[0];
        else comment = "off";
        if(me.you==0) me.AI_Move_Prompt(comment,lvl);
        while(!me.cb.checkmate(me.opp) && !me.cb.draw(me.opp))
        {
            me.Player_Move_Prompt(scanner);
            if(!me.cb.checkmate(me.you) && !me.cb.draw(me.you)) me.AI_Move_Prompt(comment,lvl);
            else loser = me.you;
        }
        if(me.cb.draw(loser)) System.out.println("\nIt's a Draw!\nGame Over");
        else System.out.println("\n"+((loser==0) ? "White" : "Black")+" loses!\nGame Over");
    }

    public myBot(){
        cb = new board();
    }

    public String AI_Move_Prompt(String args, int lvl)
    {
        String kcr = "";
        long start = System.currentTimeMillis();
        kcr = fmove(lvl,args);
        long finish = System.currentTimeMillis();
        if(args.compareTo("on")==0){
            System.out.println("Time Taken: "+(finish-start)+"ms"); 
            System.out.println("--------------------------------------");
        }
        if(kcr!=null) System.out.println("\nMy Move:\n"+kcr);
        return kcr;
    }

    public void Player_Move_Prompt(Scanner scanner)
    {
        int x = 0;
        while(x==0)
        {
            String a; String i; String f = "";
            System.out.println("\nYour Turn:");
            a = scanner.nextLine();
            i = ""+a.charAt(0)+a.charAt(1);
            for(int c=3;c<a.length();c++) f += a.charAt(c);
            x = oppmove(i,f);
            if(x==0) System.out.println("Invalid Move!\nPlease Choose Different Move");
        }
    }

    public String fmove(int level, String comment)
    {
        ABPruning(level);
        if(comment.compareTo("on")==0){
            System.out.println("\n-----Info-----------------------------");
            System.out.println("Total Moves Calculated: "+count);
            System.out.println("Move Value: "+al);
            System.out.println("Number of Moves in the Game So Far: "+cb.en_passant.size());
            
        }
        String kx = ""+piece.position(new int[]{cb.pieces[you].get(pc).x,cb.pieces[you].get(pc).y})+
                    " "+piece.position(mv);
        piece captured = cb.movep(cb.pieces[you].get(pc),mv[0],mv[1],0);
        return kx;
    }

    public void ABPruning(int depth){
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;
        pc = -1; count = 0;
        MaxFunc(depth,depth,alpha,beta);
    }

    public double MaxFunc(int depth, int lvl, double alpha, double beta){
        if(cb.draw(you)) return 0;
        double out = -10000*(depth+1);
        PriorityQueue<pack> in = queue(you);
        pack successor = in.poll();
        while(successor!=null)
        {
            int x = cb.pieces[you].get(successor.index).x; int y = cb.pieces[you].get(successor.index).y;
            piece p = cb.movep(cb.pieces[you].get(successor.index),successor.des[0],successor.des[1],0);
            out = Math.max(out,MinFunc(depth,lvl,alpha,beta));
            if(successor.des[1]<10) cb.movep(cb.pieces[you].get(successor.index),x,y,1);
            else cb.movep(cb.pieces[you].get(successor.index),x,y,2);
            if(p!=null) cb.movep(p,successor.des[0],successor.des[1],1);
            if(depth!=lvl && out>=beta) return out;
            if(depth==lvl && out>alpha){
                al = out; pc = successor.index; mv = successor.des;
            } 
            alpha = Math.max(alpha,out);
            successor = in.poll();
        }
        return out; 
    }

    public double MinFunc(int depth, int lvl, double alpha, double beta){
        if(cb.draw(opp)) return 0;
        double out = 10000*(depth+1); 
        PriorityQueue<pack> in = queue(opp);
        pack successor = in.poll();
        while(successor!=null)
        {
            int x = cb.pieces[opp].get(successor.index).x; int y = cb.pieces[opp].get(successor.index).y;
            if(depth==0) out = Math.min(out,terminal()-nmval(cb.pieces[opp].get(successor.index),successor.des));
            piece p = cb.movep(cb.pieces[opp].get(successor.index),successor.des[0],successor.des[1],0);
            if(depth!=0) out = Math.min(out,MaxFunc(depth-1,lvl,alpha,beta));
            if(successor.des[1]<10) cb.movep(cb.pieces[opp].get(successor.index),x,y,1);
            else cb.movep(cb.pieces[opp].get(successor.index),x,y,2);
            if(p!=null) cb.movep(p,successor.des[0],successor.des[1],1);
            if(out<=alpha) return out;
            beta = Math.min(beta,out);
            successor = in.poll();
        }
        return out; 
    }

    public double terminal(){
        double val = 0;
        for(piece p :cb.pieces[you])
            if(p.y!=9) val+=p.value;
        for(piece p :cb.pieces[opp])
            if(p.y!=9) val-=p.value;
        return val;
    }

    public PriorityQueue<pack> queue(int clr){
        PriorityQueue<pack> out = new PriorityQueue<pack>(new myComparator());
        for(piece p :cb.pieces[clr])
        {
            ArrayList<int[]> c = cb.possible(p);
            for(int[] d :c) out.add(new pack(p.index,d,-distance(d,new double[]{3.5,3.5})));
        }
        return out;
    }

    public int oppmove(String i,String f){
        int[] i_conv = piece.cord(i);
        int[] f_conv = piece.cord(f);
        int j = cb.all_pcs.get(opp)[i_conv[0]][i_conv[1]];
        if(j!=-1)
        {
            ArrayList<int[]> p_mv = cb.possible(cb.pieces[opp].get(j));
            for(int[] mv:p_mv){
                if(mv[0]==f_conv[0] && mv[1]==f_conv[1]){
                    cb.movep(cb.pieces[opp].get(j),f_conv[0],f_conv[1],0);
                    return 1;
                }
            }
        }
        return 0;
    }

    public double distance(int[] in, double[] fn){
        return Math.sqrt(Math.pow(in[0]-fn[0],2)+Math.pow(in[1]-fn[1],2));
    }

    public double nmval(piece p,int[] f){
        count++; 
        int x = f[0]; int y = f[1];
        double val = -distance(f,new double[]{3.5,3.5});
        if(x==8) return 10;
        if(y>=10) y = 7-7*p.color;
        piece victim = null;
        int a = cb.all_pcs.get(1-p.color)[x][y];
        if(a!=-1) 
        {
            victim = cb.pieces[1-p.color].get(a);
            val += victim.value;
        }
        int os = cb.support(x,y,p.color,victim);
        if(os!=0)
        {
            int ms = cb.support(x,y,1-p.color,victim);
            if(os>=ms) val = val-p.value;
            else if(y>=10){
                if(y==10) val+=120;
                else if(y==13) val+=240;
                else val+=60;
            }
        }
        return val;
    }
}