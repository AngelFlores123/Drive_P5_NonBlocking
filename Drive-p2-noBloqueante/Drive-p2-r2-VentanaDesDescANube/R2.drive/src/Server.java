import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

// puerto servidor = 9876
// puerto cliente = 9877

public class Server{
    public static void main(String[] args) throws Exception {
        // Crear las carpetas necesarias
        crearCarpetaEnRutaProyecto("Nube");
        crearCarpetaEnRutaProyecto("Local");

        try {
            DatagramChannel channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(9876));
            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            ByteBuffer buffer = ByteBuffer.allocate(2000);
            buffer.flip(); // Cambiar el buffer a modo lectura

            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isReadable()) {
                        buffer.clear();
                        System.out.println("Servidor listo para recibir mensajes...\n");
                        SocketAddress clientAddress = channel.receive(buffer);

                        // Recibir opción del cliente
                        String opcion = new String(buffer.array(), 0, buffer.position());
                        System.out.println("Opción recibida: " + opcion + "\n");
            
                        // Analizar la opción recibida y ejecutar la acción correspondiente
                        analizarOpcion(channel, buffer, opcion, clientAddress);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void crearCarpetaEnRutaProyecto(String nombreCarpeta) {
        String rutaProyecto = System.getProperty("user.dir");
        File nuevaCarpeta = new File(rutaProyecto + "/" + nombreCarpeta);
        if (nuevaCarpeta.mkdir()) {
            System.out.println("Carpeta '" + nombreCarpeta + "' creada con éxito en la ruta del proyecto.");
        } else {
            System.out.println("Error al crear la carpeta '" + nombreCarpeta + "' en la ruta del proyecto. \n O la carpeta ya existe");
        }
    }

    public static void crearArchivoLocal(String nombreCarpeta, String nombreArchivo, String contenido) {
        String rutaProyecto = System.getProperty("user.dir");
        File archivo = new File(rutaProyecto + "/" + nombreCarpeta + "/" + nombreArchivo + ".txt");
        try {
            if (archivo.createNewFile()) {
                System.out.println("Archivo '" + nombreArchivo + ".txt' creado con éxito.");
                FileWriter escritor = new FileWriter(archivo);
                escritor.write(contenido);
                escritor.close();
                System.out.println("Contenido escrito en el archivo.");
            } else {
                System.out.println("El archivo '" + nombreArchivo + ".txt' ya existe.");
            }
        } catch (IOException e) {
            System.out.println("Error al crear el archivo.");
            e.printStackTrace();
        }
    }

    public static String recibirPaqueteDelCliente(DatagramChannel channel, ByteBuffer buffer) throws Exception {
        System.out.println("Servidor listo para recibir mensajes...\n");
        SocketAddress cliente = channel.receive(buffer);
    
        if (cliente == null) {
            return null;
        }
    
        String mensaje = new String(buffer.array(), 0, buffer.limit());
        buffer.clear();
        System.out.println("Mensaje recibido de: " + cliente);
    
        return mensaje;
    }

    public static void enviarMensajeAlCliente(DatagramChannel channel, ByteBuffer buffer, String message, SocketAddress clientAddress) throws Exception {
        buffer.clear();
        buffer.put(message.getBytes());    
        channel.send(buffer, clientAddress);
        buffer.clear();
    
        System.out.println("Mensaje enviado a: " + clientAddress);
    }
    
    public static void mostrarContenidoCarpeta(DatagramChannel channel, ByteBuffer buffer, String nombreCarpeta, SocketAddress clientAddress) throws Exception {
        String rutaProyecto = System.getProperty("user.dir");
        File carpeta = new File(rutaProyecto + "/" + nombreCarpeta);
    
        if (carpeta.exists() && carpeta.isDirectory()) {
            File[] archivos = carpeta.listFiles();
            if (archivos != null && archivos.length > 0) {
                enviarMensajeAlCliente(channel, buffer, "Contenido de la carpeta '" + nombreCarpeta + "':", clientAddress);
                for (File archivo : archivos) {
                    buffer.clear();
                    enviarMensajeAlCliente(channel, buffer, archivo.getName(), clientAddress);
    
                    // Esperar ACK del cliente
                    buffer.clear();
                    channel.receive(buffer);
                    buffer.flip();
                    String ack = new String(buffer.array(), 0, buffer.limit()).trim();
                    if (!ack.equals("ACK")) {
                        System.out.println("ACK no recibido, reenviando...");
                        buffer.clear();
                        enviarMensajeAlCliente(channel, buffer, archivo.getName(), clientAddress);
                    }
                }
            } else {
                enviarMensajeAlCliente(channel, buffer, "La carpeta está vacía.", clientAddress);
            }
        } else {
            enviarMensajeAlCliente(channel, buffer, "La carpeta no existe.", clientAddress);
        }
    
        enviarMensajeAlCliente(channel, buffer, "Fin", clientAddress);
    } 

    public static void crearCarpetaEnRutaPersonalizada(String carpetaPrincipal, String nombreCarpeta) {
        String rutaProyecto = System.getProperty("user.dir");
        File nuevaCarpeta = new File(rutaProyecto + "/" + carpetaPrincipal + "/" + nombreCarpeta);
        if (nuevaCarpeta.mkdir()) {
            System.out.println("Carpeta '" + nombreCarpeta + "' creada con éxito en la ruta del proyecto.");
        } else {
            System.out.println("Error al crear la carpeta '" + nombreCarpeta + "' en la ruta del proyecto. \n O la carpeta ya existe");
        }
    }

    public static void crearCarpeta(String nombreCarpeta, String nombreArchivo) throws Exception{
        if (nombreCarpeta.equalsIgnoreCase("Nube") || nombreCarpeta.equalsIgnoreCase("nube")) {
            crearCarpetaEnRutaPersonalizada(nombreCarpeta, nombreArchivo);
        }else if(nombreCarpeta.equalsIgnoreCase("Local") || nombreCarpeta.equalsIgnoreCase("local")){
            crearCarpetaEnRutaPersonalizada(nombreCarpeta, nombreArchivo);
        }
    }

    public static void crearArchivo(DatagramChannel channel, ByteBuffer buffer, String nombreCarpeta, String nombreArchivo, SocketAddress clientAddress) throws Exception{
        String rutaProyecto = System.getProperty("user.dir");
        File nuevoArchivo = new File(rutaProyecto + "/" + nombreCarpeta + "/" + nombreArchivo + ".txt");
        try{
            if(nuevoArchivo.createNewFile()){
                enviarMensajeAlCliente(channel, buffer, ("Archivo " + nombreArchivo + " creado con exito"), clientAddress);
            }else{
                enviarMensajeAlCliente(channel, buffer, ("Error o el archivo " + nombreArchivo + " no ya existe"), clientAddress);
            }
        }catch(IOException e){
            enviarMensajeAlCliente(channel, buffer, ("Error al crear el archivo " + nombreArchivo + ".txt"), clientAddress);
        }
    }

    public static void eliminarRuta(DatagramChannel channel, ByteBuffer buffer, String carpetaPrincipal, String archivo_carpeta, SocketAddress clientAddress) throws Exception{
        String ruta = System.getProperty("user.dir") + File.separator + carpetaPrincipal + File.separator + archivo_carpeta;
        Path rutaAEliminar = Paths.get(ruta);

        try {
            if (Files.exists(rutaAEliminar)) { // Verificar que la ruta exista
                if (Files.isDirectory(rutaAEliminar)) {
                    // Si es una carpeta, eliminar todo su contenido
                    Files.walk(rutaAEliminar)
                        .sorted((path1, path2) -> path2.compareTo(path1))
                        .forEach(path -> {
                            try {
                                Files.delete(path); // Elimina cada archivo o subcarpeta
                                enviarMensajeAlCliente(channel, buffer, ("Eliminado: " + path), clientAddress);
                            } catch (Exception e) {
                                System.out.println("Error al eliminar el archivo.");
                                e.printStackTrace();
                            }
                        });
                    enviarMensajeAlCliente(channel, buffer, ("Carpeta eliminada: " + ruta), clientAddress);
                } else if (Files.isRegularFile(rutaAEliminar)) {
                    // Si es un archivo, solo lo elimina
                    Files.delete(rutaAEliminar);
                    enviarMensajeAlCliente(channel, buffer, ("Archivo eliminado: " + ruta), clientAddress);
                }
            } else {
                enviarMensajeAlCliente(channel, buffer, ("No se encontró el archivo o carpeta: " + ruta), clientAddress);
            }
        } catch (Exception e) {
            System.out.println("Error al eliminar el archivo.");
            e.printStackTrace();
        }
    }

    public static void nuevoNombreArchivo(DatagramChannel channel, ByteBuffer buffer, String nombreArchivo, SocketAddress clientAddress) throws Exception{
    String[] partesNuevoNombreArchivo = nombreArchivo.split(",");
    String carpetaPrincipalNuevoNombre = partesNuevoNombreArchivo[0];
    String archivoActualNuevoNombre = partesNuevoNombreArchivo[1];
    String nuevoNombre = partesNuevoNombreArchivo[2];
    String rutaProyecto = System.getProperty("user.dir");

        File archivoActualCarpeta = new File(rutaProyecto + "/" + carpetaPrincipalNuevoNombre + "/" + archivoActualNuevoNombre);
        File archivoNuevoCarpeta = new File(rutaProyecto + "/" + carpetaPrincipalNuevoNombre + "/" + nuevoNombre);
        if(archivoActualCarpeta.exists()){
            if (archivoActualCarpeta.renameTo(archivoNuevoCarpeta)) {
                enviarMensajeAlCliente(channel, buffer, ("Carpeta renombrada a " + nuevoNombre), clientAddress);
            }else {
                enviarMensajeAlCliente(channel, buffer, ("Error al renombrar " + archivoActualNuevoNombre + " a " + nuevoNombre), clientAddress);
            }
        }else{
            enviarMensajeAlCliente(channel, buffer, ("Error al cambiar el nombre"), clientAddress);
        }
    }

    public static void descargarArchivo(DatagramChannel channel, ByteBuffer buffer, String nombreArchivo, SocketAddress clientAddress) throws Exception {
        // Recibir tamaño de ventana y tamaño de paquete desde el cliente
        String tamanos = recibirPaqueteDelCliente(channel, buffer); // Recibir los tamaños desde el cliente
        String[] partes = tamanos.split(","); // Separar los valores recibidos por coma
        int tamanoVentana = Integer.parseInt(partes[0]); // El tamaño de la ventana
        int tamanoPaquete = Integer.parseInt(partes[1]); // El tamaño de cada paquete

        // Ruta del archivo en la carpeta Nube
        String rutaProyecto = System.getProperty("user.dir"); // Obtener la ruta del directorio de trabajo actual
        File archivo = new File(rutaProyecto + "/Nube/" + nombreArchivo); // Crear un objeto File con la ruta completa del archivo

        try{
            if (!archivo.exists()) { // Verificar si el archivo no existe
                // Si el archivo no existe, enviar un mensaje de error al cliente
                String mensajeError = "El archivo no existe."; // Mensaje de error
                buffer.clear();
                buffer.flip(); // Cambiar el buffer a modo escritura
                buffer.put(mensajeError.getBytes());
                channel.send(buffer, clientAddress);
                buffer.clear();
                System.out.println("El archivo solicitado no existe."); // Imprimir en consola el mensaje de error
                return; // Salir del método si el archivo no existe
            }

            // Si el archivo existe, iniciar la transferencia
            try (FileInputStream fis = new FileInputStream(archivo)) { // Abrir un flujo de entrada para leer el archivo
                byte[] paqueteBuffer = new byte[tamanoPaquete]; // Buffer para leer el archivo en trozos del tamaño del paquete
                int bytesLeidos; // Variable para almacenar los bytes leídos en cada iteración
                int numeroPaquete = 0; // Contador para el número de paquete
                boolean finArchivo = false; // Bandera que indica si ya se ha llegado al final del archivo

                buffer.flip(); // Cambiar el buffer a modo lectura
                while (!finArchivo) { // Mientras no se haya alcanzado el final del archivo
                    // Crear un StringBuilder para almacenar los paquetes de la ventana
                    StringBuilder ventanaContenido = new StringBuilder();

                    // Leer múltiples paquetes para formar una ventana completa
                    for (int i = 0; i < tamanoVentana / tamanoPaquete; i++) {
                        bytesLeidos = fis.read(paqueteBuffer); // Leer bytes del archivo en el buffer

                        if (bytesLeidos == -1) { // Si no se leen más bytes (fin del archivo)
                            finArchivo = true; // Marcar el fin del archivo
                            break; // Salir del bucle si se llegó al final del archivo
                        }

                        // Añadir el paquete leído a la ventana
                        ventanaContenido.append(numeroPaquete) // Número de paquete
                                .append(" ").append(nombreArchivo) // Nombre del archivo
                                .append(" ").append(new String(paqueteBuffer, 0, bytesLeidos)); // Datos leídos en el paquete
                        numeroPaquete++; // Incrementar el contador de paquetes
                    }

                    // Enviar la ventana completa de paquetes al cliente
                    buffer.clear();
                    buffer.put(ventanaContenido.toString().getBytes());
                    channel.send(buffer, clientAddress);
                    buffer.clear();

                    // Mostrar en consola que la ventana de paquetes fue enviada
                    System.out.println("\nVentana de paquetes enviada.\n");

                    // Esperar el ACK del cliente
                    boolean ackRecibido = false;

                    while (!ackRecibido) {
                        try {
                            buffer.clear();
                            channel.receive(buffer); // Recibir el ACK desde el cliente
                            String ack = new String(buffer.array(), 0, buffer.limit()); // Convertir el ACK a String
                            System.out.println("\nACK recibido: " + ack + "\n"); // Mostrar el ACK recibido
                            ackRecibido = true; // Si el ACK es recibido correctamente, continuar
                            buffer.clear();
                        } catch (SocketTimeoutException e) {
                            System.out.println("Tiempo de espera agotado. Reintentando el envío...");
                            buffer.clear();
                            buffer.flip();
                            buffer.put(ventanaContenido.toString().getBytes());
                            channel.send(buffer, clientAddress);
                            buffer.flip();
                            buffer.clear(); // Reintentar el envío si no se recibe el ACK
                        }
                    }
                }

                // Enviar mensaje de finalización de transferencia
                String finMensaje = "FIN"; // Mensaje indicando que la transferencia ha terminado
                buffer.clear(); 
                buffer.flip(); // Cambiar el buffer a modo escritura
                buffer.put(finMensaje.getBytes());
                channel.send(buffer, clientAddress); // Enviar el mensaje de fin al cliente
                buffer.clear();
                buffer.flip(); // Volver al modo lectura
                System.out.println("Transferencia finalizada, se envió el mensaje de fin."); // Mostrar mensaje en consola
            }
        } catch (IOException e) { // Manejo de excepciones si ocurre un error en la transferencia
            System.err.println("Error al enviar el archivo: " + e.getMessage()); // Imprimir el error en consola
        }
        System.out.println("Archivo enviado correctamente."); // Imprimir mensaje de éxito cuando la transferencia ha finalizado
    }

    public static void cargarArchivo(DatagramChannel channel, ByteBuffer buffer, SocketAddress clientAddress) throws Exception {
        System.out.println("Servidor listo para recibir archivos...");
    
        // Recibir nombre del archivo
        buffer.clear();
        channel.receive(buffer);
        String nombreArchivo = new String(buffer.array(), 0, buffer.limit());
        buffer.clear();
    
        File archivo = new File(System.getProperty("user.dir") + "/nube/" + nombreArchivo);
        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            boolean finArchivo = false;
            boolean[] paquetesRecibidos = new boolean[256];

            while (!finArchivo) {
                // Recibir paquete
                buffer.clear();
                channel.receive(buffer);

                byte[] datos = buffer.array();
                int numeroSecuencia = datos[0] & 0xFF; // Extraer número de secuencia, se usa 0xff para tratarlo como entero sin signo
                String contenido = new String(datos, 1, datos.length - 1);
                buffer.clear();

                if (contenido.equals("FIN")) {
                    System.out.println("Archivo recibido completamente: " + nombreArchivo);
                    finArchivo = true;
                    break;
                }

                // Escribir paquete y registrarlo como recibido
                if (!paquetesRecibidos[numeroSecuencia]) {
                    fos.write(datos, 1, datos.length - 1);
                    paquetesRecibidos[numeroSecuencia] = true;
                    System.out.println("Paquete #" + numeroSecuencia + " recibido.");
                }else{
                    System.out.println("Paquete duplicado #" + numeroSecuencia + " descartado.");
                }
                
                // Revision de que se han recibido todos los paquetes
                int ultimoAck = numeroSecuencia;
                int base = 0;
                for (int i = base; i <= numeroSecuencia; i++) {
                    if (!paquetesRecibidos[i % 256]) {
                        break;
                    }
                    base = (i + 1) % 256; // Actualizar la base de la ventana
                }
                ultimoAck = (base - 1 + 256) % 256; // El último ACK confirmado             

                // Enviar ACK para el último paquete confirmado
                byte[] ack = new byte[1];
                ack[0] = (byte) ultimoAck;
                buffer.clear();
                buffer.flip(); // Cambiar el buffer a modo escritura
                buffer.put(ack);
                channel.send(buffer, clientAddress);
                buffer.clear();
                buffer.flip(); // Cambiar el buffer a modo lectura
                System.out.println("ACK enviado para el paquete #" + ultimoAck);
            }
        }
    } 

