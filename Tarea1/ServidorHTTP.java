import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

class ServidorHTTP {
  static class Worker extends Thread {
    Socket conexion;
    private static final String LAST_MODIFIED = "Fri, 08 Mar 2024 12:00:00 GMT";

    Worker(Socket conexion) {
      this.conexion = conexion;
    }

    int valor(String parametros, String variable) throws Exception {
      String[] p = parametros.split("&");
      for (String s : p) {
        String[] pair = s.split("=");
        if (pair[0].equals(variable))
          return Integer.parseInt(pair[1]);
      }
      throw new Exception("Se espera la variable: " + variable);
    }

    public void run() {
      try {
        BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        PrintWriter salida = new PrintWriter(conexion.getOutputStream());

        String req = entrada.readLine();
        System.out.println("Petición: " + req);

        boolean notModified = false;
        for (;;) {
          String encabezado = entrada.readLine();
          System.out.println("Encabezado: " + encabezado);
          if (encabezado.startsWith("If-Modified-Since")) {
            String date = encabezado.split(": ")[1];
            if (date.equals(LAST_MODIFIED)) {
              notModified = true;
            }
          }
          if (encabezado.equals(""))
            break;
        }

        if (notModified) {
          salida.println("HTTP/1.1 304 Not Modified");
          salida.println("Connection: close");
          salida.println();
          salida.flush();
        } else if (req.startsWith("GET / ")) {
          String respuesta = "<html>" +
              "<script>" +
              "function get(req,callback){" +
              "const xhr = new XMLHttpRequest();" +
              "xhr.open('GET', req, true);" +
              "xhr.onload=function(){" +
              "if (callback != null) callback(xhr.status,xhr.response);" +
              "};" +
              "xhr.send();" +
              "}" +
              "</script>" +
              "<body>" +
              "<button onclick=\"get('/suma?a=1&b=2&c=3',function(status,response){alert(status + ' ' + response);})\">Aceptar</button>"
              +
              "</body>" +
              "</html>";
          salida.println("HTTP/1.1 200 OK");
          salida.println("Content-type: text/html; charset=utf-8");
          salida.println("Content-length: " + respuesta.length());
          salida.println("Last-Modified: " + LAST_MODIFIED);
          salida.println("Connection: close");
          salida.println();
          salida.println(respuesta);
          salida.flush();
        } else if (req.startsWith("GET /suma?")) {
          String parametros = req.split(" ")[1].split("\\?")[1];
          String respuesta = String.valueOf(valor(parametros, "a") + valor(parametros, "b") + valor(parametros, "c"));
          salida.println("HTTP/1.1 200 OK");
          salida.println("Access-Control-Allow-Origin: *");
          salida.println("Content-type: text/plain; charset=utf-8");
          salida.println("Content-length: " + respuesta.length());
          salida.println("Last-Modified: " + LAST_MODIFIED);
          salida.println("Connection: close");
          salida.println();
          salida.println(respuesta);
          salida.flush();
        } else {
          salida.println("HTTP/1.1 404 File Not Found");
          salida.flush();
        }
      } catch (Exception e) {
        System.err.println("Error en la conexión: " + e.getMessage());
      } finally {
        try {
          conexion.close();
        } catch (Exception e) {
          System.err.println("Error en close: " + e.getMessage());
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    ServerSocket servidor = new ServerSocket(8085);
    System.out.println("Servidor HTTP en el puerto 8085");

    for (;;) {
      Socket conexion = servidor.accept();
      new Worker(conexion).start();
    }
  }
}