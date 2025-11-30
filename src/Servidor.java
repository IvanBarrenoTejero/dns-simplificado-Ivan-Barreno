import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Servidor {
    private static final int puerto = 3068;

    public static void main(String[] args) throws IOException {

        ServerSocket server = new ServerSocket(puerto);
        Socket cliente = server.accept();
        System.out.println("Cliente conectado");

        BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
        BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(cliente.getOutputStream()));

        File fichero = new File("out/production/dns-simplificado-Ivan-Barreno/TodosLosRegistros");
        BufferedReader br = new BufferedReader(new FileReader(fichero));

        HashMap<String, ArrayList<Registro>> registros = new HashMap<>();
        String linea;

        while ((linea = br.readLine()) != null) {
            String[] partes = linea.trim().split(" ");

            String dominio = partes[0];
            String tipo = partes[1];
            String ip = partes[2];

            Registro registro = new Registro(dominio, tipo, ip);

            registros.putIfAbsent(dominio, new ArrayList<>());
            registros.get(dominio).add(registro);
        }

        String mensajeCliente;

        do {
            mensajeCliente = entrada.readLine();
            if (mensajeCliente == null) break;
            mensajeCliente = mensajeCliente.trim();

            try {
                if (mensajeCliente.equals("EXIT")) {
                    break;
                } else if (mensajeCliente.startsWith("LOOKUP")) {
                    String[] partes = mensajeCliente.split(" ");
                    if (partes.length != 3) {
                        salida.write("400 Bad request\n");
                        salida.flush();
                        continue;
                    }

                    String respuesta = respuestaLookUp(mensajeCliente, registros);
                    if (respuesta.startsWith("No se encontraron") || respuesta.startsWith("Dominio no encontrado")) {
                        salida.write("404 Not Found\n");
                    } else {
                        for (String r : respuesta.split("\n")) {
                            String[] datos = r.split(" ");
                            salida.write("200 " + datos[2] + "\n");
                        }
                    }
                    salida.flush();
                } else {
                    salida.write("400 Bad request\n");
                    salida.flush();
                }
            } catch (Exception e) {
                salida.write("500 Server error\n");
                salida.flush();
            }

        } while (true);

        br.close();
        entrada.close();
        salida.close();
        cliente.close();
        server.close();
    }

    private static String respuestaLookUp(String texto, HashMap<String, ArrayList<Registro>> registros) {
        String[] caracteres = texto.split(" ");

        String tipoBuscado = caracteres[1];
        String dominioBuscado = caracteres[2];

        ArrayList<Registro> registrosDominio = registros.get(dominioBuscado);

        if (registrosDominio == null) {
            return "Dominio no encontrado";
        }

        StringBuilder resultado = new StringBuilder();

        for (Registro registro : registrosDominio) {
            if (registro.getTipo().equals(tipoBuscado)) {
                resultado.append(registro.getDominio())
                        .append(" ")
                        .append(registro.getTipo())
                        .append(" ")
                        .append(registro.getIp())
                        .append("\n");
            }
        }

        if (resultado.isEmpty()) {
            return "No se encontraron registros del tipo " + tipoBuscado;
        }

        return resultado.toString().trim();
    }
}
