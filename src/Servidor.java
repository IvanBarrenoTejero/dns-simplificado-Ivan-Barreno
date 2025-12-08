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
        File fichero = new File("src/TodosLosRegistros");
        BufferedReader br = new BufferedReader(new FileReader(fichero));

        // Creamos un HashMap que tenga como key: el dominio y como valor: ArrayList<Registros> ya que un dominio como google puede tener varios valores
        HashMap<String, ArrayList<Registro>> registros = new HashMap<>();

        // Rellenamos el HasMap con los datos del txt, el formato de cada línea es: google.com A 172.217.17.4
        String linea;
        while ((linea = br.readLine()) != null) {
            String[] partes = linea.trim().split(" ");

            String dominio = partes[0];
            String tipo = partes[1];
            String ip = partes[2];
            Registro registro = new Registro(dominio, tipo, ip);


            // Creamos una entrada si no existe al valor que estamos mirando actualmente. putIfAbsent es perfecto para esto. (El ArrayList que se crea está vacio luego se rellena)
            registros.putIfAbsent(dominio, new ArrayList<>());
            // Vamos a la entrada a la que queremos añadir el registro y lo ponemos. (En este caso cojemos un ArrayList acordarse)
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
                        String[] partes = mensajeCliente.split(" ");
                        if (partes.length != 3) {
                            salida.write("400 Bad request\n");
                            salida.flush();
                        } else {
                            String tipo = partes[1];
                            String dominio = partes[2];

                            // La respuesta ya incluye "200 ..." o "404 Not Found"
                            String respuesta = respuestaLookUp(tipo, dominio, registros);

                            salida.write(respuesta + "\n");
                            salida.flush();
                        }
                    }

                    case "LIST" -> {
                        if (mensajeCliente.split(" ").length != 1) {
                            salida.write("400 Bad request\n");
                            salida.flush();
                        } else {
                            String respuesta = respuestaList(registros);

                            for (String r : respuesta.split("\n")) {
                                salida.write(r + "\n");

                            }
                        }
                        salida.flush();
                    }

                    case "REGISTER" -> {
                        if (mensajeCliente.split(" ").length != 4) {
                            salida.write("400 Bad request\n");
                            salida.flush();
                        } else {
                            String[] parte = mensajeCliente.split(" ");
                            String respuesta = respuestaRegister(parte[1], parte[2], parte[3], fichero, registros);
                            salida.write(respuesta + "\n");
                            salida.flush();
                        }
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

    private static String respuestaLookUp(String tipoBuscado, String dominioBuscado, HashMap<String, ArrayList<Registro>> registros) {

        ArrayList<Registro> registrosDominio = registros.get(dominioBuscado);

        // Mandar el mensaje 404 si no existe
        if (registrosDominio == null) {
            return "404 Not Found";
        }

        StringBuilder resultado = new StringBuilder();

        for (Registro registro : registrosDominio) {
            if (registro.getTipo().equalsIgnoreCase(tipoBuscado)) {
                resultado.append("200 ").append(registro.getValor()).append("\n");
            }
        }

        // Mandar 404 si no lo encontró
        if (resultado.isEmpty()) {
            return "404 Not Found";
        }

        return resultado.toString().trim();
    }

    private static String respuestaRegister(String dominio, String tipo, String valor, File fichero, HashMap<String, ArrayList<Registro>> registros) {
            /*
            Hacer comprobaciones para saber si está correctamente escrito, uso de expresiones regulares (regex)

            Validación del dominio:
            - [a-zA-Z0-9-]+ -> letras, números o guion, al menos uno
            - [.]           -> un punto literal
            - [a-zA-Z]+     -> solo letras, al menos una
             */

        if (!dominio.matches("[a-zA-Z0-9-]+[.]{1}[a-zA-Z]+")) return ("400 Bad request\n");

        if (tipo.equals("A")) {
            // Valor no vacío y contiene solo dígitos y puntos
            if (!valor.matches("[0-9.]+")) return "400 Bad request\n";

        } else if (tipo.equals("MX") || tipo.equals("CNAME")) {
            // Valor no vacío y contiene letras o guiones o puntos
            if (!valor.matches("[a-zA-Z0-9-.]+")) return "400 Bad request\n";

        } else {
            // Tipo no válido
            return "400 Bad request\n";
        }

        // Una vez está todo correcto añadimos al HasMap y al txt
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fichero, true))) {
            Registro nuevo = new Registro(dominio, tipo, valor);
            registros.putIfAbsent(dominio, new ArrayList<>());
            registros.get(dominio).add(nuevo);

            bw.write(dominio + " " + tipo + " " + valor + "\n");
            return "200 Record added";
        } catch (IOException e) {
            return "500 Error de servidor";
        }
    }

    private static String respuestaList(HashMap<String, ArrayList<Registro>> registros) {
        StringBuilder respuesta = new StringBuilder();

        respuesta.append("150 Inicio listado\n");

        for (Map.Entry<String, ArrayList<Registro>> entry : registros.entrySet()) { // por cada entrada entry, recorre todos los valores del map
            ArrayList<Registro> listaActual = entry.getValue();

            for (Registro registroActual : listaActual) {
                respuesta.append(registroActual.getDominio())
                        .append(" ")
                        .append(registroActual.getTipo())
                        .append(" ")
                        .append(registroActual.getValor())
                        .append("\n");

            }
        }

        respuesta.append("226 Fin listado\n");

        return respuesta.toString();
    }
}
