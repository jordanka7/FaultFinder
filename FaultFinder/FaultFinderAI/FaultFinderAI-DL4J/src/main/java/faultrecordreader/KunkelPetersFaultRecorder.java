package faultrecordreader;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.datavec.api.conf.Configuration;
import org.datavec.api.records.Record;
import org.datavec.api.records.listener.RecordListener;
import org.datavec.api.records.metadata.RecordMetaData;
import org.datavec.api.records.metadata.RecordMetaDataURI;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.split.InputSplit;
import org.datavec.api.writable.IntWritable;
import org.datavec.api.writable.NDArrayWritable;
import org.datavec.api.writable.Writable;

import clasDC.factories.FaultFactory;
import clasDC.faults.Fault;
import clasDC.faults.FaultNames;
import strategies.FaultRecordScalerStrategy;
import strategies.MinMaxStrategy;
import utils.FaultUtils;

public class KunkelPetersFaultRecorder implements RecordReader {

	protected FaultFactory factory = null;
	private int label;
	// args for FaultFactory constructor
	private int superLayer;
	private int maxFaults;
	private FaultNames desiredFault;
	private boolean singleFaultGeneration;
	private boolean blurredFaults;
	private int nchannels;
	private boolean conv3D;
	private boolean isMultiLabeled;

	public KunkelPetersFaultRecorder(int superLayer, int maxFaults, FaultNames desiredFault,
			boolean singleFaultGeneration, boolean blurredFaults, int nchannels) {
		// this(superLayer, maxFaults, desiredFault, singleFaultGeneration,
		// false, nchannels, false, false);

		this.superLayer = superLayer;
		this.maxFaults = maxFaults;
		this.desiredFault = desiredFault;
		this.singleFaultGeneration = singleFaultGeneration;
		this.blurredFaults = blurredFaults;
		this.nchannels = nchannels;
		// this.isMultiLabeled = isMultiLabeled;
		this.factory = new FaultFactory(superLayer, maxFaults, desiredFault, singleFaultGeneration, blurredFaults,
				nchannels);
	}

	// public KunkelPetersFaultRecorder(CLASObject clasObject) {
	// if (clasObject instanceof SuperLayer) {
	// SuperLayer obj = (SuperLayer) clasObject;
	// this.factory = new FaultFactory(obj.getSuperlayer(), obj.getMaxFaults(),
	// obj.getDesiredFault(),
	// obj.isSingleFaultGen(), true, obj.getNchannels());
	// } else {
	// throw new IllegalArgumentException("Only superlayer object is valid");
	// }
	//
	// }

	@Override
	public boolean batchesSupported() {
		// I might want to set this to true so that I train in batches, reduces
		// memory
		// will get back to this after impl of modes
		return true;
	}

	@Override
	public boolean hasNext() {
		// since this is batch mode, and the data is generated on the fly, this
		// should always be true
		return true;
	}

	@Override
	public List<Writable> next() {
		List<Writable> ret = new ArrayList<>();
		// ret.add(new NDArrayWritable(factory.getFeatureVector()));
		if (conv3D) {
			ret.add(new NDArrayWritable(factory.asImageMatrix(2).getImage()));
		} else {
			ret.add(new NDArrayWritable(factory.asImageMatrix().getImage()));
		}

		// ret.add(new
		// NDArrayWritable(factory.asUnShapedImageMatrix().getImage()));

		ret.add(new IntWritable(getLabelInt(factory.getFaultLabel())));

		this.reset();
		return ret;
	}

	@Override
	public void reset() {
		this.factory = new FaultFactory(this.superLayer, this.maxFaults, this.desiredFault, this.singleFaultGeneration,
				this.blurredFaults, this.nchannels);
	}

	@Override
	public boolean resetSupported() {
		// Why would we need to reset in this type of training?
		return true;
	}

	@Override
	public List<List<Writable>> next(int batch) {
		List<List<Writable>> ret = new ArrayList<>();
		for (int i = 0; i < batch && hasNext(); i++) {
			ret.add(next());
		}
		return ret;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Configuration getConf() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setConf(Configuration arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getLabels() {
		List<String> labels = new ArrayList<>();
		for (Fault faults : factory.getFaultList()) {
			labels.add(faults.getFaultName());
		}
		return null;
	}

	@Override
	public List<RecordListener> getListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(InputSplit arg0) throws IOException, InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public void initialize(Configuration arg0, InputSplit arg1) throws IOException, InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public Record loadFromMetaData(RecordMetaData arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Record> loadFromMetaData(List<RecordMetaData> arg0) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Record nextRecord() {
		List<Writable> list = next();
		URI uri = URI.create("FaultFinder");
		// return new org.datavec.api.records.impl.Record(list, metaData)
		return new org.datavec.api.records.impl.Record(list,
				new RecordMetaDataURI(null, KunkelPetersFaultRecorder.class));

	}

	@Override
	public List<Writable> record(URI arg0, DataInputStream arg1) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setListeners(RecordListener... arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setListeners(Collection<RecordListener> arg0) {
		// TODO Auto-generated method stub

	}

	protected int getLabelInt(int[] labels) {
		for (int i = 0; i < labels.length; i++) {
			if (labels[i] == 1) {
				this.label = i;
				return i;
			}
		}
		this.label = 0;
		return 0;
	}

	public int getLabelInt() {
		return this.label;
	}

	public void initialize() {

	}

	private void test(int i) {

		FaultUtils.draw(factory.asImageMatrix().getImage(), " Plot " + i);

		System.out.println(getLabelInt(factory.getFaultLabel()) + "   for " + i);

		this.reset();

	}

	public static void main(String[] args) {

		FaultRecordScalerStrategy strategy = new MinMaxStrategy();
		KunkelPetersFaultRecorder recordReader = new KunkelPetersFaultRecorder(2, 20, FaultNames.PIN_BIG, true, true,
				1);
		for (int i = 0; i < 10; i++) {
			recordReader.test(i);

		}
	}
}
