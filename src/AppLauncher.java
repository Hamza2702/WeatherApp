import javax.swing.*;

public class AppLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run(){
                // Display weather app GUI
                new WeatherAppGui().setVisible(true);
//                System.out.println(WtApp.getCurrentTime());
            }
        });
    }

}