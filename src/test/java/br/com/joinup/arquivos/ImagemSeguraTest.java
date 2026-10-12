package br.com.joinup.arquivos;

import br.com.joinup.http.HttpException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class ImagemSeguraTest {
    public static byte[] png() throws IOException {return raster("png",2,2);}
    public static byte[] raster(String formato,int w,int h) throws IOException {
        var out=new ByteArrayOutputStream(); assertTrue(ImageIO.write(new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB),formato,out));return out.toByteArray();
    }
    public static byte[] multipart(String contexto,long alvo,String nome,String mime,byte[] imagem,String extras) throws IOException {
        var out=new ByteArrayOutputStream();
        out.write(("--JoinUp06\r\nContent-Disposition: form-data; name=\"contexto\"\r\n\r\n"+contexto+"\r\n--JoinUp06\r\nContent-Disposition: form-data; name=\"alvoId\"\r\n\r\n"+alvo+"\r\n--JoinUp06\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\""+nome+"\"\r\nContent-Type: "+mime+"\r\n"+extras+"\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(imagem);out.write("\r\n--JoinUp06--\r\n".getBytes(StandardCharsets.US_ASCII));return out.toByteArray();
    }
    public static final String WEBP="UklGRjgAAABXRUJQVlA4ICwAAABwAQCdASoCAAIAAUAmJaACdAFAAAD+9pWX/uDj/+4OP/7g4/et3SfqfygAAA==";
    @ParameterizedTest @ValueSource(strings={"png","jpeg","webp"}) void decodificaTresFormatosENormaliza(String formato) throws Exception {
        byte[] b=formato.equals("webp")?Base64.getDecoder().decode(WEBP):raster(formato,2,2);
        byte[] n=ImagemSegura.normalizar(new MultipartImagem("evento",1,"foto."+formato,"image/"+formato,b));
        assertEquals(2,ImageIO.read(new ByteArrayInputStream(n)).getWidth()); assertEquals(137,n[0]&255);
    }
    @Test void retiraPayloadAnexadoAoPng() throws Exception {
        var out=new ByteArrayOutputStream();out.write(png());out.write("<script>window.bad=1</script>".getBytes(StandardCharsets.US_ASCII));
        byte[] n=ImagemSegura.normalizar(new MultipartImagem("evento",1,"foto.png","image/png",out.toByteArray()));
        assertFalse(new String(n,StandardCharsets.ISO_8859_1).contains("script"));
    }
    @Test void recusaDimensoesAntesDeDecodificar() throws Exception {
        assertEquals(400,assertThrows(HttpException.class,()->ImagemSegura.normalizar(new MultipartImagem("evento",1,"a.png","image/png",raster("png",4097,1)))).status());
    }
    @Test void multipartNaoConverteBinarioEmTexto() throws Exception {
        byte[] b={0,13,10,(byte)255,(byte)128,1};
        var m=MultipartImagem.ler("multipart/form-data; boundary=\"JoinUp06\"",multipart("postagem",3,"a.png","image/png",b,""));assertArrayEquals(b,m.bytes());
    }
    @ParameterizedTest @ValueSource(strings={"../x.png","file:x.png","data:x.png","x.svg","a.html","x.png.exe","x\r\nX-Hack: a.png"}) void nomesSuspeitos(String nome) throws Exception {
        assertThrows(HttpException.class,()->MultipartImagem.ler("multipart/form-data; boundary=JoinUp06",multipart("evento",1,nome,"image/png",png(),"")));
    }
    @Test void arquivoNaoPodeSerSobrescritoNemLidoPorPathTraversal() throws Exception {
        Path dir=Files.createTempDirectory("joinup-media06-");var a=new ArmazenamentoLocal(dir);String id="a".repeat(32);
        try{a.gravar(id,png());assertThrows(IOException.class,()->a.gravar(id,png()));assertThrows(HttpException.class,()->a.ler("../"));assertArrayEquals(png(),a.ler(id));}
        finally{a.apagar(id);Files.delete(dir);}
    }
    @Test void repositorioNaoPodeSerDiretorioDeUploads() {
        assertThrows(IllegalArgumentException.class,()->new ArmazenamentoLocal(Path.of(System.getProperty("user.dir"),"uploads")));
    }
    @Test void recusaLinkDeArquivoQuandoSistemaPermiteCriarSymlink() throws Exception {
        // Link de arquivo é verificado independentemente da permissão de criação do SO.
        Path dir=Files.createTempDirectory("joinup-nofollow06-");var a=new ArmazenamentoLocal(dir);String id="b".repeat(32);
        Path externo=Files.createTempFile("joinup-alheio06-",".png");Files.write(externo,png());
        boolean criado=false;
        try {
            try {Files.createSymbolicLink(dir.resolve(id+".png"),externo);criado=true;} catch(IOException | UnsupportedOperationException e) {
                // Windows sem privilégio: testa bloqueio de diretório/arquivo não regular.
                Files.createDirectory(dir.resolve(id+".png"));
            }
            assertThrows(HttpException.class,()->a.ler(id));
        } finally {Files.deleteIfExists(dir.resolve(id+".png"));Files.delete(externo);Files.delete(dir);}
    }
}
