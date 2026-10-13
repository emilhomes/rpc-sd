package sincrono;

import comum.ServicoRemoto;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServidorSincrono {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "192.168.10.1";

        // Essencial: a VM1 tem duas interfaces; anunciamos o IP da Rede B
        System.setProperty("java.rmi.server.hostname", host);

        ServicoSincronoImpl impl = new ServicoSincronoImpl();
        // Porta fixa (1100) para o objeto remoto: facilita firewall e diagnóstico
        ServicoRemoto stub = (ServicoRemoto) UnicastRemoteObject.exportObject(impl, 1100);

        Registry registry = LocateRegistry.createRegistry(1099);
        registry.rebind("ServicoSincrono", stub);

        System.out.println("Servidor RPC SÍNCRONO no ar em " + host + " (registry 1099, objeto 1100)");
    }
}
