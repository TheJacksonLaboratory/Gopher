package gopher.util;

import org.apache.log4j.Logger;
import gopher.model.Model;


import java.io.*;

/**
 * This class is responsible for serializing and deserializing the {@link Model} object that represents the project.
 */
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
        model.setClean(true);
    }

    public static Model deserializeModel(String fileName) throws IOException,ClassNotFoundException {
        Object obj;
        FileInputStream fis = new FileInputStream(fileName);
        ObjectInputStream ois = new ObjectInputStream(fis);
        obj = ois.readObject();
        if (obj==null) {
            logger.error("Deserialized object was NULL");
        }
        ois.close();
        return (Model) obj;
    }



}
