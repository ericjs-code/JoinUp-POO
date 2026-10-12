package br.com.joinup.paineis.http;

import br.com.joinup.auth.SegurancaHttp;
import br.com.joinup.http.*;
import br.com.joinup.paineis.service.PainelService;
import br.com.joinup.usuarios.domain.Perfil;
import java.util.*;

public final class PainelRoutes {
    private final HttpResponses respostas;
    private final PainelService service;
    public PainelRoutes(HttpResponses respostas, PainelService service) { this.respostas=respostas; this.service=service; }
    public void registrar(Router router) {
        for (var perfil : Perfil.values()) router.registrar("GET", "/api/" + perfil.name().toLowerCase(Locale.ROOT) + "/painel",
                (e,b,p) -> respostas.json(e,200,Map.of("dados",service.consultar(SegurancaHttp.token(e),perfil))));
    }
}
