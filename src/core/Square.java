package core;

public enum Square {
    A1, B1, C1, D1, E1, F1, G1, H1,
    A2, B2, C2, D2, E2, F2, G2, H2,
    A3, B3, C3, D3, E3, F3, G3, H3,
    A4, B4, C4, D4, E4, F4, G4, H4,
    A5, B5, C5, D5, E5, F5, G5, H5,
    A6, B6, C6, D6, E6, F6, G6, H6,
    A7, B7, C7, D7, E7, F7, G7, H7,
    A8, B8, C8, D8, E8, F8, G8, H8;

    private static final Square[] VALUES = values();

    public int getIndex() {
        return ordinal();
    }

    public static Square fromIndex(int index) {
        if (index < 0 || index >= 64) {
            return null;
        }
        return VALUES[index];
    }

    public static Square fromName(String name) {
        if (name == null || name.length() != 2) {
            return null;
        }
        char file = name.charAt(0);
        char rank = name.charAt(1);
        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            return null;
        }
        int fileIndex = file - 'a';
        int rankIndex = rank - '1';
        return fromIndex(rankIndex * 8 + fileIndex);
    }

    public int getFile() {
        return ordinal() % 8;
    }

    public int getRank() {
        return ordinal() / 8;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
