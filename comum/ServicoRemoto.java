package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServicoRemoto extends Remote {

    // Operação 1: rápida
    double calcular(double a, double b, String operacao) throws RemoteException;

    // Operação 2: demorada (sleep controlado, ex.: 5, 10 ou mais segundos)
    String processamentoDemorado(int segundos) throws RemoteException;

    // Operação 3: processamento de coleção
    Estatisticas estatisticasColecao(List<Double> valores) throws RemoteException;
}
