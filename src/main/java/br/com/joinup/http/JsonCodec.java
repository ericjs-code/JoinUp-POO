package br.com.joinup.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Mapper configurado uma única vez, seguro para compartilhamento entre handlers. */
public final class JsonCodec {
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(com.fasterxml.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .withCoercionConfig(com.fasterxml.jackson.databind.type.LogicalType.Textual, c -> c
                    .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer, com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                    .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float, com.fasterxml.jackson.databind.cfg.CoercionAction.Fail)
                    .setCoercion(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean, com.fasterxml.jackson.databind.cfg.CoercionAction.Fail))
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public byte[] escrever(Object valor) throws JsonProcessingException {
        return mapper.writeValueAsBytes(valor);
    }

    public <T> T ler(byte[] corpo, Class<T> tipo) {
        try {
            T valor = mapper.readValue(corpo, tipo);
            if (valor != null) return valor;
        } catch (java.io.IOException e) {
            // Não expor corpo, nomes internos de classes ou detalhes do parser.
        }
        throw new HttpException(400, "REQUISICAO_INVALIDA", "Corpo JSON inválido");
    }
}
