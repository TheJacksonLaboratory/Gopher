package gopher.service.model;

import gopher.service.model.viewpoint.Segment;

import java.util.List;

/**
 * This class evaluates each baited restriction fragment with respect to unilateral baits, shifted baits, and
 * @author  Peter N Robinson
 */
public class BaitedRestrictionFragmentEvaluation {


    final int n_bait_number;

    final int n_bait_upstream;

    final int n_bait_downstream;


    final int n_downstream_shifted;

    final int n_upstream_shifted;

    final boolean bilateral;

    final boolean shifted;

    public BaitedRestrictionFragmentEvaluation(Segment segment) {
        this.n_bait_number = segment.getBaitNumTotal();
        this.n_bait_upstream = segment.getBaitNumUp();
        this.n_bait_downstream = segment.getBaitNumDown();
        this.bilateral = this.n_bait_upstream > 0 && this.n_bait_downstream > 0;
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
        // do we have AT LEAST ONE unshofted bait upstream (Note -- GOPHER can place one unshoifted and one shifted
        boolean unshiftedUpstream  = n_upstream_shifted < n_bait_upstream;
        boolean unshiftedDownstream =  n_downstream_shifted < n_bait_downstream;
        this.shifted = bilateral &&   ! (  unshiftedUpstream  &&  unshiftedDownstream  );
    }



    public boolean isBilateralWellPlacedBaitedFragment() {
        return bilateral && (! shifted);
    }


    public boolean isBilateral() {
        return bilateral;
    }

    public boolean isUnilateral() {
        return ! bilateral;
    }


    public boolean hasZeroBait() {
        return this.n_bait_number == 0;
    }

    public boolean isShifted() {
       return shifted;
    }




    public static int getWellPlacedBaitedFragmentCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::isBilateralWellPlacedBaitedFragment)
                .count();
    }



    public static int getUnilateralBaitCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::isUnilateral)
                .count();
    }

    public static int getShiftedBaitCount(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::isShifted)
                .count();
    }

    public static int getCountOfFragmentsWithZeroBaits(List<BaitedRestrictionFragmentEvaluation> fragments) {
        return (int) fragments.stream()
                .filter(BaitedRestrictionFragmentEvaluation::hasZeroBait)
                .count();
    }
}
