package gopher.service.model.dialog;

import gopher.service.model.RestrictionEnzyme;

import java.util.List;

public class RestrictionEnzymeResult {

    private final List<RestrictionEnzyme> chosenEzymes;


    public RestrictionEnzymeResult(List<RestrictionEnzyme> chosenEzymes) {
        this.chosenEzymes = chosenEzymes;
    }

    public List<RestrictionEnzyme> getChosenEzymes() {
        return chosenEzymes;
    }
}
