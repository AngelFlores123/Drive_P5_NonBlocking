import java.io.File;

public class Archivos {
    public static void main(String[] args) {
        // Ruta base donde se crearán las carpetas
        String rutaBase = "C:\\Users\\Berlin\\Desktop\\p2 r2";  // Puedes cambiar la ruta a la que desees

        // Crear la primera carpeta
        File carpeta1 = new File(rutaBase + "/Carpeta1");
        if (carpeta1.mkdir()) {
            System.out.println("Carpeta1 creada con éxito.");
        } else {
            System.out.println("Error al crear Carpeta1.");
        }

        // Crear más carpetas en el mismo lugar
        File carpeta2 = new File(rutaBase + "/Carpeta2");
        File carpeta3 = new File(rutaBase + "/Carpeta3");

        if (carpeta2.mkdir()) {
            System.out.println("Carpeta2 creada con éxito.");
        } else {
            System.out.println("Error al crear Carpeta2.");
        }

        if (carpeta3.mkdir()) {
            System.out.println("Carpeta3 creada con éxito.");
        } else {
            System.out.println("Error al crear Carpeta3.");
        }
    }
}
