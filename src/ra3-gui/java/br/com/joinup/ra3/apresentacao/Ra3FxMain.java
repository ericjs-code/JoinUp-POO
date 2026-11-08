package br.com.joinup.ra3.apresentacao;

import br.com.joinup.ra3.persistencia.*;
import br.com.joinup.ra3.servico.*;
import jakarta.persistence.EntityManagerFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.EnumMap;
import java.util.Optional;

/** Uma janela; operações e transações pertencem ao mesmo serviço usado pelo CLI. */
public final class Ra3FxMain extends Application {
    private EntityManagerFactory fabrica;
    private OperacoesRa3 operacoes;
    private Stage janela;
    private final ComboBox<TipoRegistro> entidade = new ComboBox<>();
    private final TableView<OperacoesRa3.Registro> tabela = new TableView<>();
    private final Label paginaAtual = new Label();
    private final Button anterior = new Button("Anterior"), proxima = new Button("Próxima");
    private int pagina = 1;

    public static void main(String[] args) { launch(args); }

    @Override public void start(Stage stage) {
        janela = stage;
        janela.setTitle("JoinUp-POO RA3");
        try {
            fabrica = PersistenciaRa3.abrir(ConfiguracaoRa3.doAmbiente(System.getenv()));
            operacoes = new ServicoCrudRa3(fabrica);
            // Verifica também se o esquema manual permite a primeira leitura.
            operacoes.listar(TipoRegistro.GENERO, 1);
        } catch (RuntimeException e) {
            aviso(Alert.AlertType.ERROR, "Não foi possível iniciar. Confira MySQL, esquema manual e as três variáveis JOINUP_RA3_JDBC_URL, JOINUP_RA3_DB_USER e JOINUP_RA3_DB_PASSWORD.");
            Platform.exit();
            return;
        }
        entidade.getItems().setAll(TipoRegistro.values());
        entidade.setConverter(new StringConverter<>() {
            @Override public String toString(TipoRegistro t) { return t == null ? "" : t.nomeJpa(); }
            @Override public TipoRegistro fromString(String s) { throw new UnsupportedOperationException(); }
        });
        entidade.setValue(TipoRegistro.GENERO);
        entidade.setOnAction(e -> executar(() -> carregar(1)));
        var id = new TableColumn<OperacoesRa3.Registro, Number>("ID");
        id.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().id()));
        id.setPrefWidth(80);
        var resumo = new TableColumn<OperacoesRa3.Registro, String>("Resumo");
        resumo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().resumo()));
        resumo.setPrefWidth(760);
        id.setSortable(false); resumo.setSortable(false);
        tabela.getColumns().add(id); tabela.getColumns().add(resumo);
        tabela.setPlaceholder(new Label("Nenhum registro nesta página."));
        var acoes = new HBox(8,
                botao("Cadastrar", () -> formulario(null)),
                botao("Consultar ID", () -> pedirId(false).ifPresent(n -> mostrar(operacoes.consultar(tipo(), n)))),
                botao("Atualizar", () -> pedirId(true).ifPresent(n -> formulario(operacoes.consultar(tipo(), n)))),
                botao("Excluir", this::excluir), botao("Atualizar lista", () -> carregar(pagina)),
                botao("Sair", janela::close));
        anterior.setOnAction(e -> executar(() -> carregar(pagina - 1)));
        proxima.setOnAction(e -> executar(() -> carregar(pagina + 1)));
        var topo = new HBox(8, new Label("Entidade:"), entidade);
        var rodape = new VBox(10, new HBox(8, anterior, paginaAtual, proxima), acoes);
        var raiz = new BorderPane(tabela, topo, null, rodape, null);
        raiz.setPadding(new Insets(12));
        BorderPane.setMargin(tabela, new Insets(12, 0, 12, 0));
        janela.setScene(new Scene(raiz, 930, 490));
        janela.show();
        executar(() -> carregar(1));
    }
    private TipoRegistro tipo() { return entidade.getValue(); }
    private Button botao(String nome, Runnable acao) {
        var botao = new Button(nome); botao.setOnAction(e -> executar(acao)); return botao;
    }
    private void carregar(int destino) {
        var registros = operacoes.listar(tipo(), destino);
        if (registros.isEmpty() && destino > 1) { carregar(destino - 1); return; }
        tabela.getItems().setAll(registros);
        pagina = destino;
        paginaAtual.setText("Página " + pagina + " · até 20 registros · ID crescente");
        anterior.setDisable(pagina == 1);
        proxima.setDisable(registros.size() < OperacoesRa3.LIMITE);
    }
    private Optional<Integer> pedirId(boolean usarSelecao) {
        var selecionado = tabela.getSelectionModel().getSelectedItem();
        var dialogo = new TextInputDialog(usarSelecao && selecionado != null ? Integer.toString(selecionado.id()) : "");
        dialogo.initOwner(janela); dialogo.setTitle("ID de " + tipo().nomeJpa());
        dialogo.setHeaderText("Informe um ID existente."); dialogo.setContentText("ID:");
        return dialogo.showAndWait().map(FormularioRa3::id);
    }
    private void formulario(OperacoesRa3.Registro atual) {
        var dialogo = new Dialog<ButtonType>();
        dialogo.initOwner(janela);
        dialogo.setTitle((atual == null ? "Cadastrar " : "Atualizar ") + tipo().nomeJpa());
        dialogo.setHeaderText(atual == null ? "ID automático. Use dados fictícios." : "Novos valores para ID " + atual.id() + ". Preencha todos os campos editáveis.");
        var grade = new GridPane(); grade.setHgap(10); grade.setVgap(8); grade.setPadding(new Insets(10));
        var campos = new EnumMap<FormularioRa3.Campo, TextInputControl>(FormularioRa3.Campo.class);
        int linha = 0;
        if (atual != null) {
            var leitura = new TextArea(atual.resumo()); leitura.setEditable(false); leitura.setWrapText(true); leitura.setPrefRowCount(3);
            grade.add(new Label("Valores atuais:"), 0, linha); grade.add(leitura, 1, linha++);
        }
        for (var campo : FormularioRa3.campos(tipo(), atual != null)) {
            TextInputControl entrada;
            if (campo.longo()) { var area = new TextArea(); area.setPrefRowCount(3); area.setWrapText(true); entrada = area; }
            else entrada = new TextField();
            entrada.setPrefWidth(410);
            var rotulo = new Label(campo.rotulo()); rotulo.setLabelFor(entrada);
            grade.add(rotulo, 0, linha); grade.add(entrada, 1, linha++); campos.put(campo, entrada);
        }
        var instrucao = new Label("Consulte os pais no ComboBox/lista antes de cadastrar e digite seus IDs.\nData UTC: 2030-11-12T18:30:00. Imagem é uma referência textual.\nCampos opcionais vazios limpam o valor na atualização.");
        instrucao.setWrapText(true); grade.add(instrucao, 0, linha, 2, 1);
        var scroll = new ScrollPane(grade); scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(Math.min(330, 105 + campos.size() * 45 + (atual == null ? 0 : 100)));
        dialogo.getDialogPane().setContent(scroll);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        // Validação falha sem fechar o formulário nem perder a digitação.
        dialogo.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, evento -> {
            try {
                var valores = new EnumMap<FormularioRa3.Campo, String>(FormularioRa3.Campo.class);
                campos.forEach((c, entrada) -> valores.put(c, entrada.getText()));
                var resultado = atual == null ? operacoes.cadastrar(FormularioRa3.cadastro(tipo(), valores))
                        : operacoes.atualizar(FormularioRa3.alteracao(tipo(), atual.id(), valores));
                dialogo.setResult(ButtonType.OK); dialogo.close();
                aposMutacao("Registro salvo. ID: " + resultado.id());
            } catch (RuntimeException e) { evento.consume(); erro(e); }
        });
        dialogo.showAndWait();
    }
    private void excluir() {
        pedirId(true).ifPresent(n -> {
            var registro = operacoes.consultar(tipo(), n);
            var alerta = new Alert(Alert.AlertType.CONFIRMATION, "Excluir ID " + n + "?\n" + registro.resumo()
                    + "\nRegistros com dependentes serão recusados.", ButtonType.YES, ButtonType.NO);
            alerta.initOwner(janela); alerta.setTitle("Confirmar exclusão"); alerta.setHeaderText("Excluir somente este registro?");
            if (alerta.showAndWait().orElse(ButtonType.NO) == ButtonType.YES
                    && operacoes.excluir(tipo(), n, FormularioRa3.confirmacao(true))) aposMutacao("Registro excluído. ID: " + n);
        });
    }
    private void mostrar(OperacoesRa3.Registro r) { aviso(Alert.AlertType.INFORMATION, "ID: " + r.id() + "\n" + r.resumo()); }
    private void aposMutacao(String mensagem) {
        aviso(Alert.AlertType.INFORMATION, mensagem);
        executar(() -> carregar(pagina));
    }
    private void executar(Runnable acao) { try { acao.run(); } catch (RuntimeException e) { erro(e); } }
    private void erro(RuntimeException e) {
        var nivel = e instanceof FalhaRa3 f && f.motivo() == FalhaRa3.Motivo.NAO_ENCONTRADO
                ? Alert.AlertType.WARNING : Alert.AlertType.ERROR;
        aviso(nivel, e instanceof FalhaRa3 || e instanceof IllegalArgumentException ? e.getMessage()
                : "Não foi possível concluir. Confira o banco e tente atualizar a lista.");
    }
    private void aviso(Alert.AlertType tipo, String mensagem) {
        var alerta = new Alert(tipo); alerta.initOwner(janela); alerta.setTitle("JoinUp-POO RA3"); alerta.setHeaderText(null);
        var texto = new TextArea(mensagem); texto.setEditable(false); texto.setWrapText(true); texto.setPrefRowCount(5);
        alerta.getDialogPane().setContent(texto); alerta.showAndWait();
    }
    @Override public void stop() {
        try { if (fabrica != null && fabrica.isOpen()) fabrica.close(); }
        finally { com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown(); }
    }
}
