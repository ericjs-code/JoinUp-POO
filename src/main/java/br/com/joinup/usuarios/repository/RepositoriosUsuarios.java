package br.com.joinup.usuarios.repository;

import br.com.joinup.usuarios.domain.Perfil;
import java.util.Map;

public final class RepositoriosUsuarios {
    private final Map<Perfil, UsuarioRepository> repositorios;
    public RepositoriosUsuarios(UsuarioRepository participante, UsuarioRepository organizador, UsuarioRepository suporte) {
        repositorios = Map.of(Perfil.PARTICIPANTE, participante, Perfil.ORGANIZADOR, organizador, Perfil.SUPORTE, suporte);
    }
    public UsuarioRepository de(Perfil perfil) { return repositorios.get(perfil); }
}
