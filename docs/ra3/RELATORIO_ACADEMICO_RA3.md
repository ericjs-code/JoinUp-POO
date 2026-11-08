# JoinUp-POO — Relatório acadêmico RA3

PUCPR · Engenharia de Software · Design de Software

Atividade: Especificação do Projeto — Avaliação Somativa (RA3)

Equipe: **[preencher nomes e identificação acadêmica]** · Turma: **[preencher]**

> Nota editorial, fora do corpo do relatório: este é um texto-base elaborado a partir
> do código e das evidências do projeto. Antes da entrega, os integrantes devem
> confirmar e ajustar as reflexões àquilo que efetivamente vivenciaram, preencher
> a identificação e retirar esta nota da versão acadêmica final. O registro das
> decisões e de suas evidências está em [Validação documental](VALIDACAO_ENTREGA_04.md).

## 1. Escopo e propósito do sistema

O JoinUp organiza informações sobre eventos e a interação de seus participantes
em comunidades. O módulo didático RA3 utiliza sete entidades: GeneroMusical,
Organizador, Participante, Evento, TipoIngresso, Comunidade e Postagem. As seis
associações principais vinculam eventos a organizadores e gêneros, tipos de
ingresso e comunidades a eventos, e postagens a participantes e comunidades.
O domínio permite, por exemplo, cadastrar um evento, definir suas categorias
de ingresso e registrar publicações em sua comunidade.

O módulo reaproveita a base Java do JoinUp-POO, mas funciona de forma independente
da aplicação web anterior. Sua interface é um terminal em Java 21, com persistência
no banco MySQL exclusivo `joinup_poo_ra3`, usando Jakarta Persistence API 3.1.0
e EclipseLink 4.0.4. Todas as sete classes possuem operações reais de cadastro,
consulta, atualização e exclusão. Os registros confirmados permanecem no banco
após encerrar e reiniciar o programa. O escopo é o estudo do mapeamento de objetos
para tabelas relacionais; não inclui autenticação nem compra ou venda de ingressos.

A organização em três camadas separa a apresentação, em `MenuRa3`, os casos de uso,
em `ServicoCrudRa3`, e a persistência, coordenada por `UnidadeTrabalhoRa3` com
EclipseLink e MySQL. A escrita utiliza transações. A exclusão de registros sem
dependentes usa `EntityManager.remove()`, exige confirmação e protege vínculos
existentes. A remoção física foi uma decisão técnica do projeto, pois o enunciado
solicita exclusão sem determinar sua modalidade.

## 2. Reflexões da equipe sobre o desenvolvimento

**Preservação da aplicação anterior.** A base existente utilizava JDBC, com classes
e projeções que não correspondiam diretamente a entidades JPA mutáveis. Introduzir
ORM nessas mesmas classes poderia comprometer o funcionamento já validado. A
decisão foi manter o módulo RA3 em pacotes próprios, com perfil Maven e banco
separados. Essa escolha evidencia que adotar uma tecnologia de persistência exige
considerar os limites da arquitetura existente e verificar também o que precisa
continuar funcionando.

**Correspondência entre objetos e relações.** O mapeamento exigiu distinguir a
referência obrigatória de uma comunidade ao evento da possibilidade de um evento
ainda não possuir comunidade. A FK única em `comunidades.id_evento` materializa
essa relação de zero ou uma comunidade por evento. Nos tipos de ingresso, a FK
pertence a `TipoIngresso.evento`, enquanto `Evento.tiposIngressos` é a coleção
inversa. Os testes de relações e restrições mostram que cardinalidades precisam
ser coerentes entre o diagrama, as anotações e o banco. Também foi necessário
preservar o nascimento em `CHAR(14)` e a UF em `ENUM`, adaptando a representação
Java ao contrato SQL existente.

**Tratamento de datas e horários.** A leitura independente por JDBC identificou
um deslocamento de três horas ao converter datas no ambiente Windows com fuso
UTC−3. A solução foi explicitar UTC no conversor `DataHoraUtcConverter` e na
configuração da conexão, mantendo a precisão de segundos do `DATETIME`. O
aprendizado é que recuperar um objeto pelo mesmo mecanismo que o gravou não basta
para demonstrar que o valor físico está correto: uma verificação independente
pode revelar diferenças ocultas pelo processo de conversão.

**Consistência das operações CRUD.** Gerenciar o ciclo de vida do `EntityManager`,
confirmar transações e desfazer operações após falhas foram cuidados centrais.
O serviço usa um contexto por operação, com rollback em falhas de escrita, e
recusa a remoção de pais com dependentes. Essa proteção é necessária inclusive
onde o SQL mantém exclusão em cascata. Os testes com falha após `flush` e com
operações concorrentes verificam a preservação dos dados. A experiência técnica
registrada reforça que CRUD envolve integridade e tratamento de erros, além de
executar operações isoladas com sucesso.
