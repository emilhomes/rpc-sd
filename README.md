# RPC Síncrono e Assíncrono Multithread (Java RMI) em ambiente virtualizado

Trabalho 2 de Sistemas Distribuídos (UERN).
Autores: NOME 1 e NOME 2

## 1. Visão geral

Três VMs Linux no VirtualBox, em dois segmentos de rede, rodando um serviço RPC em Java RMI com duas versões:

- **RPC síncrono**: o cliente bloqueia até receber a resposta.
- **RPC assíncrono**: o cliente envia a requisição, recebe um ticket, continua trabalhando e coleta o resultado com `CompletableFuture.get()`.

Nas duas versões o servidor é multithread.

## 2. Infraestrutura

| VM | Papel | Interfaces / IP |
|----|-------|-----------------|
| VM1 | Roteador/gateway, servidor DHCP e servidor RPC | `enp0s3` (Rede A, NAT do VirtualBox, 10.0.2.15) e `enp0s8` (Rede B, 192.168.10.1/24) |
| VM2 | Cliente 1 | Rede B, 192.168.10.12 (DHCP) |
| VM3 | Cliente 2 | Rede B, 192.168.10.13 (DHCP) |

- A VM1 roteia a Rede B para a Rede A (`ip_forward = 1`) e faz NAT (`MASQUERADE` na `enp0s3`).
- O DHCP da Rede B é o `isc-dhcp-server` na `enp0s8`.
- Java 25 (JDK) instalado nas três VMs.

Comandos de verificação:

```bash
# VM1
ip -br a
cat /proc/sys/net/ipv4/ip_forward
sudo iptables -t nat -L POSTROUTING -n -v
systemctl status isc-dhcp-server --no-pager

# VM2 e VM3
ip -br a
ip route
ping -c 3 192.168.10.1     # gateway
ping -c 3 <IP da outra VM cliente>
ping -c 3 8.8.8.8          # internet via VM1
ping -c 3 google.com       # DNS
```

## 3. Estrutura do repositório

```
comum/        interface ServicoRemoto e classe Estatisticas (usadas pela versão síncrona)
sincrono/     ServicoSincronoImpl, ServidorSincrono, ClienteSincrono
assincrono/   ServicoAssincronoRemoto, ServicoAssincronoImpl, ServidorAssincrono, ClienteAssincrono
```

## 4. Serviço implementado

Três operações remotas com tempos de processamento diferentes:

1. `calcular(a, b, operacao)`: operação aritmética simples (`+`, `-`, `*`, `/`).
2. `processamentoDemorado(segundos)`: `Thread.sleep` controlado (5 s, 10 s ou mais).
3. `estatisticasColecao(lista)`: média, maior, menor e quantidade de uma coleção de valores.

## 5. Como executar

Em cada VM:

```bash
git clone https://github.com/emilhomes/rpc-sd.git
cd rpc-sd
mkdir -p out
javac -d out comum/*.java sincrono/*.java assincrono/*.java
```

### 5.1 RPC síncrono

```bash
# VM1 (servidor)
java -cp out sincrono.ServidorSincrono 192.168.10.1

# VM2 e VM3 (ao mesmo tempo)
java -cp out sincrono.ClienteSincrono 192.168.10.1 CLIENTE-1 10    # VM2
java -cp out sincrono.ClienteSincrono 192.168.10.1 CLIENTE-2 5     # VM3
```

Portas: registry 1099, objeto remoto 1100. O argumento final é a duração (em segundos) da operação demorada.

### 5.2 RPC assíncrono

```bash
# VM1 (servidor)
java -cp out assincrono.ServidorAssincrono 192.168.10.1

# VM2 e VM3 (ao mesmo tempo)
java -cp out assincrono.ClienteAssincrono 192.168.10.1 CLIENTE-1 10   # VM2
java -cp out assincrono.ClienteAssincrono 192.168.10.1 CLIENTE-2 5    # VM3
```

Portas: registry 1098, objeto remoto 1101.

Observação: o servidor define `java.rmi.server.hostname=192.168.10.1`, pois a VM1 tem duas interfaces e, sem isso, o RMI poderia anunciar o IP do NAT.

## 6. Como funciona

**Síncrono.** O cliente chama o stub RMI e a thread principal fica bloqueada até a resposta. O RMI atende cada chamada em uma thread própria no servidor (o log mostra `RMI TCP Connection(N)-IP`).

**Assíncrono.** O servidor expõe métodos `submeter...`, que guardam o trabalho em um `ExecutorService` (pool fixo de 4 threads, `worker-N`) e devolvem um ticket imediatamente. O resultado é obtido com `obterResultado(ticket)`. No cliente, a espera pelo resultado ocorre em uma thread de fundo (`CompletableFuture`), enquanto a thread principal executa atividades locais.

