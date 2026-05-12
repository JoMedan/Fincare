package Fincare.FincareAppProject.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {

    INCOME("수입"),
    EXPENSE("지출");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    /** JSON 직렬화: "수입" / "지출" 으로 출력 (프론트엔드 호환) */
    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    /** JSON 역직렬화: "수입"/"지출" 또는 "INCOME"/"EXPENSE" 모두 허용 */
    @JsonCreator
    public static TransactionType from(String value) {
        for (TransactionType type : values()) {
            if (type.displayName.equals(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("알 수 없는 거래 유형입니다: " + value);
    }

    public boolean isExpense() {
        return this == EXPENSE;
    }

    public boolean isIncome() {
        return this == INCOME;
    }
}
