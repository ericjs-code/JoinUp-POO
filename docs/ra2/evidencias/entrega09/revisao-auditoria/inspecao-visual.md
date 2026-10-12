# Inspeção visual após auditoria — PR #11

12/10/2026, America/Sao_Paulo. Codex leu integralmente o
[parecer do Tech Lead](https://github.com/ericjs-code/JoinUp-POO/pull/11#issuecomment-6127161117)
e abriu individualmente as seis renderizações finais por rasterização dos
SVGs PlantUML/Graphviz com Sharp, fundo branco e resolução nativa. Esta é a
revisão do head auditado 860492c; as capturas anteriores foram preservadas.

| Captura / dimensão | Conferência visual e semântica |
| --- | --- |
| [Pacotes](pacotes.png), 2128×921 | Nove pastas reais contêm vinte classes e uma interface; dez setas consumidor → dependência. Os nomes/classes são legíveis em resolução nativa; setas distribuídas sem o emaranhado anterior de 25 arestas. Duas setas vermelhas social/mídia e nota curta ACOP-01 visíveis. O acréscimo de elementos aumenta a largura: usar paisagem/tela inteira; não reduzir a miniatura |
| [Componentes](componentes.png), 721×784 | Recorte nuclear com duas conexões de bola/encaixe, círculo no fornecedor e semicírculo no lado consumidor, conferidos visualmente. VerificadorSenha e UsuarioRepository nomeados com operações reais; fornece/requer identificados nos componentes. Navegador/HTTP ficaram no anexo. Sem cruzamento entre os dois contratos |
| [Implantação](implantacao.png), 1770×635 | Um host físico contém Windows e JVM como ambientes de execução. Navegador, JoinUp/HttpServer e MySQL têm símbolo de componente e rótulo de programa/serviço, sem estereótipo de ambiente. JAR/SQL/recursos/PNG/dados usam artefatos; protocolos legíveis. Sem caminho absoluto de teste; 3315 só na legenda da homologação |
| [Cardinalidades](cardinalidades.png), 1747×676 | Título e legenda identificam visão conceitual da persistência e preservação do UML geral obrigatório. Todas as multiplicidades/papéis e notas SQL/serviço conservados, sem diamantes; identidade das dez relações também conferida automaticamente |
| [Pacotes — anexo](pacotes-anexo.png), 1574×2170 | Quatorze pastas reais com elementos e as 62 dependências agregadas. Vista deliberadamente densa para consulta com zoom; rótulo explícito para não usar como lâmina. Margens, título, conteúdo e legenda completos; ACOP-01 exposto. A conferência de arestas usa jdeps e não depende de distinguir 62 trajetórias na miniatura |
| [Componentes — contexto](componentes-anexo.png), 1679×1202 | Contexto do monólito, frontend/navegador, entrada HTTP, seis áreas, persistência/contratos/IO e ACOP-01 preservados. Legenda distingue dependências de consumo do recorte nuclear de bola/encaixe. Consultar com zoom, fora da lâmina simplificada |

As seis imagens finais têm título, nós/elementos, notas e legenda completos,
sem tela de erro ou corte. As vistas de apresentação de pacotes e componentes
foram separadas dos anexos: não se afirma que o anexo de 62 setas seja legível
em projeção reduzida. O equipamento/ensaio real em sala continua pendente.

Rastreabilidade: namespace/caminho de cada elemento e cada seta dos pacotes
conferidos pelo script, contratos Java reconferidos nos fontes, dez associações
de cardinalidade iguais ao head auditado. O indicador visual fornece/requer
não reivindica novos GoF ou novos módulos implantáveis.

Comando de rasterização (runtime Node/Sharp local, sem editar manualmente SVG):

```javascript
await sharp(caminhoSvg).flatten({ background: '#ffffff' }).png().toFile(caminhoPng);
```

O layout foi iterado até estas capturas: notas longas foram removidas da vista
nuclear e as setas de pacote reduzidas, mantendo o grafo completo no anexo.
Esta inspeção não executa V26/V44/V45, não certifica acessibilidade assistiva
e não equivale à aprovação da nova auditoria técnica.
