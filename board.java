import java.util.*;

@SuppressWarnings("unchecked")
//Check en_passant size, however rest works fine
public class board
{
    ArrayList<piece>[] pieces = new ArrayList[2];
    ArrayList<int[][]> all_pcs = new ArrayList<>();
    ArrayList<int[]> str8 = standardTools.straightMove();
    ArrayList<int[]> diag_l = standardTools.diagonalMove();
    ArrayList<int[]> knig_mv = standardTools.knightMove();
    HashSet<Integer>[] str_index = standardTools.straightIndex();
    HashSet<Integer>[] diag_index = standardTools.diagonalIndex();
    HashSet<Integer>[] knig_index = standardTools.knightIndex();
    HashSet<Integer>[] pwn_index = standardTools.pawnIndex();
    Stack<int[]> en_passant = new Stack();

    public board()
    {
        pieces[0] = standardTools.whitePieces(); 
        pieces[1] = standardTools.blackPieces();
        all_pcs.add(standardTools.whiteBoard());
        all_pcs.add(standardTools.blackBoard()); 
        en_passant.push(new int[]{-1});
    }

    public boolean validpos(int i, int j){
        return (i>=0 && i<8 && j>=0 && j<8) ? true : false;
    }

    public void avmove_basic(ArrayList<int[]> out, int i, int j, int[][] o, int[][] yo, int a, int b){
        i+=a; j+=b;
        while(validpos(i,j) && yo[i][j]==-1)
        {
            if(o[i][j]==-1)
            {
                out.add(new int[]{i,j});
                i+=a; j+=b;
            }
            else
            {
                out.add(new int[]{i,j});
                return;
            }
        }
    }

    public void avmove_near(ArrayList<int[]> out, int i, int j, int[][] o, int[][] yo){
        if(validpos(i,j) && yo[i][j]==-1) out.add(new int[]{i,j});
    }

    public void avmove_castle(ArrayList<int[]> out, int[][] o, int[][] yo, int color){
        if(pieces[color].get(4).hist==0 && (pieces[color].get(0).hist==0 || 
           pieces[color].get(7).hist==0) && check(pieces[color].get(4))==-1){
            if(pieces[color].get(0).hist==0 && o[1][7*color]==-1 && yo[1][7*color]==-1) 
                castle_basic(out,2,o,yo,color);
            if(pieces[color].get(7).hist==0) castle_basic(out,5,o,yo,color);
        }
    }

