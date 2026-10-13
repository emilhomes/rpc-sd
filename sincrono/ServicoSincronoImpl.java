package sincrono;

import comum.Estatisticas;
import comum.ServicoRemoto;

import java.rmi.RemoteException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServicoSincronoImpl implements ServicoRemoto {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static void log(String msg) {
        System.out.println(LocalTime.now().format(F) + " [" + Thread.currentThread().getName() + "] " + msg);
    }

    @Override
    public double calcular(double a, double b, String operacao) throws RemoteException {
        log("calcular(" + a + " " + operacao + " " + b + ") recebido");
        switch (operacao) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw new RemoteException("Divisão por zero");
                return a / b;
            default:
                throw new RemoteException("Operação inválida: " + operacao);
        }
    }

    @Override
    public String processamentoDemorado(int segundos) throws RemoteException {
        log("processamentoDemorado(" + segundos + "s) INICIADO");
        try {
            Thread.sleep(segundos * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Processamento interrompido");
        }
        log("processamentoDemorado(" + segundos + "s) CONCLUÍDO");
        return "Processamento de " + segundos + "s concluído pela thread " + Thread.currentThread().getName();
    }

    @Override
    public Estatisticas estatisticasColecao(List<Double> valores) throws RemoteException {
        log("estatisticasColecao com " + valores.size() + " valores recebido");
        if (valores.isEmpty()) throw new RemoteException("Coleção vazia");
        double soma = 0, maior = Double.NEGATIVE_INFINITY, menor = Double.POSITIVE_INFINITY;
        for (double v : valores) {
            soma += v;
            maior = Math.max(maior, v);
            menor = Math.min(menor, v);
        }
        return new Estatisticas(soma / valores.size(), maior, menor, valores.size());
    }
}
