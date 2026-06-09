package org.example.practice2.warehouse;

public enum CommandType {
    GET_PRODUCT_QUANTITY(1),
    DEDUCT_PRODUCTS(2),
    ADD_PRODUCTS(3),
    ADD_GROUP(4),
    ADD_PRODUCT_NAME_TO_GROUP(5),
    SET_PRODUCT_PRICE(6),
    SEARCH_PRODUCTS(7),
    DELETE_PRODUCT(8);

    private final int code;

    CommandType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static CommandType fromCode(int code) {
        for (CommandType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown command code: " + code);
    }
}
