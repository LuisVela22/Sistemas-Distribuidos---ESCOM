import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import java.net.ServerSocket;

public class AdministradorTraficoSSL {

  static String host_remoto_1;
  static int puerto_remoto_1;
  static String host_remoto_2;
  static int puerto_remoto_2;
  static int puerto_local;

  static class Worker_1 extends Thread {
    Socket cliente_1, servidor_1, servidor_2;

    Worker_1(Socket cliente_1) {
      this.cliente_1 = cliente_1;
    }

    public void run() {
      try {
        servidor_1 = new Socket(host_remoto_1, puerto_remoto_1);
        servidor_2 = new Socket(host_remoto_2, puerto_remoto_2);

        new Worker_2(cliente_1, servidor_1).start();

        InputStream entrada_1 = cliente_1.getInputStream();
        OutputStream salida_1 = servidor_1.getOutputStream();
        OutputStream salida_2 = servidor_2.getOutputStream();
        byte[] buffer = new byte[1024];
        int n;

        while ((n = entrada_1.read(buffer)) != -1) {
          salida_1.write(buffer, 0, n);
          salida_1.flush();

          salida_2.write(buffer, 0, n);
          salida_2.flush();
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (cliente_1 != null) cliente_1.close();
          if (servidor_1 != null) servidor_1.close();
          if (servidor_2 != null) servidor_2.close();
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
    }
  }

  static class Worker_2 extends Thread {
    Socket cliente_1, servidor_1;

    Worker_2(Socket cliente_1, Socket servidor_1) {
      this.cliente_1 = cliente_1;
      this.servidor_1 = servidor_1;
    }

    public void run() {
      try {
        InputStream entrada_1 = servidor_1.getInputStream();
        OutputStream salida_1 = cliente_1.getOutputStream();
        byte[] buffer = new byte[4096];
        int n;

        while ((n = entrada_1.read(buffer)) != -1) {
          salida_1.write(buffer, 0, n);
          salida_1.flush();
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (cliente_1 != null) cliente_1.close();
          if (servidor_1 != null) servidor_1.close();
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 5) {
      System.err.println("Uso:\njava AdministradorTraficoSSL <host-remoto-1> <puerto-remoto-1> <host-remoto-2> <puerto-remoto-2> <puerto-local>");
      System.exit(1);
    }

    host_remoto_1 = args[0];
    puerto_remoto_1 = Integer.parseInt(args[1]);
    host_remoto_2 = args[2];
    puerto_remoto_2 = Integer.parseInt(args[3]);
    puerto_local = Integer.parseInt(args[4]);

    System.setProperty("javax.net.ssl.keyStore", "keystore_servidor.jks");
    System.setProperty("javax.net.ssl.keyStorePassword", "1234567");

    SSLServerSocketFactory socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    SSLServerSocket serverSocket = (SSLServerSocket) socket_factory.createServerSocket(puerto_local);

    System.out.println("Conectando a " + host_remoto_1 + ":" + puerto_remoto_1 + " y " + host_remoto_2 + ":" + puerto_remoto_2 + " en puerto local (SSL) " + puerto_local);

    while (true) {
      Socket cliente_1 = serverSocket.accept();
      new Worker_1(cliente_1).start();
    }
  }
}
