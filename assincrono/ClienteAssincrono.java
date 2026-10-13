package assincrono;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClienteAssincrono {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ExecutorService EXEC = Executors.newCachedThreadPool();

    private static void log(String msg) {
        System.out.println(LocalTime.now().format(F) + " - " + msg);
    }

    private static String segundosDesde(long inicioNano) {
        return String.format("%.1f", (System.nanoTime() - inicioNano) / 1_000_000_000.0);
    }

    // Espera o resultado numa thread de fundo; a thread principal fica livre
    private static CompletableFuture<Object> acompanhar(ServicoAssincronoRemoto servico,
                                                        String ticket, String rotulo) {
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                return servico.obterResultado(ticket);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, EXEC);
        future.whenComplete((resultado, erro) -> {
            if (erro != null) log("Erro na chamada " + rotulo + ": " + erro.getMessage());
            else log("Resultado RPC disponível (" + rotulo + "): " + resultado);
        });
        return future;
    }

    private static void atividadeLocal(int n) throws InterruptedException {
        log("Executando atividade local " + n);
        Thread.sleep(2000);
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "192.168.10.1";
        String nome = args.length > 1 ? args[1] : "CLIENTE";
        int segundos = args.length > 2 ? Integer.parseInt(args[2]) : 10;

        Registry registry = LocateRegistry.getRegistry(host, 1098);
        ServicoAssincronoRemoto servico =
                (ServicoAssincronoRemoto) registry.lookup("ServicoAssincrono");

        System.out.println("===== " + nome + " (RPC ASSÍNCRONO) =====");
        long inicio = System.nanoTime();

        log("Solicitando processamento...");
        String tDemorado = servico.submeterProcessamentoDemorado(segundos);
        log("Requisição assíncrona enviada: processamentoDemorado(" + segundos + ") [ticket " + tDemorado + "]");
        CompletableFuture<Object> futDemorado = acompanhar(servico, tDemorado, "processamentoDemorado");

        String tCalc = servico.submeterCalculo(12, 4, "*");
        log("Requisição assíncrona enviada: calcular(12.0 * 4.0) [ticket " + tCalc + "]");
        CompletableFuture<Object> futCalc = acompanhar(servico, tCalc, "calcular");

        List<Double> valores = List.of(7.5, 3.0, 9.5, 1.0, 4.0);
        String tEst = servico.submeterEstatisticas(valores);
        log("Requisição assíncrona enviada: estatisticasColecao(" + valores + ") [ticket " + tEst + "]");
        CompletableFuture<Object> futEst = acompanhar(servico, tEst, "estatisticasColecao");

        log("Cliente continuou sua execução");
        for (int i = 1; i <= 4; i++) {
            atividadeLocal(i);
        }

        log("Atividades locais terminadas; chamando future.get() do processamento demorado");
        Object resposta = futDemorado.get();
        log("Resultado recebido: " + resposta);
        log("Tempo total: " + segundosDesde(inicio) + " segundos");

        futCalc.get();
        futEst.get();
        log(nome + " finalizado");
        EXEC.shutdown();
    }
}
