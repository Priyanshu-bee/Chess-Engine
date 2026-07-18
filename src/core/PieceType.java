package core;

public enum PieceType {
    PAWN(100, 'p'),
    KNIGHT(300, 'n'),
    BISHOP(300, 'b'),
    ROOK(500, 'r'),
    QUEEN(900, 'q'),
    KING(10000, 'k');

    private final int value;
    private final char symbol;

    PieceType(int value, char symbol) {
        this.value = value;
        this.symbol = symbol;
    }

    public int getValue() {
        return value;
    }

    public char getSymbol() {
        return symbol;
    }

    public static PieceType fromSymbol(char symbol) {
        char lower = Character.toLowerCase(symbol);
        for (PieceType type : values()) {
            if (type.symbol == lower) {
                return type;
            }
        }
        return null;
    }
}