    public ArrayList<int[]> avmove(piece n){
        ArrayList<int[]> out = new ArrayList<>();
        int color = n.color;
        int[][] yo = all_pcs.get(color);
        int[][] o = all_pcs.get(1-color);
        int x = n.x;
        int y = n.y;
        if(y!=9)
        {
            if(n.name.compareTo("rook")==0 || n.name.compareTo("queen")==0)
                for(int[] smove: str8) 
                    avmove_basic(out,x,y,o,yo,smove[0],smove[1]);

            if(n.name.compareTo("knight")==0)
                for(int[] kmove: knig_mv) 
                    avmove_near(out,x+kmove[0],y+kmove[1],o,yo);

            if(n.name.compareTo("bishop")==0 || n.name.compareTo("queen")==0)
                for(int[] dmove: diag_l) 
                    avmove_basic(out,x,y,o,yo,dmove[0],dmove[1]);

            if(n.name.compareTo("king")==0){
                for(int[] smove: str8)
                    avmove_near(out,x+smove[0],y+smove[1],o,yo);
                for(int[] dmove: diag_l)
                    avmove_near(out,x+dmove[0],y+dmove[1],o,yo);
                avmove_castle(out,o,yo,color);
            }

            if(n.name.compareTo("pawn")==0)
            {
                int pwn_mv = 1-2*color; int pawnpos = 1+5*color;
                if(y!=6-5*color)
                {
                    if(validpos(x,y+pwn_mv) && yo[x][y+pwn_mv]==-1 && o[x][y+pwn_mv]==-1){
                        out.add(new int[]{x,y+pwn_mv});
                        if(y==pawnpos && yo[x][y+2*pwn_mv]==-1 && o[x][y+2*pwn_mv]==-1) 
                            out.add(new int[]{x,y+2*pwn_mv});
                    }
                    if(validpos(x-1,y+pwn_mv) && o[x-1][y+pwn_mv]!=-1) out.add(new int[]{x-1,y+pwn_mv});
                    if(validpos(x+1,y+pwn_mv) && o[x+1][y+pwn_mv]!=-1) out.add(new int[]{x+1,y+pwn_mv});
                    if(en_passant.peek()[0]!=-1 && en_passant.peek()[1]==y && Math.abs(en_passant.peek()[0]-x)==1)
                        out.add(new int[]{en_passant.peek()[0],en_passant.peek()[1]});
                }

                else
                {
                    if(validpos(x,y+pwn_mv) && yo[x][y+pwn_mv]==-1 && o[x][y+pwn_mv]==-1)
                        for(int i=10;i<14;i++) out.add(new int[]{x,i});
                    if(validpos(x-1,y+pwn_mv) && o[x-1][y+pwn_mv]!=-1)
                        for(int i=10;i<14;i++) out.add(new int[]{x-1,i});
                    if(validpos(x+1,y+pwn_mv) && o[x+1][y+pwn_mv]!=-1)
                        for(int i=10;i<14;i++) out.add(new int[]{x+1,i});
                }
            }
        }
        return out;
    }

    public void castle_basic(ArrayList<int[]> out, int i, int[][] o, int[][] yo, int color){
        ArrayList<int[]> l_check = new ArrayList();
        l_check.add(new int[]{i,7*color});
        l_check.add(new int[]{i+1,7*color});
        for(int[] l:l_check)
            if(o[l[0]][l[1]]!=-1 || yo[l[0]][l[1]]!=-1 || check(new piece("temp",l[0],l[1],color,-1))!=-1) return;
        out.add(new int[]{8,i});
    }

    public Integer check_basic(int i, int j, int[][] o, int[][] yo, int a, int b, HashSet<Integer> indx){
        i+=a; j+=b;
        while(validpos(i,j) && yo[i][j]==-1)
        {
            if(indx.contains(o[i][j])) return o[i][j];
            if(o[i][j]!=-1) return null;
            i+=a; j+=b;
        }
        return null;
    }

    public Integer check_knight(int i, int j, int[][] o, int[][] yo, HashSet<Integer> indx){
        return (validpos(i,j) && indx.contains(o[i][j])) ? o[i][j] : null;
    }

    public boolean check_king(int i, int j, int[][] o){
        return (validpos(i,j) && o[i][j]==4) ? true : false;
    }

    public int check(piece n){
        int y = n.y;
        int x = n.x;
        if(y!=9)
        {
            int color = n.color;
            int[][] yo = all_pcs.get(color);
            int[][] o = all_pcs.get(1-color);   
            Integer p = null;
            for(int i=0;i<4;i++)
            { 
                p = check_basic(x,y,o,yo,str8.get(i)[0],str8.get(i)[1],str_index[1-color]);
                if(p!=null) return p;
                p = check_basic(x,y,o,yo,diag_l.get(i)[0],diag_l.get(i)[1],diag_index[1-color]);
                if(p!=null) return p;
                if(check_king(x+str8.get(i)[0],y+str8.get(i)[1],o)) return 4;
                if(check_king(x+diag_l.get(i)[0],y+diag_l.get(i)[1],o)) return 4;
            }
            for(int[] kmove: knig_mv)
            { 
                p = check_knight(x+kmove[0],y+kmove[1],o,yo,knig_index[1-color]);
                if(p!=null) return p;
            }
            int pwn_mv = 1-2*color;
            if(validpos(x-1,y+pwn_mv) && pwn_index[color].contains(o[x-1][y+pwn_mv])) return o[x-1][y+pwn_mv];
            if(validpos(x+1,y+pwn_mv) && pwn_index[color].contains(o[x+1][y+pwn_mv])) return o[x+1][y+pwn_mv];
            return -1;
        }
        return 0;
    }

