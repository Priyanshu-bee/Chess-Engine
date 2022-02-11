import java.util.*;

@SuppressWarnings("unchecked")

public class standardTools
{
    private static ArrayList<piece> whitePieces = new ArrayList<>();
    private static ArrayList<piece> blackPieces = new ArrayList<>();

    //White Pieces on board in starting position
    public static ArrayList<piece> whitePieces()
    {
        whitePieces.add(new piece("rook",0,0,0,0));
        whitePieces.add(new piece("knight",1,0,0,1));
        whitePieces.add(new piece("bishop",2,0,0,2));
        whitePieces.add(new piece("queen",3,0,0,3));
        whitePieces.add(new piece("king",4,0,0,4));
        whitePieces.add(new piece("bishop",5,0,0,5));
        whitePieces.add(new piece("knight",6,0,0,6));
        whitePieces.add(new piece("rook",7,0,0,7));
        whitePieces.add(new piece("pawn",0,1,0,8));
        whitePieces.add(new piece("pawn",1,1,0,9));
        whitePieces.add(new piece("pawn",2,1,0,10));
        whitePieces.add(new piece("pawn",3,1,0,11));
        whitePieces.add(new piece("pawn",4,1,0,12));
        whitePieces.add(new piece("pawn",5,1,0,13));
        whitePieces.add(new piece("pawn",6,1,0,14));
        whitePieces.add(new piece("pawn",7,1,0,15));
        return whitePieces;
    }

    //Black pieces on board in starting position
    public static ArrayList<piece> blackPieces()
    {
        blackPieces.add(new piece("rook",0,7,1,0));
        blackPieces.add(new piece("knight",1,7,1,1));
        blackPieces.add(new piece("bishop",2,7,1,2));
        blackPieces.add(new piece("queen",3,7,1,3));
        blackPieces.add(new piece("king",4,7,1,4));
        blackPieces.add(new piece("bishop",5,7,1,5));
        blackPieces.add(new piece("knight",6,7,1,6));
        blackPieces.add(new piece("rook",7,7,1,7));
        blackPieces.add(new piece("pawn",0,6,1,8));
        blackPieces.add(new piece("pawn",1,6,1,9));
        blackPieces.add(new piece("pawn",2,6,1,10));
        blackPieces.add(new piece("pawn",3,6,1,11));
        blackPieces.add(new piece("pawn",4,6,1,12));
        blackPieces.add(new piece("pawn",5,6,1,13));
        blackPieces.add(new piece("pawn",6,6,1,14));
        blackPieces.add(new piece("pawn",7,6,1,15));
        return blackPieces;
    }

    //Squares occupied by whitePieces on board
    public static int[][] whiteBoard()
    {
        int[][] whiteBoard = new int[8][8];
        for(int i=0;i<8;i++) for(int j=0;j<8;j++) whiteBoard[i][j] = -1;
        for(piece p: whitePieces) whiteBoard[p.x][p.y] = p.index;
        return whiteBoard;
    }

    //Squares occupied by blackPieces on board
    public static int[][] blackBoard()
    {
        int[][] blackBoard = new int[8][8];
        for(int i=0;i<8;i++) for(int j=0;j<8;j++) blackBoard[i][j] = -1;
        for(piece p: blackPieces) blackBoard[p.x][p.y] = p.index;
        return blackBoard;
    }

    //Dispacement Vector for straight move
    public static ArrayList<int[]> straightMove()
    {
        ArrayList<int[]> straightMove = new ArrayList<>();
        straightMove.add(new int[]{1,0}); 
        straightMove.add(new int[]{-1,0});
        straightMove.add(new int[]{0,1}); 
        straightMove.add(new int[]{0,-1});
        return straightMove;
    }

    //Displacement Vector for diagonal move
    public static ArrayList<int[]> diagonalMove()
    {
        ArrayList<int[]> diagonalMove = new ArrayList<>();
        diagonalMove.add(new int[]{1,1}); 
        diagonalMove.add(new int[]{-1,-1});
        diagonalMove.add(new int[]{-1,1}); 
        diagonalMove.add(new int[]{1,-1});
        return diagonalMove;
    }

    //Displacement Vector for knight move
    public static ArrayList<int[]> knightMove()
    {
        ArrayList<int[]> knightMove = new ArrayList<>();
        knightMove.add(new int[]{2,1}); 
        knightMove.add(new int[]{2,-1});
        knightMove.add(new int[]{-2,1}); 
        knightMove.add(new int[]{-2,-1});
        knightMove.add(new int[]{1,2}); 
        knightMove.add(new int[]{-1,2});
        knightMove.add(new int[]{1,-2}); 
        knightMove.add(new int[]{-1,-2});
        return knightMove;
    }

    //Index of Pieces that can move Straight in the Line
    public static HashSet<Integer>[] straightIndex()
    {
        HashSet<Integer>[] straightIndex = new HashSet[2];
        straightIndex[0] = new HashSet<>(); straightIndex[1] = new HashSet<>();
        straightIndex[0].add(0); straightIndex[1].add(0);
        straightIndex[0].add(3); straightIndex[1].add(3);
        straightIndex[0].add(7); straightIndex[1].add(7);
        return straightIndex;
    }

    //Index of Pieces that can move Diagonally
    public static HashSet<Integer>[] diagonalIndex()
    {
        HashSet<Integer>[] diagonalIndex = new HashSet[2];
        diagonalIndex[0] = new HashSet<>(); diagonalIndex[1] = new HashSet<>();
        diagonalIndex[0].add(2); diagonalIndex[1].add(2);
        diagonalIndex[0].add(3); diagonalIndex[1].add(3); 
        diagonalIndex[0].add(5); diagonalIndex[1].add(5);
        return diagonalIndex;
    }

    //Index of Pieces that can perform knight moves
    public static HashSet<Integer>[] knightIndex()
    {
        HashSet<Integer>[] knightIndex = new HashSet[2];
        knightIndex[0] = new HashSet<>(); knightIndex[1] = new HashSet<>();
        knightIndex[0].add(1); knightIndex[1].add(1);
        knightIndex[0].add(6); knightIndex[1].add(6);
        return knightIndex;
    }

    //Index of Pieces that can perform pawn moves
    public static HashSet<Integer>[] pawnIndex()
    {
        HashSet<Integer>[] pawnIndex = new HashSet[2];
        pawnIndex[0] = new HashSet<>(); pawnIndex[1] = new HashSet<>();
        for(int i=8;i<16;i++) pawnIndex[0].add(i); 
        for(int i=8;i<16;i++) pawnIndex[1].add(i);
        return pawnIndex;
    }
}