package vpvgui.util;

import org.apache.log4j.Logger;
import vpvgui.model.Model;
import vpvgui.model.viewpoint.Segment;

import java.io.*;

public class SerializationManager {
    private static final Logger logger = Logger.getLogger(SerializationManager.class.getName());
    /** This serializes the Model object. It replaces any spaces in the filename with underscores. */
    public static void serializeModel(Model model, String fileName)
            throws IOException {
        fileName = fileName.replaceAll(" ","_");
        FileOutputStream fos = new FileOutputStream(fileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(model);

        fos.close();
    }

    public static Model deserializeModel(String fileName) throws IOException,ClassNotFoundException {
        logger.trace(String.format("Deserializing model \"%s\"",fileName ));
        Object obj=null;
        FileInputStream fis = new FileInputStream(fileName);
        ObjectInputStream ois = new ObjectInputStream(fis);
        obj = ois.readObject();
        if (obj==null) {
            logger.error("Deserialized object was NULL");
        }
        ois.close();
        Model model = (Model) obj;
        return model;
    }



}