## 7. Resultados observados

**Síncrono** (CLIENTE-1 com 10 s na VM2, CLIENTE-2 com 5 s na VM3):

- CLIENTE-1: 22:56:07 a 22:56:17, bloqueado por 10,0 s.
- CLIENTE-2: 22:56:09 a 22:56:14, bloqueado por 5,0 s, terminando antes do CLIENTE-1.
- Servidor: duas chamadas demoradas simultâneas, em threads diferentes.

**Assíncrono:**

- CLIENTE-1: requisições enviadas às 23:37:04; as atividades locais 1 a 4 rodaram enquanto a operação de 10 s executava; resultado disponível às 23:37:14.
- CLIENTE-2: requisições enviadas às 23:37:07; resultado da operação de 5 s disponível às 23:37:12. O tempo total de 8,1 s corresponde às 4 atividades locais de 2 s, e não ao processamento remoto.
- Servidor: `processamentoDemorado(10s)` em `worker-4` e `processamentoDemorado(5s)` em `worker-1`, com execuções sobrepostas.

## 8. Análise comparativa

**1. Qual foi a principal diferença observada entre RPC síncrona e assíncrona?**
Na síncrona, a thread do cliente fica bloqueada até a resposta chegar. Na assíncrona, a chamada retorna imediatamente (ticket/future) e o cliente segue executando, buscando o resultado depois. No teste síncrono nada foi impresso entre o envio e a resposta; no assíncrono, o cliente executou atividades locais durante a espera.

**2. O que aconteceu com o Cliente 1 enquanto aguardava uma chamada síncrona de 10 segundos?**
Ficou bloqueado na chamada `processamentoDemorado(10)`, sem executar nada, das 22:56:07 às 22:56:17. O tempo total medido foi 10,0 s.

**3. O que o cliente conseguiu realizar durante uma chamada assíncrona?**
Enviou outras duas requisições (cálculo e estatísticas, cujos resultados chegaram no mesmo segundo) e executou as atividades locais 1 a 4, enquanto a operação de 10 s rodava no servidor. Só depois chamou `future.get()`.

**4. Qual é a vantagem de utilizar um servidor multithread neste cenário?**
Atender vários clientes ao mesmo tempo. Com duas threads, a chamada de 5 s do Cliente 2 terminou às 22:56:14, antes da de 10 s do Cliente 1 (22:56:17), e o servidor ainda respondeu a outras operações curtas enquanto uma longa estava em andamento.

**5. O que aconteceria se o servidor possuísse apenas uma thread e recebesse simultaneamente uma operação de 10 segundos e outra de 5 segundos?**
As requisições seriam processadas em série. A segunda esperaria a primeira terminar: o cliente da operação que ficasse por último receberia a resposta após cerca de 15 s (10 + 5), em vez de 10 s ou 5 s. O tempo total seria a soma, e não o maior dos dois.

**6. RPC assíncrona necessariamente torna o processamento realizado pelo servidor mais rápido? Justifique.**
Não. O trabalho no servidor leva o mesmo tempo (a operação de 10 s levou 10 s nas duas versões). A assíncrona só muda como o cliente espera: ele aproveita esse tempo para outras tarefas em vez de ficar parado.

**7. Qual é a diferença entre assincronicidade no cliente e concorrência no servidor?**
Assincronicidade no cliente é o cliente não bloquear após a chamada. Concorrência no servidor é o servidor tratar várias requisições com sobreposição no tempo (várias threads). São independentes: um cliente assíncrono pode usar um servidor de uma só thread (as requisições ficam em fila), e clientes síncronos podem usar um servidor multithread (como no teste síncrono, em que dois clientes bloqueados foram atendidos ao mesmo tempo).

**8. As duas chamadas RPC foram realmente executadas simultaneamente ou concorrentemente? Explique considerando o ambiente utilizado.**
Foram executadas **concorrentemente**, com sobreposição no tempo (no assíncrono: 23:37:04–23:37:14 e 23:37:07–23:37:12, em `worker-4` e `worker-1`). Se houve execução em **paralelo** de fato depende do hardware: o servidor roda em uma VM do VirtualBox com N vCPU(s), que compartilha os núcleos do host com as outras VMs; o paralelismo real exige mais de um núcleo disponível. Como as operações demoradas usam `Thread.sleep` (a thread fica bloqueada sem consumir CPU), a sobreposição aconteceria mesmo com um único núcleo. Em uma carga de CPU intensiva, o ganho dependeria do número de vCPUs.
