package gopher.service.model.dialog;

import gopher.service.model.RestrictionEnzyme;

import java.util.List;

/**
 * Simple class designed to hold the results of a Dialog for choosing restriction enzymes.
 * @author Peter N Robinson
 */
public record RestrictionEnzymeResult(List<RestrictionEnzyme> chosenEzymes) {
}
