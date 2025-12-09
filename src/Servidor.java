import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {
    private static final int puerto = 3068;

    public static void main(String[] args) throws IOException {

        // Seleccionamos el fichero en el que se encuentran los datos y creamos un Buffer para leer los datos de este
        File fichero = new File("src/TodosLosRegistros");

        // Creamos un HashMap que tenga como key: el dominio y como valor: ArrayList<Registros>
        // ya que un dominio como google puede tener varios valores
        HashMap<String, ArrayList<Registro>> registros = new HashMap<>();

        // Rellenamos el HasMap con los datos del txt, el formato de cada línea es: google.com A 172.217.17.4
        if (fichero.exists()) {
            BufferedReader br = new BufferedReader(new FileReader(fichero));
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.trim().split(" ");
                if (partes.length != 3) continue;

                String dominio = partes[0];
                String tipo = partes[1];
                String valor = partes[2];
                Registro registro = new Registro(dominio, tipo, valor);

                // Creamos una entrada si no existe al valor que estamos mirando actualmente.
                registros.putIfAbsent(dominio, new ArrayList<>());
                // Vamos a la entrada a la que queremos añadir el registro y lo ponemos
                registros.get(dominio).add(registro);
            }
            br.close();
        }

        // Conexión del servidor
        ServerSocket server = new ServerSocket(puerto);
        System.out.println("Servidor DNS multihilo esperando conexiones...");

        // ExecutorService para máximo 5 clientes simultáneos, luego con pool.execute(worker) dejara a los otors clientes esperando
        ExecutorService pool = Executors.newFixedThreadPool(5);

        while (true) {
            // Accept Conexión: el servidor espera a que un cliente se conecte
            Socket cliente = server.accept();
            System.out.println("Cliente conectado");

            // Crear Worker: nuevo hilo que gestiona la comunicación con el cliente
            Worker worker = new Worker(cliente, registros, fichero);
            pool.execute(worker);
        }
    }

    // Métodos de respuesta (LOOKUP, REGISTER, LIST) se mantienen igual
    public static String respuestaLookUp(String tipoBuscado, String dominioBuscado, HashMap<String, ArrayList<Registro>> registros) {

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

    public static String respuestaRegister(String dominio, String tipo, String valor, File fichero, HashMap<String, ArrayList<Registro>> registros) {
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

    public static String respuestaList(HashMap<String, ArrayList<Registro>> registros) {
        StringBuilder respuesta = new StringBuilder();

        respuesta.append("150 Inicio listado\n");

        for (Map.Entry<String, ArrayList<Registro>> entry : registros.entrySet()) {
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
