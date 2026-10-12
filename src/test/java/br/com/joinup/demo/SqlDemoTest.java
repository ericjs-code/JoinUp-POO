package br.com.joinup.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqlDemoTest {
    @Test void naoDivideStringsComentariosOuIdentificadores() {
        var l=SqlDemo.separar("-- ignorar ;\nUSE `joinup_poo`; INSERT INTO `x` VALUES('a;b','it''s','a\\'b'); /* ; */ SELECT 1; # fim ;");
        assertEquals(3,l.size());assertTrue(l.get(1).contains("a;b"));assertEquals("SELECT 1",l.get(2));
    }
    @Test void rejeitaScriptIncompleto(){assertThrows(IllegalArgumentException.class,()->SqlDemo.separar("SELECT 'x"));assertThrows(IllegalArgumentException.class,()->SqlDemo.separar("/* x"));}
}
