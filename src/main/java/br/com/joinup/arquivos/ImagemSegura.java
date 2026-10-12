package br.com.joinup.arquivos;

import br.com.joinup.http.HttpException;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.*;
import java.util.*;

public final class ImagemSegura {
    private ImagemSegura() { }
    /** Decodifica e normaliza para PNG, retirando EXIF, nomes e payloads anexados. */
    public static byte[] normalizar(MultipartImagem m) {
        byte[] b = m.bytes();
        if (b.length == 0 || b.length > MultipartImagem.MAX_IMAGEM) throw invalida();
        String detectado = "";
        if (b.length >= 8 && Arrays.equals(Arrays.copyOf(b,8), new byte[]{(byte)137,80,78,71,13,10,26,10})) detectado="image/png";
        if (b.length >= 3 && b[0]==(byte)255 && b[1]==(byte)216 && b[2]==(byte)255) detectado="image/jpeg";
        if (b.length >= 12 && b[0]==82 && b[1]==73 && b[2]==70 && b[3]==70 && b[8]==87 && b[9]==69 && b[10]==66 && b[11]==80) detectado="image/webp";
        String extensao = m.nome().substring(m.nome().lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        String esperado = switch(extensao) { case "png" -> "image/png"; case "jpg", "jpeg" -> "image/jpeg"; case "webp" -> "image/webp"; default -> ""; };
        if (detectado.isEmpty() || !detectado.equals(m.mime()) || !detectado.equals(esperado)) throw invalida();
        try (var in = new MemoryCacheImageInputStream(new ByteArrayInputStream(b))) {
            var readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw invalida();
            var reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int w=reader.getWidth(0), h=reader.getHeight(0);
                if (w<1 || h<1 || w>4096 || h>4096 || (long)w*h>4000000) throw invalida();
                reader.addIIOReadWarningListener((r, warning) -> { throw invalida(); });
                var imagem = reader.read(0);
                var out = new ByteArrayOutputStream();
                if (imagem == null || !ImageIO.write(imagem, "png", out)) throw invalida();
                if (out.size() > MultipartImagem.MAX_IMAGEM) throw new HttpException(413, "REQUISICAO_MUITO_GRANDE", "Imagem normalizada excede 1 MiB");
                return out.toByteArray();
            } finally { reader.dispose(); }
        } catch (HttpException e) { throw e; }
        catch (IOException | RuntimeException e) { throw invalida(); }
    }
    private static HttpException invalida() { return new HttpException(400, "IMAGEM_INVALIDA", "Formato, conteúdo ou dimensões de imagem inválidos"); }
}
