package gopher.service.model;

import gopher.service.model.viewpoint.Segment;

import java.util.List;

/**
 * This class evaluates each baited restriction fragment with respect to unilateral baits, shifted baits, and
 * high GC content, and also provides a method that reports of the restriction fragment has high-quality bilateral
 * baits that do not have a too high GC content: {@link #wellPlacedHighQuality}.
 * @author  Peter N Robinson
 */
public class BaitedRestrictionFragmentEvaluation {

    /**
     * If true, then the baits for this restriction fragment are well placed (two baits, both not shifted)
     * and the GC content is not over 50%
     */
    final boolean wellPlacedHighQuality;
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
        int downstreamPos = segment.getEndPos();
        n_downstream_shifted = (int)  segment.getBaitsForDownstreamMargin()
                .stream()
                .filter(bait -> bait.getEndPos() != downstreamPos)
                .count();
        int upstreamPos = segment.getStartPos();
        n_upstream_shifted = (int) segment.getBaitsForUpstreamMargin()
                .stream()
                .filter(bait -> bait.getStartPos() != upstreamPos)
                .count();
        gcContentDown = segment.getGCcontentDown();
        gcContentUp = segment.getGCcontentUp();
        unilateralBait = n_bait_upstream == 0 || n_bait_downstream == 0;
        highGc = (n_bait_upstream > 0 && gcContentUp > 0.5) || (n_bait_downstream > 0 && gcContentDown > 0.5);
        wellPlacedHighQuality = (! unilateralBait) &&
                (n_downstream_shifted + n_upstream_shifted == 0) &&
                (! highGc);
    }

    public boolean isWellPlacedHighQuality() {
        return wellPlacedHighQuality;
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
                .filter(BaitedRestrictionFragmentEvaluation::isWellPlacedHighQuality)
                .count();
    }



    public static int getUnilateralBaitCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::isUnilateralBait)
                .count();
    }

    public static int getShiftedBaitCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::isShifted)
                .count();
    }

    public static int getHighGcBaitCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::isHighGc)
                .count();
    }

}