    public static void analizarOpcion(DatagramChannel channel, ByteBuffer buffer, String opcion, SocketAddress clientAddress) throws Exception{
        switch(opcion){
            case "1":
                enviarMensajeAlCliente(channel, buffer, "Crear carpeta", clientAddress);
                String datosCarpeta = recibirPaqueteDelCliente(channel, buffer);
                if (datosCarpeta == null || datosCarpeta.isEmpty()) {
                    enviarMensajeAlCliente(channel, buffer, "Error: Datos no recibidos", clientAddress);
                    return;
                }
                String[] partes = datosCarpeta.split(",");
                String carpetaPrincipal = partes[0];
                String carpetaSecundario = partes[1];
                crearCarpeta(carpetaPrincipal, carpetaSecundario);
                enviarMensajeAlCliente(channel, buffer, ("Carpeta " + carpetaSecundario + " creada exitosamente"), clientAddress);
                break;
            case "2":
                enviarMensajeAlCliente(channel, buffer, "Crear archivo txt", clientAddress);
                String datosArchivo = recibirPaqueteDelCliente(channel, buffer);
                if (datosArchivo == null || datosArchivo.isEmpty()) {
                    enviarMensajeAlCliente(channel, buffer, "Error: Datos no recibidos", clientAddress);
                    return;
                }
                String[] partesArchivo = datosArchivo.split(",");
                String carpetaPrincipalArchivo = partesArchivo[0];
                String nombreArchivo = partesArchivo[1];
                crearArchivo(channel, buffer, carpetaPrincipalArchivo, nombreArchivo, clientAddress);
                break;
            case "3":
                enviarMensajeAlCliente(channel, buffer, "\nMostrar contenido de carpeta", clientAddress);
                String carpetaSeleccionada = recibirPaqueteDelCliente(channel, buffer);
                if (carpetaSeleccionada == null || carpetaSeleccionada.isEmpty()) {
                    enviarMensajeAlCliente(channel, buffer, "Error: Datos no recibidos", clientAddress);
                    return;
                }
                if (carpetaSeleccionada.equalsIgnoreCase("Nube") || carpetaSeleccionada.equalsIgnoreCase("nube")){
                    System.out.println("El cliente ha seleccionado la carpeta Nube");
                    mostrarContenidoCarpeta(channel, buffer, "Nube", clientAddress);
                }else if (carpetaSeleccionada.equalsIgnoreCase("Local") || carpetaSeleccionada.equalsIgnoreCase("local")){
                    System.out.println("El cliente ha seleccionado la carpeta Local");
                    mostrarContenidoCarpeta(channel, buffer, "Local", clientAddress);
                }else{
                    enviarMensajeAlCliente(channel, buffer, "Opción inválida", clientAddress);
                }
                break;
            case "4":
                enviarMensajeAlCliente(channel, buffer, "Cargar archivo a la Nube:", clientAddress);
                cargarArchivo(channel, buffer, clientAddress);
                break;
            case "5":
                enviarMensajeAlCliente(channel, buffer, "Seleccione el archivo a descargar de Nube:", clientAddress);
                String archivoSolicitado = recibirPaqueteDelCliente(channel, buffer);
                if (archivoSolicitado == null || archivoSolicitado.isEmpty()) {
                    enviarMensajeAlCliente(channel, buffer, "Error: Datos no recibidos", clientAddress);
                    return;
                }
                descargarArchivo(channel, buffer, archivoSolicitado, clientAddress);
                break;
            case "6":
                enviarMensajeAlCliente(channel, buffer, "Eliminar carpeta/archivo", clientAddress);
                String datos = recibirPaqueteDelCliente(channel, buffer);
                if (datos == null || datos.isEmpty()) {
                    enviarMensajeAlCliente(channel, buffer, "Error: Datos no recibidos", clientAddress);
                    return;
                }
                String[] partesRuta = datos.split(",");
                String principal = partesRuta[0];
                String archivo_carpeta = partesRuta[1];
                eliminarRuta(channel, buffer, principal, archivo_carpeta, clientAddress);
                break;
            case "7":
                enviarMensajeAlCliente(channel, buffer, "Renombrar archivo/carpeta", clientAddress);
                String nuevoNombreArchivo = recibirPaqueteDelCliente(channel, buffer);
                if (nuevoNombreArchivo == null || nuevoNombreArchivo.isEmpty()) {
                    enviarMensajeAlCliente(channel, buffer, "Error: Datos no recibidos", clientAddress);
                    return;
                }
                nuevoNombreArchivo(channel, buffer, nuevoNombreArchivo, clientAddress);
                break;
            default:
                enviarMensajeAlCliente(channel, buffer, "Opcion no valida", clientAddress);
        }
    }
}

