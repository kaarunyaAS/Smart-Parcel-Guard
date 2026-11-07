package com.smartparcelguard.parcel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParcelStatusRulesTest {

    @Test
    void allowsTheNextOperationalStatus() {
        assertTrue(ParcelStatusRules.operatorCanChange(ParcelStatus.REGISTERED, ParcelStatus.READY_FOR_PICKUP));
        assertTrue(ParcelStatusRules.operatorCanChange(ParcelStatus.OUT_FOR_DELIVERY, ParcelStatus.DELIVERED));
    }

    @Test
    void blocksSkippingTheDeliveryFlow() {
        assertFalse(ParcelStatusRules.operatorCanChange(ParcelStatus.REGISTERED, ParcelStatus.DELIVERED));
        assertFalse(ParcelStatusRules.operatorCanChange(ParcelStatus.DELIVERED, ParcelStatus.IN_TRANSIT));
    }
}
