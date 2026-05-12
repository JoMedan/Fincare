package Fincare.FincareAppProject.Enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * DB에 저장된 "수입"/"지출" 문자열과 TransactionType enum 사이를 변환한다.
 * 기존 데이터 마이그레이션 없이 enum을 도입할 수 있다.
 */
@Converter(autoApply = true)
public class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {

    @Override
    public String convertToDatabaseColumn(TransactionType type) {
        if (type == null) return null;
        return type.getDisplayName();
    }

    @Override
    public TransactionType convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return TransactionType.from(dbValue);
    }
}
