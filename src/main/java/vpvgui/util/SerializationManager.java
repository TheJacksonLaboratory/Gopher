package vpvgui.util;

import vpvgui.model.Model;

import java.io.*;

public class SerializationManager {

    public static void serializeModel(Model model, String fileName)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(model);

        fos.close();
    }

    public static Model deserializeModel(String fileName){
        Object obj=null;
        try {
            FileInputStream fis = new FileInputStream(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            obj = ois.readObject();
            ois.close();
        } catch (IOException ioe) {

        } catch (ClassNotFoundException cnfe) {

        }
        Model model = (Model) obj;
        return model;
    }



}
