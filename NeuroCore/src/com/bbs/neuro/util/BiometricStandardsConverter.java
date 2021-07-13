package com.bbs.neuro.util;

import com.bbs.neuro.fingers.FingerTools;
import com.neurotec.biometrics.NEExtractor;
import com.neurotec.biometrics.NEPosition;
import com.neurotec.biometrics.NETemplate;
import com.neurotec.biometrics.NFExtractor;
import com.neurotec.biometrics.NFImpressionType;
import com.neurotec.biometrics.NFPosition;
import com.neurotec.biometrics.NFTemplate;
import com.neurotec.biometrics.NLExtractor;
import com.neurotec.biometrics.NLTemplate;
import com.neurotec.biometrics.NTemplate;
import com.neurotec.biometrics.standards.ANTemplate;
import com.neurotec.biometrics.standards.FCRFaceImage;
import com.neurotec.biometrics.standards.FCRecord;
import com.neurotec.biometrics.standards.FIRFingerView;
import com.neurotec.biometrics.standards.FIRecord;
import com.neurotec.biometrics.standards.FMRecord;
import com.neurotec.biometrics.standards.IIRIrisImage;
import com.neurotec.biometrics.standards.IIRecord;
import com.neurotec.images.NGrayscaleImage;
import com.neurotec.images.NImage;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;
import org.jdesktop.swingworker.SwingWorker;

public final class BiometricStandardsConverter {

	// ===========================================================
	// Nested classes
	// ===========================================================

	private abstract class StandardsWorker extends SwingWorker<Void, Void> {

		private PropertyChangeListener listener;

		public StandardsWorker(PropertyChangeListener listener) {
			super();
			this.listener = listener;
		}

		@Override
		protected Void doInBackground() {
			this.addPropertyChangeListener(listener);
			nTemplate = createNTemplate();
			return (Void) null;
		}

		@Override
		protected void done() {
			try {
				get();
			} catch (InterruptedException e) {
				System.out.println(e); // Log and continue.
			} catch (ExecutionException e) {
				exception = e;
			} finally {
				listener = null;
			}
		}

		protected abstract NTemplate createNTemplate();

	}

	private final class ANTemplateWorker extends StandardsWorker {

		private final ANTemplate anTemplate;

		public ANTemplateWorker(ANTemplate anTemplate, PropertyChangeListener listener) {
			super(listener);
			this.anTemplate = anTemplate;
		}

		@Override
		protected NTemplate createNTemplate() {
			return anTemplate.toNTemplate();
		}

	}

	private final class FMRecordWorker extends StandardsWorker {

		private final FMRecord fmRecord;

		public FMRecordWorker(FMRecord fmRecord, PropertyChangeListener listener) {
			super(listener);
			this.fmRecord = fmRecord;
		}

		@Override
		protected NTemplate createNTemplate() {
			return fmRecord.toNTemplate();
		}

	}

	private final class FIRecordWorker extends StandardsWorker {

		private final FIRecord fiRecord;

		public FIRecordWorker(FIRecord fiRecord, PropertyChangeListener listener) {
			super(listener);
			this.fiRecord = fiRecord;
		}

		@Override
		protected NTemplate createNTemplate() {
			NFTemplate nfTemplate = null;
			NImage image = null;
			NGrayscaleImage grayscaleImage = null;
			try {
				nfTemplate = new NFTemplate();
				for (FIRFingerView fv : fiRecord.getFingerViews()) {
					image = fv.toNImage();
					grayscaleImage = image.toGrayscale();
					NFExtractor.ExtractResult result = FingerTools.getInstance().getExtractor().extract(grayscaleImage, NFPosition.get(fv.getPosition().getValue()), NFImpressionType.get(fv.getImpressionType().getValue()));
					nfTemplate.getRecords().add(result.getRecord().save());
				}
				NTemplate nTemplate = new NTemplate();
				nTemplate.addFingers(nfTemplate.save());
				return nTemplate;
			} finally {
				if (nfTemplate != null) {
					nfTemplate.dispose();
				}
				if (image != null) {
					image.dispose();
				}
				if (grayscaleImage != null) {
					grayscaleImage.dispose();
				}
			}
		}

	}



	private NTemplate nTemplate;
	private ExecutionException exception;

	// ===========================================================
	// Public methods
	// ===========================================================

	public void anRecordToNTemplate(ANTemplate anTemplate, PropertyChangeListener listener) {
		ANTemplateWorker worker = new ANTemplateWorker(anTemplate, listener);
		worker.execute();
	}

	public void fmRecordToNTemplate(FMRecord fmRecord, PropertyChangeListener listener) {
		FMRecordWorker worker = new FMRecordWorker(fmRecord, listener);
		worker.execute();
	}

	public void fiRecordToNTemplate(FIRecord fiRecord, PropertyChangeListener listener) {
		FIRecordWorker worker = new FIRecordWorker(fiRecord, listener);
		worker.execute();
	}

	

	public NTemplate getNTemplate() throws ExecutionException {
		if (exception == null) {
			if (nTemplate == null) {
				throw new IllegalStateException("The converter has not run yet.");
			} else {
				try {
					return (NTemplate) nTemplate.clone();
				} catch (CloneNotSupportedException e) {
					throw new AssertionError(e); // Can't happen;
				}
			}
		} else {
			ExecutionException e = exception;
			exception = null;
			throw e;
		}
	}

}
