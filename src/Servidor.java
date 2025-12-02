import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Servidor {
    private static final int puerto = 3068;

    public static void main(String[] args) throws IOException {

        // Conexión del cliente
        ServerSocket server = new ServerSocket(puerto);
        Socket cliente = server.accept();
        System.out.println("Cliente conectado");

        // Buffers de entrada y salida para la comunicación con el cliente
        BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
        BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(cliente.getOutputStream()));

        // Seleccionamos el fichero en el que se encuentran los datos y creamos un Buffer para leer los datos de este
        File fichero = new File("out/production/dns-simplificado-Ivan-Barreno/TodosLosRegistros");
        BufferedReader br = new BufferedReader(new FileReader(fichero));

        // Creamos un HashMap que tenga como key: el dominio y como valor: ArrayList<Registros> ya que un dominio como google puede tener varios valores
        HashMap<String, ArrayList<Registro>> registros = new HashMap<>();
        String linea;

        // Rellenamos el HasMap con los datos del txt, el formato de cada línea es: google.com A 172.217.17.4
        while ((linea = br.readLine()) != null) {
            String[] partes = linea.trim().split(" ");

            String dominio = partes[0];
            String tipo = partes[1];
            String ip = partes[2];

            Registro registro = new Registro(dominio, tipo, ip);

            // Creamos una entrada si no existe al valor que estamos mirando actualmente. putIfAbsent es perfecto para esto. (El ArrayList que se crea está vacio luego se rellena)
            registros.putIfAbsent(dominio, new ArrayList<>());
            // Vamos a la entrada a la que queremos añadir el registro y lo ponemos.
            registros.get(dominio).add(registro);
        }

        String mensajeCliente;

        do {

            mensajeCliente = entrada.readLine();

            // Necesario hacer una comprobación de que el mensaje no esté vacío para que el split no salte un IndexOutOfBoundException
            mensajeCliente = mensajeCliente.trim();
            if (mensajeCliente.isEmpty()) continue;

            // Cojemos la primera parte de lo que escriba
            String comando = mensajeCliente.split(" ")[0];

            try {
                switch (comando) {

                    case "EXIT" -> {
                        break;
                    }

                    case "LOOKUP" -> {
                        String[] partes = mensajeCliente.split(" "); // Tocamos el mensaje no el comando
                        if (partes.length != 3) {
                            salida.write("400 Bad request\n");
                            salida.flush();
                        } else {
                            String respuesta = respuestaLookUp(mensajeCliente, registros);


                            if (respuesta.startsWith("No se encontraron")
                                    || respuesta.startsWith("Dominio no encontrado")) {
                                salida.write("404 Not Found\n");
                            } else {
                                for (String r : respuesta.split("\n")) {
                                    String[] datos = r.split(" ");
                                    salida.write("200 " + datos[2] + "\n");
                                }
                            }
                            salida.flush();
                        }
                    }

                    case "LIST" -> {
                        if (mensajeCliente.split(" ").length != 1) {
                            salida.write("400 Bad request\n");
                            salida.flush();
                        } else{
                            String respuesta = respuestaList(registros);

                            for (String r : respuesta.split("\n")) {
                                salida.write(r + "\n");

                            }
                        }
                        salida.flush();

                    }

                    default -> {
                        salida.write("400 Bad request\n");
                        salida.flush();
                    }
                }

                if (comando.equals("EXIT")) break;

            } catch (Exception e) {
                salida.write("500 Server error\n");
                salida.flush();
            }

        } while (!mensajeCliente.equals("EXIT"));

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

        StringBuilder respuesta = new StringBuilder();

        for (Registro registro : registrosDominio) {
            if (registro.getTipo().equals(tipoBuscado)) {
                respuesta.append(registro.getDominio())
                        .append(" ")
                        .append(registro.getTipo())
                        .append(" ")
                        .append(registro.getIp())
                        .append("\n");
            }
        }

        if (respuesta.isEmpty()) {
            return "No se encontraron registros del tipo " + tipoBuscado;
        }

        return respuesta.toString().trim();
    }

    private static String respuestaList(HashMap<String, ArrayList<Registro>> registros) {
        StringBuilder respuesta = new StringBuilder();

        respuesta.append("150 Inicio listado\n");

        for (Map.Entry<String, ArrayList<Registro>> entry : registros.entrySet()) { // registros.entry devuelve cada entrada
            ArrayList<Registro> listaActual = entry.getValue();

            for (Registro registroActual : listaActual) {
                respuesta.append(registroActual.getDominio())
                        .append(" ")
                        .append(registroActual.getTipo())
                        .append(" ")
                        .append(registroActual.getIp())
                        .append("\n");

            }
        }

        respuesta.append("226 Fin listado\n");

        return respuesta.toString();
    }
}
