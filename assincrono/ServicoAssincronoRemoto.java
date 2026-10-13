package assincrono;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServicoAssincronoRemoto extends Remote {

    // As três operações: retornam imediatamente um ticket
    String submeterCalculo(double a, double b, String operacao) throws RemoteException;

    String submeterProcessamentoDemorado(int segundos) throws RemoteException;

    String submeterEstatisticas(List<Double> valores) throws RemoteException;

    // Consulta sem bloquear
    boolean resultadoPronto(String ticket) throws RemoteException;

    // Bloqueia até o resultado existir e o devolve
    Object obterResultado(String ticket) throws RemoteException;
}
