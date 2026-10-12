package br.com.joinup.arquivos;

import br.com.joinup.auth.SegurancaHttp;
import br.com.joinup.http.*;
import br.com.joinup.usuarios.domain.Perfil;
import br.com.joinup.usuarios.service.ContaService;
import java.util.Map;

public final class ArquivoRoutes {
    private final ArquivoService service;
    private final ContaService contas;
    private final SegurancaHttp seguranca;
    private final HttpResponses respostas;
    public ArquivoRoutes(ArquivoService s,ContaService c,SegurancaHttp h,HttpResponses r){service=s;contas=c;seguranca=h;respostas=r;}
    public void registrar(Router router) {
        router.registrarComLimite("POST","/api/arquivos",MultipartImagem.MAX_CORPO,(e,b,p)->{
            contas.identificar(SegurancaHttp.token(e),Perfil.PARTICIPANTE,Perfil.ORGANIZADOR);
            seguranca.exigirCsrf(e);
            var tipos=e.getRequestHeaders().get("Content-Type");
            if(tipos==null || tipos.size()!=1)throw new HttpException(400,"MULTIPART_INVALIDO","Content-Type único obrigatório");
            respostas.json(e,201,Map.of("dados",service.enviar(SegurancaHttp.token(e),MultipartImagem.ler(tipos.getFirst(),b))));
        });
        router.registrar("GET","/api/arquivos/{arquivoId}",(e,b,p)->{
            byte[] imagem=service.ler(SegurancaHttp.token(e),p.caminho().get("arquivoId"));
            e.getResponseHeaders().set("Content-Disposition","inline; filename=\"imagem.png\"");
            respostas.bytes(e,200,"image/png",imagem);
        });
    }
}
