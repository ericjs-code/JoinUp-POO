# Verificação de Comunidades/Social — Correção 04.1

Usar Java 21, Maven e **MySQL 8.4.0 isolado**. A aplicação usa `joinup_it` com
somente SELECT/INSERT/UPDATE/DELETE; usuário administrativo separado serve para
schema, fixtures e triggers. Nunca apontar a suíte ao banco PHP ou a produção.
O workflow `.github/workflows/java.yml` já provisiona `mysql:8.4.0` e executa
o perfil completo; inclui automaticamente `ComunidadeMySqlIT`.

No ambiente local previamente provisionado, carregar as credenciais exclusivas
da instância pelo ambiente, sem versioná-las. Exemplo de configuração (senha
DML/admin deve ser fornecida por configuração local):

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA/JoinUp-POO/tools/jdk-21.0.12.1+1"
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
$env:JOINUP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3307/joinup_poo?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=10000'
$env:JOINUP_DB_USER = 'joinup_it'
$env:JOINUP_IT_ADMIN_USER = 'root'
$env:JOINUP_IT_DATABASE = 'instancia-isolada'
# JOINUP_DB_PASSWORD e JOINUP_IT_ADMIN_PASSWORD: configuração secreta local.
mvn --batch-mode --no-transfer-progress -Pmysql-it clean verify
node --check src/main/resources/public/js/comunidades.js
```

As suítes exigem o marcador de instância isolada e versão real do banco. Não
desabilitam FKs, não usam DROP DATABASE/TRUNCATE e não removem fixtures seed.
A suíte social limpa por IDs exatos das próprias contas/eventos e hashes de
suas sessões; cascatas normais do schema removem comentários/curtidas/filiação.
Relatórios: `target/surefire-reports`, `target/failsafe-reports` e
`target/mysql-validation/comunidade041.txt`.

Para verificar o frontend, executar `java -jar target/joinup-poo.jar` com a
configuração DML e abrir `/comunidades.html`. Num evento próprio de teste:

1. Dois participantes compram seus ingressos e entram manualmente.
2. Primeiro publica texto e copia o link; o segundo abre o link e vê o destaque,
   curte e comenta. Testar cópia manual se Clipboard estiver indisponível.
3. Segundo sai; o link mostra metadados e acesso bloqueado até reentrada.
4. Primeiro anuncia seu último ingresso; segundo compra a revenda. A associação
   do primeiro fica inativa e o link não permite ler ou escrever. Histórico
   continua disponível ao segundo, depois de sua entrada manual.
5. Após logout, abrir o mesmo link: sem conteúdo protegido. Tentar APIs diretas:
   mensagens, posts, detalhe e comentários retornam 401 sem sessão.
6. Verificar Anterior/Próxima em listas com mais de 50 registros. Upload não está
   disponível; qualquer referência de imagem enviada à API deve ser rejeitada.

As contagens, capturas e execução efetiva estão em
[CORRECAO_04_1_RESULTADOS.md](CORRECAO_04_1_RESULTADOS.md). O relatório antigo
com 136 unitários não é evidência HTTP/MySQL do módulo social. CI verde não
substitui a auditoria do Tech Lead. **Não fazer merge antes dessa auditoria.**