    public void unavail_basic(ArrayList<int[]> out,int i,int j,int[][] o,int[][] yo,int a,int b,HashSet<Integer> indx){
        int h = 0; int[] p = null;
        i+=a; j+=b;
        while(validpos(i,j))
        {
            if(h==1)
            {
                if(indx.contains(o[i][j])) 
                {
                    out.add(p);
                    return;
                }
                if(yo[i][j]!=-1 || o[i][j]!=-1) return;
            }
            if(h==0 && yo[i][j]!=-1)
            {
                h = 1;
                p = new int[]{i,j};
            }
            i+=a;j+=b;
        }   
    }

    public ArrayList<int[]> unavail(int color){
        ArrayList<int[]> out = new ArrayList<>();
        int y = pieces[color].get(4).y;
        int x = pieces[color].get(4).x;
        int[][] yo = all_pcs.get(color);
        int[][] o = all_pcs.get(1-color);
        for(int i=0;i<4;i++){
            unavail_basic(out,x,y,o,yo,str8.get(i)[0],str8.get(i)[1],str_index[1-color]);
            unavail_basic(out,x,y,o,yo,diag_l.get(i)[0],diag_l.get(i)[1],diag_index[1-color]);
        }
        return out;
    }

    public void support_basic(ArrayList<Integer> out,int i,int j,int[][] o,int[][] yo,int a,int b,HashSet<Integer> indx){
        i+=a; j+=b;
        while(validpos(i,j) && yo[i][j]==-1)
        {
            if(indx.contains(o[i][j]))
            { 
                out.add(o[i][j]);
                return;
            }
            if(o[i][j]!=-1) return;
            i+=a; j+=b;
        }
    }

    public void support_knight(ArrayList<Integer> out, int i, int j, int[][] o, int[][] yo,HashSet<Integer> indx){
        if(validpos(i,j) && indx.contains(o[i][j])) out.add(o[i][j]);
    }

    public void support_king(ArrayList<Integer> out, int i, int j, int[][] o){
        if(validpos(i,j) && o[i][j]==4) out.add(4);
    }

    public int support(int a,int b,int color,piece p){
        ArrayList<Integer> use = supportalt(a,b,color);
        int count = 0; if(p!=null) movep(p,9,9,0);
        for(Integer i: use)
        {
            if(i==4) count++;
            else
            {
                piece x = pieces[1-color].get(i);
                int initial1 = x.x; int initial2 = x.y;
                int y0 = all_pcs.get(color)[initial1][initial2];
                int y1 = all_pcs.get(1-color)[initial1][initial2];
                movep(x,a,b,0);
                if(check(pieces[1-color].get(4))==-1) count++;
                movep(x,initial1,initial2,1);
            }
        }
        if(p!=null) movep(p,a,b,1);
        return count;
    }
   
    public ArrayList<Integer> supportalt(int x,int y,int color){
        ArrayList<Integer> out = new ArrayList<Integer>();
        int[][] yo = all_pcs.get(color);
        int[][] o = all_pcs.get(1-color);
        for(int i=0;i<4;i++){
            support_basic(out,x,y,o,yo,str8.get(i)[0],str8.get(i)[1],str_index[1-color]);
            support_basic(out,x,y,o,yo,diag_l.get(i)[0],diag_l.get(i)[1],diag_index[1-color]);
            support_king(out,x+str8.get(i)[0],y+str8.get(i)[1],o);
            support_king(out,x+diag_l.get(i)[0],y+diag_l.get(i)[1],o);
        }
        for(int[] kmove: knig_mv) 
            support_knight(out,x+kmove[0],y+kmove[1],o,yo,knig_index[1-color]);
        int pwn_mv = 1-2*color;
        if(validpos(x-1,y+pwn_mv) && pwn_index[color].contains(o[x-1][y+pwn_mv])) out.add(o[x-1][y+pwn_mv]);
        if(validpos(x+1,y+pwn_mv) && pwn_index[color].contains(o[x+1][y+pwn_mv])) out.add(o[x+1][y+pwn_mv]);
        return out;
   }

