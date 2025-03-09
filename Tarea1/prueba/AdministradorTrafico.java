import java.io.*;
import java.net.*;

class AdministradorTrafico {
  static String ipServidor1, ipServidor2;
  static int puertoServidor1, puertoServidor2, puertoLocal;

  static class Worker extends Thread {
    Socket cliente;

    Worker(Socket cliente) {
      this.cliente = cliente;
    }

    public void run() {
      try {
        // Cliente-1 se conecta a Servidor-1
        Socket cliente1 = new Socket(ipServidor1, puertoServidor1);
        new RespuestaServidor(cliente, cliente1, true).start();

        // Cliente-2 se conecta a Servidor-2 (respaldo)
        Socket cliente2 = new Socket(ipServidor2, puertoServidor2);
        new RespuestaServidor(cliente, cliente2, false).start();

        InputStream entradaCliente = cliente.getInputStream();
        OutputStream salidaServidor1 = cliente1.getOutputStream();
        OutputStream salidaServidor2 = cliente2.getOutputStream();

        byte[] buffer = new byte[4096];
        int n;
        while ((n = entradaCliente.read(buffer)) != -1) {
          salidaServidor1.write(buffer, 0, n);
          salidaServidor1.flush();

          salidaServidor2.write(buffer, 0, n);
          salidaServidor2.flush();
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (cliente != null)
            cliente.close();
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
    }
  }

  static class RespuestaServidor extends Thread {
    Socket cliente, servidor;
    boolean enviarAlNavegador;

    RespuestaServidor(Socket cliente, Socket servidor, boolean enviarAlNavegador) {
      this.cliente = cliente;
      this.servidor = servidor;
      this.enviarAlNavegador = enviarAlNavegador;
    }

    public void run() {
      try {
        InputStream entradaServidor = servidor.getInputStream();
        OutputStream salidaCliente = cliente.getOutputStream();

        byte[] buffer = new byte[4096];
        int n;
        while ((n = entradaServidor.read(buffer)) != -1) {
          if (enviarAlNavegador) {
            salidaCliente.write(buffer, 0, n);
            salidaCliente.flush();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (servidor != null)
            servidor.close();
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 5) {
      System.err.println(
          "Uso: java AdministradorTrafico <ip-servidor1> <puerto-servidor1> <ip-servidor2> <puerto-servidor2>");
      System.exit(1);
    }

    puertoLocal = 8080;
    ipServidor1 = args[1];
    puertoServidor1 = Integer.parseInt(args[2]);
    ipServidor2 = args[3];
    puertoServidor2 = Integer.parseInt(args[4]);

    System.out.println("Proxy en puerto: " + puertoLocal);
    System.out.println("Servidor-1 en " + ipServidor1 + ":" + puertoServidor1);
    System.out.println("Servidor-2 en " + ipServidor2 + ":" + puertoServidor2);

    ServerSocket serverSocket = new ServerSocket(puertoLocal);
    for (;;) {
      Socket cliente = serverSocket.accept();
      new Worker(cliente).start();
    }
  }
}
