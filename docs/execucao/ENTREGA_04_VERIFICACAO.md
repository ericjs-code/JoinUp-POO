# Verificação da Entrega 04 – Comunidades e Social

## 1. Pré‑requisitos
- **MySQL 8** rodando na máquina local (porta **3306**).
- Usuário **root** sem senha (ou ajuste `JOINUP_DB_PASSWORD` no `launch.json`).
- Banco **`joinup_poo`** já criado e com o schema `src/main/resources/db/schema_v0.2.sql` carregado.

## 2. Como iniciar o banco de testes
```bash
# Caso já tenha o serviço MySQL instalado como Windows Service
net start MySQL80
# ou, usando Docker (mais rápido para ambientes de desenvolvimento)
docker run -d --name joinup-mysql \
  -e MYSQL_ROOT_PASSWORD=\"\" \
  -p 3306:3306 mysql:8.0 \
  --default-authentication-plugin=mysql_native_password
```

### Carregar o schema
```bash
mysql -u root < src/main/resources/db/schema_v0.2.sql
# (opcional) carregar dados de demonstração se existir
# mysql -u root joinup_poo < src/main/resources/db/seed_demo_v0.2.sql
```

## 3. Iniciar a aplicação
```powershell
# Parar eventual instância anterior
# (PID 27268 já foi finalizado anteriormente)

# Iniciar com o Java 21 configurado no launch.json
mvnw.cmd exec:java -Dexec.mainClass=br.com.joinup.Main
```
A aplicação ficará disponível em **http://127.0.0.1:8080**.

## 4. Testes de sanidade (cURL)
```bash
# Lista pública de comunidades (não requer autenticação)
curl -s http://127.0.0.1:8080/api/comunidades

# Endpoints que requerem login – usar a API de login primeiro
# (exemplo simplificado, requer sessão cookie)
# 1) Login como participante (deve retornarr cookie HttpOnly)
# 2) GET detalhes da comunidade com participação
#    curl -b cookies.txt http://127.0.0.1:8080/api/comunidades/1
```
Esperado: JSON com lista vazia (`[]`) ou com objetos `{"id":...}`; nenhum erro *BANCO_INDISPONIVEL*.

## 5. Verificação da UI
Abra no navegador:
- `http://127.0.0.1:8080/comunidades.html`
- Navegue pelas abas **Comunidades**, **Chat**, **Feed**.
- Teste entrar/sair da comunidade, criar post, curtir e comentar.

## 6. Resultado esperado
- Todos os **136 testes** (`mvnw.cmd test`) passam.
- As rotas da comunidade retornam **200** quando autorizadas e **403/401** quando não.
- Não há rotas para edição ou exclusão de postagens (conforme **RF‑SOC‑01**).
- As curtidas são idempotentes e o contador não fica negativo.

## 7. Próximos passos
- Revisão do Tech Lead (PR já aberto na branch `feat/04-comunidades-social`).
- Iniciar a **Entrega 05 – Suporte e Painéis** seguindo o próximo prompt.

---
*Documento criado automaticamente para auditoria da entrega 04.*
