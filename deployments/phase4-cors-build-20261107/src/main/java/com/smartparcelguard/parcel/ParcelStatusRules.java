package com.smartparcelguard.parcel;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ParcelStatusRules {

    private static final Map<ParcelStatus, Set<ParcelStatus>> OPERATOR_TRANSITIONS = Map.of(
            ParcelStatus.REGISTERED, EnumSet.of(ParcelStatus.READY_FOR_PICKUP),
            ParcelStatus.READY_FOR_PICKUP, EnumSet.of(ParcelStatus.PICKED_UP),
            ParcelStatus.PICKED_UP, EnumSet.of(ParcelStatus.IN_TRANSIT),
            ParcelStatus.IN_TRANSIT, EnumSet.of(ParcelStatus.DELAYED, ParcelStatus.UNDER_INVESTIGATION, ParcelStatus.OUT_FOR_DELIVERY),
            ParcelStatus.DELAYED, EnumSet.of(ParcelStatus.IN_TRANSIT, ParcelStatus.UNDER_INVESTIGATION),
            ParcelStatus.UNDER_INVESTIGATION, EnumSet.of(ParcelStatus.IN_TRANSIT, ParcelStatus.RETURNED),
            ParcelStatus.OUT_FOR_DELIVERY, EnumSet.of(ParcelStatus.DELIVERED, ParcelStatus.RETURNED)
    );

    private ParcelStatusRules() {
    }

    public static boolean operatorCanChange(ParcelStatus from, ParcelStatus to) {
        return OPERATOR_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static Set<ParcelStatus> allowedForOperator(ParcelStatus from) {
        return OPERATOR_TRANSITIONS.getOrDefault(from, Set.of());
    }
}
