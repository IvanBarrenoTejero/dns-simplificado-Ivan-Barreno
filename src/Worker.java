import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Worker implements Runnable {
    private final Socket cliente;
    private final HashMap<String, ArrayList<Registro>> registros;
    private final File fichero;

    public Worker(Socket cliente, HashMap<String, ArrayList<Registro>> registros, File fichero) {
        this.cliente = cliente;
        this.registros = registros;
        this.fichero = fichero;
    }

    @Override
    public void run() {
        try (
                // Buffers de entrada y salida para la comunicación con el cliente
                BufferedReader entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(cliente.getOutputStream()))
        ) {
            String mensajeCliente;
            while ((mensajeCliente = entrada.readLine()) != null) {
                // Necesario hacer una comprobación de que el mensaje no esté vacío
                mensajeCliente = mensajeCliente.trim();
                if (mensajeCliente.isEmpty()) continue;

                String[] partes = mensajeCliente.split(" ");
                String comando = partes[0];

                try {
                    switch (comando) {
                        case "EXIT" -> {
                            return; // finaliza hilo
                        }

                        case "LOOKUP" -> {
                            if (partes.length != 3) {
                                salida.write("400 Bad request\n");
                            } else {
                                String tipo = partes[1];
                                String dominio = partes[2];

                                // La respuesta ya incluye "200 ..." o "404 Not Found"
                                String respuesta = Servidor.respuestaLookUp(tipo, dominio, registros);
                                salida.write(respuesta + "\n");
                            }
                            salida.flush();
                        }

                        case "LIST" -> {
                            if (partes.length != 1) {
                                salida.write("400 Bad request\n");
                            } else {
                                String respuesta = Servidor.respuestaList(registros);
                                for (String r : respuesta.split("\n")) {
                                    salida.write(r + "\n");
                                }
                            }
                            salida.flush();
                        }

                        case "REGISTER" -> {
                            if (partes.length != 4) {
                                salida.write("400 Bad request\n");
                            } else {
                                String respuesta = Servidor.respuestaRegister(partes[1], partes[2], partes[3], fichero, registros);
                                salida.write(respuesta + "\n");
                            }
                            salida.flush();
                        }

                        default -> {
                            salida.write("400 Bad request\n");
                            salida.flush();
                        }
                    }
                } catch (Exception e) {
                    salida.write("500 Server error\n");
                    salida.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Error en cliente: " + e.getMessage());
        }
    }
}
