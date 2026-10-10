package br.com.joinup.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public final class RequestBody {
    private RequestBody() { }

    /** Aplica limite mesmo sem Content-Length (incluindo Transfer-Encoding: chunked). */
    public static byte[] ler(HttpExchange exchange, int limite) throws IOException {
        String tamanho = exchange.getRequestHeaders().getFirst("Content-Length");
        if (tamanho != null) {
            try {
                long declarado = Long.parseLong(tamanho);
                if (declarado < 0) throw new NumberFormatException();
                if (declarado > limite) throw muitoGrande();
            } catch (NumberFormatException e) {
                throw new HttpException(400, "REQUISICAO_INVALIDA", "Tamanho de requisição inválido");
            }
        }
        byte[] corpo = exchange.getRequestBody().readNBytes(limite + 1);
        if (corpo.length > limite) throw muitoGrande();
        return corpo;
    }

    private static HttpException muitoGrande() {
        return new HttpException(413, "REQUISICAO_MUITO_GRANDE", "Corpo excede o limite permitido");
    }
}
