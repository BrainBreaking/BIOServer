package com.bbs.neuro;

import java.util.EnumSet;

import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NDeviceType;

public class Devices {
	private static Devices instance;
	private NDeviceManager fingerScanners;
	private NDeviceManager irisScanners;
	private NDeviceManager cameras;
	private NDeviceManager palmScanners;
	private NDeviceManager microphones;


	private Devices() {}

	public static Devices getInstance() {
		if (instance == null) {
			instance = new Devices();
		}
		return instance;
	}

	public NDeviceManager getFingerScanners() {
		if (fingerScanners == null) {
			fingerScanners = new NDeviceManager(EnumSet.of(NDeviceType.FSCANNER));
		}
		return fingerScanners;
	}

	public NDeviceManager getIrisScanners() {
		if (irisScanners == null) {
			irisScanners = new NDeviceManager(EnumSet.of(NDeviceType.IRIS_SCANNER));
		}
		return irisScanners;
	}

	public NDeviceManager getCameras() {
		if (cameras == null) {
			cameras = new NDeviceManager(EnumSet.of(NDeviceType.CAMERA));
		}
		return cameras;
	}

	public NDeviceManager getPalmScanners() {
		if (palmScanners == null) {
			palmScanners = new NDeviceManager(EnumSet.of(NDeviceType.PALM_SCANNER));
		}
		return palmScanners;
	}

	public NDeviceManager getMicrophones() {
		if (microphones == null) {
			microphones = new NDeviceManager(EnumSet.of(NDeviceType.MICROPHONE));
		}
		return microphones;
	}

	public void dispose() {
		if (fingerScanners != null) fingerScanners.dispose();
		if (irisScanners != null) irisScanners.dispose();
		if (palmScanners != null) palmScanners.dispose();
		if (cameras != null) cameras.dispose();
		if (microphones != null) microphones.dispose();
	}

}