    public ArrayList<int[]> possible(piece x){
        if(check(pieces[x.color].get(4))==-1 && x.index!=4)
        {
            ArrayList<int[]> arr_lst = unavail(x.color);
            for(int[] arr: arr_lst) if(arr[0]==x.x && arr[1]==x.y) return possiblealt(x);
            return avmove(x);
        }
        return possiblealt(x);
    }

    public ArrayList<int[]> possiblealt(piece x){
        int color = x.color; int index = x.index;
        piece king = pieces[color].get(4);
        ArrayList<int[]> out = avmove(x);
        int a = x.x; int b = x.y;
        for(int i=0;i<out.size();i++)
        {
            int[] outs = out.get(i);
            piece t = movep(pieces[color].get(index),outs[0],outs[1],0);
            if(check(king)!=-1)
            {
                out.remove(i);
                i--;
            }
            if(outs[1]<10) movep(x,a,b,1);
            else movep(pieces[color].get(index),a,b,2);
            if(t!=null) movep(t,outs[0],outs[1],1);
        }
        return out;
    }
    
    public piece movep_castle(piece x,int a,int b,int special){
        int color = x.color;
        if(x.index==4){
            int sign = (Math.abs(b-4))/(b-4); 
            all_pcs.get(color)[4+2*sign][7*color] = 4;
            all_pcs.get(color)[(int)(3.5*(sign+1))][7*color] = -1;
            all_pcs.get(color)[4+sign][7*color] = (int)(3.5*(sign+1));
            all_pcs.get(color)[4][7*color] = -1; x.move(4+2*sign,7*color,en_passant,0); 
            pieces[color].get((int)(3.5*(sign+1))).move(4+sign,7*color,en_passant,0);
            return pieces[color].get((int)(3.5*(sign+1)));
        }
        else return movep(x,(int)((7*b-14)/3),7*color,special);
    }

    public piece movep_promote(piece x,int a,int b,int special){
        int index = x.index; 
        int color = x.color; 
        String name = x.name;
        if(x.name.compareTo("pawn")==0){
            piece y = movep(x,a,7-7*color,special);
            if(b==10) {name = "rook"; str_index[color].add(index);}
            else if(b==11) {name = "knight"; knig_index[color].add(index);}
            else if(b==12) {name = "bishop"; diag_index[color].add(index);}
            else {name = "queen"; str_index[color].add(index); diag_index[color].add(index);}
            pieces[color].set(index, new piece(name,a,7-7*color,color,index));
            pieces[color].get(index).hist = 6;
            pwn_index[color].remove(index);
            return y;
        }

        else if(b<10){ 
            int pos1 = x.x; int pos2 = x.y; all_pcs.get(color)[a][b]=index;
            pieces[color].set(index, new piece("pawn",a,b,color,index));
            pwn_index[color].add(index); all_pcs.get(color)[pos1][pos2]=-1;
            if(name.compareTo("rook")==0) str_index[color].remove(index);
            else if(name.compareTo("knight")==0) knig_index[color].remove(index);
            else if(name.compareTo("bishop")==0) diag_index[color].remove(index);
            else{
                str_index[color].remove(index);
                diag_index[color].remove(index);
            }
            en_passant.pop();
            return null;
        }

        return movep(x,a,7*color,special);
    }

