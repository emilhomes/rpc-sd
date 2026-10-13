package sincrono;

import comum.Estatisticas;
import comum.ServicoRemoto;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClienteSincrono {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static void log(String msg) {
        System.out.println(LocalTime.now().format(F) + " - " + msg);
    }

    private static String segundosDesde(long inicioNano) {
        return String.format("%.1f", (System.nanoTime() - inicioNano) / 1_000_000_000.0);
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "192.168.10.1";
        String nome = args.length > 1 ? args[1] : "CLIENTE";
        int segundos = args.length > 2 ? Integer.parseInt(args[2]) : 10;

        Registry registry = LocateRegistry.getRegistry(host, 1099);
        ServicoRemoto servico = (ServicoRemoto) registry.lookup("ServicoSincrono");

        System.out.println("===== " + nome + " (RPC SÍNCRONO) =====");

        // Operação 1: cálculo simples
        log("Enviando requisição RPC");
        log("Operação solicitada: calcular(12.0 * 4.0)");
        long t1 = System.nanoTime();
        double resultado = servico.calcular(12, 4, "*");
        log("Resposta recebida: " + resultado);
        log("Tempo total: " + segundosDesde(t1) + " segundos");

        // Operação 2: processamento demorado (o cliente BLOQUEIA até a resposta)
        System.out.println();
        log("Solicitando processamento...");
        log("Enviando requisição RPC");
        log("Operação solicitada: processamentoDemorado(" + segundos + ")");
        log("Cliente bloqueado aguardando a resposta...");
        long t2 = System.nanoTime();
        String resposta = servico.processamentoDemorado(segundos);
        log("Resposta recebida: " + resposta);
        log("Processamento concluído");
        log("Tempo total: " + segundosDesde(t2) + " segundos");

        // Operação 3: processamento de coleção
        System.out.println();
        List<Double> valores = List.of(7.5, 3.0, 9.5, 1.0, 4.0);
        log("Enviando requisição RPC");
        log("Operação solicitada: estatisticasColecao(" + valores + ")");
        long t3 = System.nanoTime();
        Estatisticas est = servico.estatisticasColecao(valores);
        log("Resposta recebida: média=" + est.media() + ", maior=" + est.maior()
                + ", menor=" + est.menor() + ", quantidade=" + est.quantidade());
        log("Tempo total: " + segundosDesde(t3) + " segundos");

        System.out.println();
        log(nome + " finalizado");
    }
}
