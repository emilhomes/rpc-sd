package assincrono;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServidorAssincrono {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "192.168.10.1";
        System.setProperty("java.rmi.server.hostname", host);

        ServicoAssincronoImpl impl = new ServicoAssincronoImpl();
        ServicoAssincronoRemoto stub =
                (ServicoAssincronoRemoto) UnicastRemoteObject.exportObject(impl, 1101);

        Registry registry = LocateRegistry.createRegistry(1098);
        registry.rebind("ServicoAssincrono", stub);

        System.out.println("Servidor RPC ASSÍNCRONO no ar em " + host + " (registry 1098, objeto 1101)");
    }
}
