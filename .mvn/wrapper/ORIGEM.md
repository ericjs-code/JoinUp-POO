# Origem do Maven Wrapper

Verificado em 11/10/2026 na Correção 04.1. O JAR versionado é idêntico ao
artefato `io.takari:maven-wrapper:0.5.5` obtido diretamente de Maven Central:

https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.5/maven-wrapper-0.5.5.jar

SHA-256 de **ambos** os arquivos:

`164e026e01338638e3433e4f6efbda52ed9a64a94ced649c3d0bbe02a7c61282`

Comparação feita com `Get-FileHash -Algorithm SHA256` após download HTTPS do
artefato oficial, sem executar o binário antes da conferência. JAR mantido pela
origem confirmada. O wrapper conserva Maven 3.6.3 conforme suas propriedades;
o comando de referência da CI é **mvn**, com Java 21 e Maven do runner.
O perfil MySQL foi validado localmente com Maven 3.9.9. A conferência do JAR
não implica execução de toda a suíte pelo wrapper nem modernização do wrapper.
