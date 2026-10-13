package assincrono;

import comum.Estatisticas;

import java.rmi.RemoteException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ServicoAssincronoImpl implements ServicoAssincronoRemoto {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AtomicInteger contador = new AtomicInteger(1);
    // Pool de threads que executa o trabalho de verdade
    private final ExecutorService pool =
            Executors.newFixedThreadPool(4, r -> new Thread(r, "worker-" + contador.getAndIncrement()));
    private final Map<String, Future<Object>> tarefas = new ConcurrentHashMap<>();

    private static void log(String msg) {
        System.out.println(LocalTime.now().format(F) + " [" + Thread.currentThread().getName() + "] " + msg);
    }

    private String submeter(String descricao, Callable<Object> trabalho) {
        String ticket = UUID.randomUUID().toString().substring(0, 8);
        log("Requisição recebida: " + descricao + " -> ticket " + ticket + " (retornando imediatamente)");
        Future<Object> futuro = pool.submit(() -> {
            log("[ticket " + ticket + "] execução INICIADA: " + descricao);
            Object resultado = trabalho.call();
            log("[ticket " + ticket + "] execução CONCLUÍDA: " + descricao);
            return resultado;
        });
        tarefas.put(ticket, futuro);
        return ticket;
    }

    @Override
    public String submeterCalculo(double a, double b, String operacao) {
        return submeter("calcular(" + a + " " + operacao + " " + b + ")", () -> {
            switch (operacao) {
                case "+": return a + b;
                case "-": return a - b;
                case "*": return a * b;
                case "/":
                    if (b == 0) throw new IllegalArgumentException("Divisão por zero");
                    return a / b;
                default:
                    throw new IllegalArgumentException("Operação inválida: " + operacao);
            }
        });
    }

    @Override
    public String submeterProcessamentoDemorado(int segundos) {
        return submeter("processamentoDemorado(" + segundos + "s)", () -> {
            Thread.sleep(segundos * 1000L);
            return "Processamento de " + segundos + "s concluído pela thread "
                    + Thread.currentThread().getName();
        });
    }

    @Override
    public String submeterEstatisticas(List<Double> valores) {
        return submeter("estatisticasColecao(" + valores.size() + " valores)", () -> {
            if (valores.isEmpty()) throw new IllegalArgumentException("Coleção vazia");
            double soma = 0, maior = Double.NEGATIVE_INFINITY, menor = Double.POSITIVE_INFINITY;
            for (double v : valores) {
                soma += v;
                maior = Math.max(maior, v);
                menor = Math.min(menor, v);
            }
            return new Estatisticas(soma / valores.size(), maior, menor, valores.size());
        });
    }

    @Override
    public boolean resultadoPronto(String ticket) throws RemoteException {
        Future<Object> futuro = tarefas.get(ticket);
        if (futuro == null) throw new RemoteException("Ticket desconhecido: " + ticket);
        return futuro.isDone();
    }

    @Override
    public Object obterResultado(String ticket) throws RemoteException {
        Future<Object> futuro = tarefas.get(ticket);
        if (futuro == null) throw new RemoteException("Ticket desconhecido: " + ticket);
        try {
            Object resultado = futuro.get();   // espera terminar, se ainda não terminou
            tarefas.remove(ticket);
            log("[ticket " + ticket + "] resultado entregue ao cliente");
            return resultado;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Espera interrompida");
        } catch (ExecutionException e) {
            tarefas.remove(ticket);
            throw new RemoteException("Falha na execução: " + e.getCause().getMessage());
        }
    }
}
