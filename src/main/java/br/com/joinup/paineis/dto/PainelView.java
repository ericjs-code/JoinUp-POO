package br.com.joinup.paineis.dto;

import br.com.joinup.usuarios.domain.Perfil;
import java.time.Instant;
import java.util.*;

/** Projeção de indicadores, sem entidades de usuário nem documentos pessoais. */
public record PainelView(Perfil perfil, Instant consultadoEm, Map<String, Object> indicadores,
                         Map<String, Long> chamadosPorStatus, List<Map<String, Object>> eventos) {}
