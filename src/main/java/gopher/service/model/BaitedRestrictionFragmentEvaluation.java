package gopher.service.model;

import gopher.service.model.viewpoint.Segment;

import java.util.List;
import java.util.function.Predicate;

public class BaitedRestrictionFragmentEvaluation {

    /**
     * If true, then the baits for this restriction fragment are well placed (two baits, both not shifted)
     * and the GC content is not over 50%
     */
    final boolean wellPlacedGcNotHigh;
    /**
     * If true, one side has zero baits
     */
    final boolean unilateralBait;
    /**
     * If true, then one or more sides has a bait with high GC
     */
    final boolean highGc;


    final int n_bait_number;

    final int n_bait_upstream;

    final int n_bait_downstream;


    final int n_downstream_shifted;

    final int n_upstream_shifted;

    final double gcContentUp;

    final double gcContentDown;


    public BaitedRestrictionFragmentEvaluation(Segment segment) {
        this.n_bait_number = segment.getBaitNumTotal();
        this.n_bait_upstream = segment.getBaitNumUp();
        this.n_bait_downstream = segment.getBaitNumDown();
        // check if one or two baits is shifted
        int shifted = 0;
        int downstreamPos = segment.getEndPos();
        for (var bait : segment.getBaitsForDownstreamMargin()) {
            if (bait.getEndPos() != segment.getEndPos()) {
                shifted++;
            }
        }
        n_downstream_shifted = shifted;
        shifted = 0;
        for (var bait : segment.getBaitsForUpstreamMargin()) {
            if (bait.getStartPos() != segment.getStartPos()) {
                shifted++;
            }
        }
        n_upstream_shifted = shifted;
        gcContentDown = segment.getGCcontentDown();
        gcContentUp = segment.getGCcontentUp();

        wellPlacedGcNotHigh = n_bait_upstream > 0 && n_bait_downstream > 0 && n_downstream_shifted == 0 && n_upstream_shifted ==0;
        unilateralBait = n_bait_upstream == 0 || n_bait_downstream == 0;
        highGc = (n_bait_upstream > 0 && gcContentUp > 0.5) || (n_bait_downstream > 0 && gcContentDown > 0.5);
    }

    public boolean isWellPlacedGcNotHigh() {
        return wellPlacedGcNotHigh;
    }

    public boolean isUnilateralBait() {
        return unilateralBait;
    }

    public boolean isShifted() {
        return ! unilateralBait && (n_upstream_shifted>0 || n_downstream_shifted>0);
    }

    public boolean isHighGc() {
        return highGc;
    }


    public static int getGoodQualityFragmentCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::isWellPlacedGcNotHigh)
                .count();
    }



    public static int getUnilateralBaitCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::isUnilateralBait)
                .count();
    }

    public static int getShiftedBaitCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(Predicate.not(BaitedRestrictionFragmentEvaluation::isUnilateralBait))
                .filter(BaitedRestrictionFragmentEvaluation::isShifted)
                .count();
    }

    public static int getHighGcBaitCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(Predicate.not(BaitedRestrictionFragmentEvaluation::isUnilateralBait))
                .filter(Predicate.not(BaitedRestrictionFragmentEvaluation::isShifted))
                .filter(BaitedRestrictionFragmentEvaluation::isHighGc)
                .count();
    }

}
