package gopher.util;

import gopher.service.model.GopherModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;

/**
 * This class is responsible for serializing and deserializing the {@link GopherModel} object that represents the project.
 */
public class SerializationManager {
    private static final Logger logger = LoggerFactory.getLogger(SerializationManager.class.getName());
    /** This serializes the Model object. It replaces any spaces in the filename with underscores. */
    public static void serializeModel(GopherModel model, String fileName)
            throws IOException {
        //fileName = fileName.replaceAll(" ","_");
        FileOutputStream fos = new FileOutputStream(fileName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(model);
        fos.close();
        model.setClean(true);
    }

    public static GopherModel deserializeModel(String fileName) throws IOException,ClassNotFoundException {
        Object obj;
        FileInputStream fis = new FileInputStream(fileName);
        ObjectInputStream ois = new ObjectInputStream(fis);
        obj = ois.readObject();
        if (obj==null) {
            logger.error("Deserialized object was NULL");
        }
        ois.close();
        return (GopherModel) obj;
    }



}
