package com.smartparcelguard.telemetry;

import com.smartparcelguard.device.Device;
import com.smartparcelguard.parcel.Parcel;

public record AuthenticatedDevice(Device device, Parcel parcel) {
}
