# Inspeção visual — Entrega 09

12/10/2026, America/Sao_Paulo. Codex inspecionou individualmente os quatro SVGs
gerados pelo PlantUML/Graphviz por rasterização Sharp e abertura dos PNGs em
resolução nativa. Os PNGs abaixo são evidência da vista final, não substituem
as fontes/SVGs vetoriais. Fundo branco na rasterização reproduz o background
do SVG; o renderizador raster não aplica automaticamente o CSS de fundo.

| Diagrama / captura | Dimensão nativa | Conferência visual |
| --- | --- | --- |
| [Pacotes](pacotes.png) | 1339×895 | Dez agrupamentos/namespaces reais, setas tracejadas, ciclo social/mídia com dois sentidos e nota ACOP-01; composition root e legenda visíveis. Cruzamentos de dependências permanecem distinguíveis; rótulo redundante no cruzamento config/auth retirado |
| [Componentes](componentes.png) | 1679×1186 | Notação component UML2, limite do monólito, seis áreas e frontend/entrada; contratos HTTP/Java/JDBC fornecidos/requeridos nomeados e setas concretas diferenciadas. Social/mídia, pasta privada e MySQL visíveis. Orientação ajustada para reduzir largura da primeira vista (2398 px) |
| [Implantação](implantacao.png) | 1076×1256 | Um dispositivo, navegador/JVM/MySQL locais, JAR/recursos/SQL/PNG, protocolos/IO e ordem administrativa legíveis; demo 3315 e HTTP 8080 explícitos; nota do runner e legenda dentro da imagem |
| [Cardinalidades](cardinalidades.png) | 1747×643 | Sete tipos/conceitos, multiplicidades e papéis SQL legíveis; tabelas compras/filiação identificadas; notas distinguem SQL e sucesso dos serviços, sem diamantes. Titular atual e comprador original distintos |

Todas as vistas finais contêm título, margens, notas e legenda completos,
sem recorte de texto, nós ou tela de erro. As relações foram conferidas pelo
sentido/papel, incluindo consumidor → pacote/contrato usado e os dois extremos
das cardinalidades. Os SVGs permitem zoom; usar orientação paisagem para
pacotes/componentes/cardinalidades e retrato para implantação, uma vista por
página. Conferência em projeção real da sala ainda pertence ao ensaio coletivo.

Durante ajuste de layout, uma tentativa de declarar pacote vazio com `{}` na
mesma linha causou erro sintático; declaração multiline corrigida e validação
final reexecutada em todas as fontes. Uma prévia raster transparente apareceu
com fundo preto; foi recomposta em branco para permitir leitura de texto/setas.
As capturas publicadas e os SVGs são as versões finais corrigidas.

Rasterização executada com Node/Sharp disponíveis no runtime local:

```javascript
await sharp(caminhoSvg).flatten({ background: '#ffffff' }).png().toFile(caminhoPng);
```

Essa inspeção não constitui homologação funcional, assistiva ou medição de
legibilidade na sala. Não alterou V26/V44/V45 nem arquivos da Entrega 08.
