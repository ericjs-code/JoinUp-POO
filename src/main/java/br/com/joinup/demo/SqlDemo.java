package br.com.joinup.demo;

import java.util.*;

/** Lexer restrito para o script confiável empacotado: quotes, escapes e comentários. */
public final class SqlDemo {
    private SqlDemo() { }
    public static List<String> separar(String sql) {
        var lista=new ArrayList<String>(); var atual=new StringBuilder();
        char quote=0; boolean linha=false, bloco=false;
        for(int i=0;i<sql.length();i++) {
            char ch=sql.charAt(i), next=i+1<sql.length()?sql.charAt(i+1):0;
            if(linha) {if(ch=='\n'){linha=false;atual.append(' ');}continue;}
            if(bloco){if(ch=='*' && next=='/'){bloco=false;i++;atual.append(' ');}continue;}
            if(quote!=0){
                atual.append(ch);
                if(ch=='\\' && next!=0){atual.append(next);i++;}
                else if(ch==quote){if(next==quote){atual.append(next);i++;}else quote=0;}
                continue;
            }
            if(ch=='-' && next=='-' && (i+2==sql.length() || Character.isWhitespace(sql.charAt(i+2)))){linha=true;i++;continue;}
            if(ch=='#'){linha=true;continue;}
            if(ch=='/' && next=='*'){bloco=true;i++;continue;}
            if(ch=='\'' || ch=='"' || ch=='`'){quote=ch;atual.append(ch);continue;}
            if(ch==';'){if(!atual.toString().isBlank())lista.add(atual.toString().trim());atual.setLength(0);}
            else atual.append(ch);
        }
        if(quote!=0 || bloco)throw new IllegalArgumentException("Script demo incompleto");
        if(!atual.toString().isBlank())lista.add(atual.toString().trim());
        return List.copyOf(lista);
    }
}
