package core;

public class BitboardUtils {
    public static final long[] FILE_MASKS = new long[8];
    public static final long[] RANK_MASKS = new long[8];

    public static final long FILE_A = 0x0101010101010101L;
    public static final long FILE_H = 0x8080808080808080L;
    public static final long FILE_AB = 0x0303030303030303L;
    public static final long FILE_GH = 0xC0C0C0C0C0C0C0C0L;

    public static final long[] KNIGHT_ATTACKS = new long[64];
    public static final long[] KING_ATTACKS = new long[64];
    // Index 0 = WHITE, Index 1 = BLACK
    public static final long[][] PAWN_ATTACKS = new long[2][64];

    static {
        // Initialize file and rank masks
        for (int i = 0; i < 8; i++) {
            FILE_MASKS[i] = FILE_A << i;
            RANK_MASKS[i] = 0xFFL << (i * 8);
        }

        // Initialize attack tables
        for (int sq = 0; sq < 64; sq++) {
            KNIGHT_ATTACKS[sq] = calculateKnightAttacks(sq);
            KING_ATTACKS[sq] = calculateKingAttacks(sq);
            PAWN_ATTACKS[0][sq] = calculatePawnAttacks(sq, 0); // White
            PAWN_ATTACKS[1][sq] = calculatePawnAttacks(sq, 1); // Black
        }
    }

    private static long calculateKnightAttacks(int sq) {
        long attacks = 0L;
        int file = sq % 8;
        int rank = sq / 8;

        int[] df = {1, 2, 2, 1, -1, -2, -2, -1};
        int[] dr = {2, 1, -1, -2, -2, -1, 1, 2};

        for (int i = 0; i < 8; i++) {
            int nf = file + df[i];
            int nr = rank + dr[i];
            if (nf >= 0 && nf < 8 && nr >= 0 && nr < 8) {
                attacks |= (1L << (nr * 8 + nf));
            }
        }
        return attacks;
    }

    private static long calculateKingAttacks(int sq) {
        long attacks = 0L;
        int file = sq % 8;
        int rank = sq / 8;

        int[] df = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dr = {1, 1, 0, -1, -1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int nf = file + df[i];
            int nr = rank + dr[i];
            if (nf >= 0 && nf < 8 && nr >= 0 && nr < 8) {
                attacks |= (1L << (nr * 8 + nf));
            }
        }
        return attacks;
    }

    private static long calculatePawnAttacks(int sq, int color) {
        long attacks = 0L;
        int file = sq % 8;
        int rank = sq / 8;

        if (color == 0) { // White
            if (rank < 7) {
                if (file > 0) attacks |= (1L << ((rank + 1) * 8 + (file - 1)));
                if (file < 7) attacks |= (1L << ((rank + 1) * 8 + (file + 1)));
            }
        } else { // Black
            if (rank > 0) {
                if (file > 0) attacks |= (1L << ((rank - 1) * 8 + (file - 1)));
                if (file < 7) attacks |= (1L << ((rank - 1) * 8 + (file + 1)));
            }
        }
        return attacks;
    }

    // Sliding piece attack generation (Ray-casting)
    public static long getRookAttacks(int sq, long occupied) {
        long attacks = 0L;
        int file = sq % 8;
        int rank = sq / 8;

        // North
        for (int r = rank + 1; r < 8; r++) {
            int target = r * 8 + file;
            attacks |= (1L << target);
            if ((occupied & (1L << target)) != 0) break;
        }
        // South
        for (int r = rank - 1; r >= 0; r--) {
            int target = r * 8 + file;
            attacks |= (1L << target);
            if ((occupied & (1L << target)) != 0) break;
        }
        // East
        for (int f = file + 1; f < 8; f++) {
            int target = rank * 8 + f;
            attacks |= (1L << target);
            if ((occupied & (1L << target)) != 0) break;
        }
        // West
        for (int f = file - 1; f >= 0; f--) {
            int target = rank * 8 + f;
            attacks |= (1L << target);
            if ((occupied & (1L << target)) != 0) break;
        }

        return attacks;
    }

    public static long getBishopAttacks(int sq, long occupied) {
        long attacks = 0L;
        int file = sq % 8;
        int rank = sq / 8;

        // NE
        for (int i = 1; file + i < 8 && rank + i < 8; i++) {
            int target = (rank + i) * 8 + (file + i);
            attacks |= (1L << target);
            if ((occupied & (1L << target)) != 0) break;
        }
        // SE
        for (int i = 1; file + i < 8 && rank - i >= 0; i++) {
            int target = (rank - i) * 8 + (file + i);
            attacks |= (1L << target);
            if ((occupied & (1L << target)) != 0) break;
        }
        // NW
        for (int i = 1; file - i >= 0 && rank + i < 8; i++) {
            int target = (rank + i) * 8 + (file - i);
            attacks |= (1L << target);
            if ((occupied & (1L << target)) != 0) break;
        }
        // SW
        for (int i = 1; file - i >= 0 && rank - i >= 0; i++) {
            int target = (rank - i) * 8 + (file - i);
            attacks |= (1L << target);
            if ((occupied & (1L << target)) != 0) break;
        }

        return attacks;
    }

    public static long getQueenAttacks(int sq, long occupied) {
        return getRookAttacks(sq, occupied) | getBishopAttacks(sq, occupied);
    }
}
