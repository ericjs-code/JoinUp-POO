package br.com.joinup.arquivos;

import br.com.joinup.http.HttpException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Set;

/** Diretório privado do processo. Nomes exclusivamente UUID gerados pelo servidor. */
public final class ArmazenamentoLocal {
    private final Path raiz;
    public ArmazenamentoLocal(Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
        if (this.raiz.startsWith(Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()))
            throw new IllegalArgumentException("Armazenamento deve ficar fora do repositório/webroot");
    }
    private Path caminho(String id) throws IOException {
        if (!id.matches("[a-f0-9]{32}")) throw new IOException("ID inválido");
        verificarDiretorio();
        return raiz.resolve(id + ".png");
    }
    private void verificarDiretorio() throws IOException {
        Path atual = raiz.getRoot();
        for (Path parte : raiz) {
            atual = atual.resolve(parte);
            if (Files.isSymbolicLink(atual)) throw new IOException("Link não permitido");
            if (!Files.exists(atual, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(atual);
            if (!Files.isDirectory(atual, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Diretório inválido");
        }
    }
    public void gravar(String id, byte[] b) throws IOException {
        Path arquivo=caminho(id);
        boolean criado=false;
        try (var canal = Files.newByteChannel(arquivo, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS))) {
            criado=true;
            var buffer = ByteBuffer.wrap(b);
            while (buffer.hasRemaining()) canal.write(buffer);
        } catch(IOException e) {
            if(criado)try{Files.deleteIfExists(arquivo);}catch(IOException cleanup){e.addSuppressed(cleanup);}
            throw e;
        }
    }
    public byte[] ler(String id) {
        try (var canal = Files.newByteChannel(caminho(id), Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS))) {
            long size=canal.size();
            if (size<1 || size>MultipartImagem.MAX_IMAGEM) throw new IOException("Tamanho inválido");
            var buffer=ByteBuffer.allocate((int)size);
            while(buffer.hasRemaining()) if(canal.read(buffer)<0) throw new IOException("Arquivo incompleto");
            return buffer.array();
        } catch(IOException e) { throw new HttpException(404,"RECURSO_NAO_ENCONTRADO","Imagem indisponível"); }
    }
    public void apagar(String id) throws IOException { Files.deleteIfExists(caminho(id)); }
}
