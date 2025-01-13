import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Scanner;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.util.Iterator;

// puerto servidor = 9876
// puerto cliente = 9877

public class Client {
    public static void main(String[] args) throws Exception {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        InetSocketAddress serverAddress = new InetSocketAddress("localhost", 9876);

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (true) {
            System.out.println("Selecciona una opción (1-8): ");
            String opcion = menu();

            if (opcion.equals("8")) {
                System.out.println("Saliendo...");
                break;
            }

            enviarPaqueteAlServidor(channel, buffer, opcion, serverAddress);

            if (selector.select(5000) > 0) {
                Iterator<java.nio.channels.SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    java.nio.channels.SelectionKey key = keys.next();
                    if (key.isReadable()) {
                        String respuesta = recibirPaqueteDelServidor(channel, buffer);
                        if (respuesta != null) {
                            System.out.println("Respuesta del servidor: " + respuesta);
                        }
                    }
                    keys.remove();
                }
            } else {
                System.out.println("Tiempo de espera agotado. No se recibió respuesta.");
            }

            menuOpciones(channel, buffer, opcion, serverAddress);
        }

        channel.close();
        selector.close();
    }

    public static String menu(){
        Scanner scanner = new Scanner(System.in);
        while(true){
            System.out.println("Carpetas:\n ");
            System.out.println("\tNube");
            System.out.println("\tLocal\n");
            System.out.println("1. Crear carpeta");
            System.out.println("2. Crear archivo txt");
            System.out.println("3. Mostrar contenido de carpeta");
            System.out.println("4. Cargar archivos a nube");
            System.out.println("5. Descargar archivos de nube");
            System.out.println("6. Eliminar carpeta/archivo");
            System.out.println("7. Renombrar archivo/carpeta");
            System.out.println("8. Salir");
            System.out.println("\nSeleccione una opcion");
            String opcion = scanner.nextLine();
            return opcion;
        }
    }

    public static void enviarPaqueteAlServidor(DatagramChannel channel, ByteBuffer buffer, String mensaje, InetSocketAddress serverAddress) throws Exception {
        buffer.clear(); // Limpiar el buffer antes de escribir
        buffer.put(mensaje.getBytes());
        buffer.flip(); // Preparar el buffer para la lectura por el canal
        channel.send(buffer, serverAddress); // Enviar el contenido al servidor
        System.out.println("Mensaje enviado al servidor: " + mensaje);
    }

    public static String recibirPaqueteDelServidor(DatagramChannel channel, ByteBuffer buffer) throws Exception {
        buffer.clear();
        InetSocketAddress serverAddress = (InetSocketAddress) channel.receive(buffer); // Intentar recibir datos
        if (serverAddress == null) {
            return null; // No se recibió ningún paquete
        }
        buffer.flip(); // Preparar el buffer para leer
        return new String(buffer.array(), 0, buffer.limit());
    }
    
    public static void recibirContenidoDeCarpeta(DatagramChannel channel, ByteBuffer buffer) throws Exception {
        System.out.println("Recibiendo contenido de la carpeta...");
        while (true) {
            buffer.clear();
            SocketAddress serverAddress = channel.receive(buffer);
    
            if (serverAddress != null) {
                buffer.flip();
                String mensaje = new String(buffer.array(), 0, buffer.limit()).trim();
    
                if (mensaje.equals("Fin")) {
                    System.out.println("Fin del contenido.");
                    break;
                }
    
                System.out.println(mensaje);
    
                // Enviar ACK al servidor solo si se recibe un mensaje válido
                buffer.clear();
                buffer.put("ACK".getBytes());
                buffer.flip();
                channel.send(buffer, serverAddress);
            }
        }
    }    
    
    public static void registrarInformacion(DatagramChannel channel, ByteBuffer buffer, InetSocketAddress serverAddress) throws Exception{
        System.out.println("Ingrese el nombre de la carpeta principal: ");
        String carpetaPrincipal = new Scanner(System.in).nextLine();
        System.out.println("¿Como se llama la nueva carpeta/archivo?: ");
        String nombreCarpeta  = new Scanner(System.in).nextLine();
        String datosCarpetas = carpetaPrincipal + "," + nombreCarpeta;
        enviarPaqueteAlServidor(channel, buffer, datosCarpetas, serverAddress);
    }
    
    public static void registrarInformacioEliminarArchivo(DatagramChannel channel, ByteBuffer buffer, InetSocketAddress serverAddress) throws Exception{
        System.out.println("Ingrese el nombre de la carpeta principal: ");
        String carpetaPrincipal = new Scanner(System.in).nextLine();
        System.out.println("Ingrese el nombre del archivo/carpeta del que le quiere cambiar el nombre: ");
        String nombreArchivo  = new Scanner(System.in).nextLine();
        System.out.println("Ingrese el nuevo nombre del archivo/carpeta: ");
        String nuevoNombreArchivo  = new Scanner(System.in).nextLine();
        String datosCambioDeNombre = carpetaPrincipal + "," + nombreArchivo + "," + nuevoNombreArchivo;
        enviarPaqueteAlServidor(channel, buffer, datosCambioDeNombre, serverAddress);
    }

    public static void descargarArchivo(DatagramChannel channel, ByteBuffer buffer, InetSocketAddress serverAddress) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Ingrese el nombre del archivo a descargar: ");
        String nombreArchivo = scanner.nextLine();
    
        enviarPaqueteAlServidor(channel, buffer, nombreArchivo, serverAddress);
    
        System.out.print("Ingrese el tamaño de ventana en bytes: ");
        int tamanoVentana = Integer.parseInt(scanner.nextLine());
        System.out.print("Ingrese el tamaño de paquete en bytes: ");
        int tamanoPaquete = Integer.parseInt(scanner.nextLine());
    
        enviarPaqueteAlServidor(channel, buffer, tamanoVentana + "," + tamanoPaquete, serverAddress);
    
        File archivoDestino = new File(System.getProperty("user.dir") + "/Local/" + nombreArchivo);
        try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
            while (true) {
                buffer.clear();
                InetSocketAddress senderAddress = (InetSocketAddress) channel.receive(buffer);
                if (senderAddress == null) {
                    continue; // No se recibió ningún paquete, continuar
                }
                buffer.flip();
                String mensaje = new String(buffer.array(), 0, buffer.limit());
                if (mensaje.equals("El archivo no existe.")) {
                    System.out.println(mensaje);
                    archivoDestino.delete();
                    break;
                }
                if (mensaje.equals("FIN")) {
                    System.out.println("Archivo recibido correctamente.");
                    break;
                }
                fos.write(mensaje.getBytes());
                System.out.println("Paquete recibido y escrito: " + mensaje);
    
                enviarPaqueteAlServidor(channel, buffer, "ACK", senderAddress);
            }
        }
    }
    
    public static void cargarArchivo(DatagramChannel channel, ByteBuffer buffer, InetSocketAddress serverAddress) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Introduce el nombre del archivo que deseas cargar:");
        String nombreArchivo = scanner.nextLine();

        // Verificar si el archivo existe en la carpeta Local
        File archivo = new File(System.getProperty("user.dir") + "/Local/" + nombreArchivo);
        if (!archivo.exists()) {
            System.out.println("El archivo no existe en la carpeta local.\n\n");
            return;
        }

        // Enviar el nombre del archivo al servidor
        buffer.clear();
        buffer.put(nombreArchivo.getBytes());
        buffer.flip();
        channel.send(buffer, serverAddress);

        try (FileInputStream fis = new FileInputStream(archivo)) {
            byte[] paqueteBuffer = new byte[256]; // Se reservará 1 byte para el número de secuencia de cada paquete
            int bytesLeidos;
            int numeroSecuencia = 0;
            int base = 0;
            int siguientePaquete = 0;
            int ventanaSize = 3;
            boolean[] ackRecibidos = new boolean[1000];
            boolean finArchivo = false;
            byte[][] bufferDatos = new byte[ventanaSize][1024]; // Buffer de datos enviados en caso de necesitar reenviarlos

            while (!finArchivo) {
                // Enviar ventana de paquetes
                while (siguientePaquete < base + ventanaSize && !finArchivo) {
                    bytesLeidos = fis.read(paqueteBuffer);
                    if (bytesLeidos == -1) {
                        finArchivo = true;
                        break;
                    }

                    // Crear el paquete con número de secuencia
                    byte[] datos = new byte[bytesLeidos + 1];
                    datos[0] = (byte) numeroSecuencia;
                    System.arraycopy(paqueteBuffer, 0, datos, 1, bytesLeidos);

                    // Guardar los datos en el buffer para retransmisión si es necesario
                    System.arraycopy(datos, 0, bufferDatos[siguientePaquete % ventanaSize], 0, datos.length);

                    // Enviar el paquete al servidor
                    buffer.clear();
                    buffer.put(datos);
                    buffer.flip();
                    channel.send(buffer, serverAddress);
                    System.out.println("Enviado paquete #" + numeroSecuencia);

                    siguientePaquete++;
                    numeroSecuencia = (numeroSecuencia + 1) % 256; // Ciclar el número de secuencia
                }

                // Escuchar ACKs del servidor
                buffer.clear();
                channel.receive(buffer);
                buffer.flip();

                if (buffer.hasRemaining()) {
                    int ack = buffer.get() & 0xFF;
                    System.out.println("ACK recibido: #" + ack);

                    if (ack >= base && !ackRecibidos[ack]) {
                        ackRecibidos[ack] = true;
                        while (base < siguientePaquete && ackRecibidos[base]) {
                            base++;
                        }
                    }
                } else {
                    // Retransmitir paquetes en caso de timeout
                    System.out.println("Timeout. Retransmitiendo paquetes...");
                    for (int i = base; i < siguientePaquete; i++) {
                        if (!ackRecibidos[i]) {
                            System.out.println("Retransmitiendo paquete #" + i);
                            buffer.clear();
                            buffer.put(bufferDatos[i % ventanaSize]);
                            buffer.flip();
                            channel.send(buffer, serverAddress);
                        }
                    }
                }
            }

            // Enviar mensaje de finalización
            String mensajeFin = "FIN";
            buffer.clear();
            buffer.put(mensajeFin.getBytes());
            buffer.flip();
            channel.send(buffer, serverAddress);
            System.out.println("Archivo cargado correctamente al servidor.\n\n");
        }
    }
    
    public static void menuOpciones(DatagramChannel channel, ByteBuffer buffer, String opcion, InetSocketAddress serverAddress) throws Exception{
        switch (opcion){
            case "1":
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                registrarInformacion(channel, buffer, serverAddress);
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                break;
            case "2":
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                registrarInformacion(channel, buffer, serverAddress);
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                break;
            case "3":
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                while(true){
                    System.out.println("¿Que carpeta deseas ver? (Nube/Local)");
                    String carpetaSeleccionada = new Scanner(System.in).nextLine();
                    enviarPaqueteAlServidor(channel, buffer, carpetaSeleccionada, serverAddress);
                    if(carpetaSeleccionada.equalsIgnoreCase("Nube")) {
                        recibirContenidoDeCarpeta(channel, buffer);
                        break;
                    }else if(carpetaSeleccionada.equalsIgnoreCase("Local")){
                        recibirContenidoDeCarpeta(channel, buffer);
                        break;
                    }else{
                        System.out.println("Opción invalida, intenta nuevamente");
                    }
                }
                break;
            case "4":
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                cargarArchivo(channel, buffer, serverAddress);
                break;
            case "5":
                System.out.println(recibirPaqueteDelServidor(channel, buffer));  // Recibe "Seleccione el archivo a descargar"
                descargarArchivo(channel, buffer, serverAddress);
                break;
            case "6":
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                registrarInformacion(channel, buffer, serverAddress);
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                break;
            case "7":
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                registrarInformacioEliminarArchivo(channel, buffer, serverAddress);
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                break;
            default:
                System.out.println(recibirPaqueteDelServidor(channel, buffer));
                break;
        }
    }
    
}
