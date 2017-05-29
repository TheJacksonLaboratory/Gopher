package vpvgui.model.project;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;

import java.util.List;

/**
 * One of the target genes for which we want to design capture C probes
 */
public class Gene {

    private IntegerProperty entrezGeneID;

    private StringProperty geneSymbol;

    private List<ViewPoint> viewPointList;

    public Gene(String geneid, String symbol) {

    }
 }