    public piece movep_enpassant(piece x,int a,int b){
        int pos1 = x.x; int pos2 = x.y; int opp_pwn = 1-2*x.color;
        int j = all_pcs.get(1-x.color)[a][b];
        x.move(a,b+opp_pwn,en_passant,0); piece out = null;
        if(j!=-1){
            out = pieces[1-x.color].get(j);
            out.move(9,9,en_passant,0);
        }
        all_pcs.get(1-x.color)[a][b] = -1;
        all_pcs.get(x.color)[pos1][pos2] = -1;
        all_pcs.get(x.color)[a][b+opp_pwn] = x.index;
        return out;
    }

    public piece movep(piece x,int a,int b,int special){
        
        if(a==8) return movep_castle(x,a,b,special);
        else if(b>=10 || special==2) return movep_promote(x,a,b,special);
        else if(pwn_index[x.color].contains(x.index) && x.y==b) return movep_enpassant(x,a,b);
        int pos1 = x.x; int pos2 = x.y;
        int color = x.color; int i; int index = x.index;
        if(b!=9 && (i=all_pcs.get(1-color)[a][b])!=-1)
        {
            piece p = pieces[1-color].get(i);
            p.move(9,9,en_passant,special); x.move(a,b,en_passant,special);
            update(color,pos1,pos2,a,b,index);
            return p;
        }
        x.move(a,b,en_passant,special);
        update(color,pos1,pos2,a,b,index);
        return null;
    }

    public void update(int color,int pos1,int pos2,int x,int y,int i){
        if(pos2!=9) all_pcs.get(color)[pos1][pos2]=-1;
        if(y!=9)
        {
            all_pcs.get(color)[x][y]=i;
            all_pcs.get(1-color)[x][y]=-1;
        }
    }

    public boolean checkmate(int color){
        if(check(pieces[color].get(4))!=-1)
        {
            for(piece p: pieces[color]) if(possible(p).size()!=0) return false;
            return true;
        }
        return false;
    }

    public boolean draw(int color){
        //Stalemate
        int standard_break = 0;
        if(check(pieces[color].get(4))==-1)
        {
            for(piece p: pieces[color]) if(possible(p).size()!=0) {standard_break = 1; break;}
            if(standard_break!=1) return true;
        }
        //Draw by Insufficient Material
        standard_break = 0; int material_w = 0; int material_b = 0;
        while(standard_break==0)
        {
            //No Pawns
            for(int i:pwn_index[0]) if(pieces[0].get(i).y!=9) {standard_break = 1; break;}
            for(int i:pwn_index[1]) if(pieces[1].get(i).y!=9) {standard_break = 1; break;}
            //No Rooks or Queens 
            for(int i:str_index[0]) if(pieces[0].get(i).y!=9) {standard_break = 1; break;}
            for(int i:str_index[1]) if(pieces[1].get(i).y!=9) {standard_break = 1; break;}
            if(standard_break==1) break;
            //Only one Knight
            for(int i:knig_index[0]) if(pieces[0].get(i).y!=9) material_w++;
            for(int i:knig_index[1]) if(pieces[1].get(i).y!=9) material_b++;
            if(material_w> 1 || material_b>1 || (material_w==1 && material_b==1)) break;
            //Either only one Bishop or two Bishops with both sides having the Bishop of same color
            int w = -1; int b = -1;
            for(int i:diag_index[0]) if(pieces[0].get(i).y!=9) 
                {material_w++; w = pieces[0].get(i).x+pieces[0].get(i).y;}
            for(int i:diag_index[1]) if(pieces[1].get(i).y!=9) 
                {material_b++; b = pieces[1].get(i).x+pieces[1].get(i).y;}
            if(material_w> 1 || material_b>1 || (material_w==1 && material_b==1 && w>=0 && b>=0 && w%2!=b%2)) break;
            return true;
        }
        //Draw by Repetition or Draw by no Capture and no Pawn move
        if(!checkmate(color) && en_passant.size()>750) return true;
        return false;
    }
}