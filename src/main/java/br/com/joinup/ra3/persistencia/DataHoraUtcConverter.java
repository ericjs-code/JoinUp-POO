package br.com.joinup.ra3.persistencia;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Evita Timestamp.valueOf(LocalDateTime), que usa implicitamente o fuso da JVM. */
@Converter
public class DataHoraUtcConverter implements AttributeConverter<LocalDateTime, Timestamp> {
    @Override public Timestamp convertToDatabaseColumn(LocalDateTime valorUtc) {
        return valorUtc == null ? null : Timestamp.from(valorUtc.withNano(0).toInstant(ZoneOffset.UTC));
    }

    @Override public LocalDateTime convertToEntityAttribute(Timestamp valor) {
        return valor == null ? null : LocalDateTime.ofInstant(valor.toInstant(), ZoneOffset.UTC).withNano(0);
    }
}
