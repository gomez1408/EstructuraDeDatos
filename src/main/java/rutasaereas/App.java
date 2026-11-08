package rutasaereas;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("ui/mapa.fxml"));
        Parent raiz = loader.load();
        Scene scene = new Scene(raiz, 1280, 780);
        scene.getStylesheets().add(App.class.getResource("ui/estilos.css").toExternalForm());
        stage.setTitle("Rutas Aéreas · EIF207 Proyecto II");
        stage.getIcons().add(new Image(App.class.getResource("ui/img/icono.png").toExternalForm()));
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}