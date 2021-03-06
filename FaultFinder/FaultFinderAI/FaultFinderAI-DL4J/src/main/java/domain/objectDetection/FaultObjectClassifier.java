package domain.objectDetection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.datavec.api.records.reader.RecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import clasDC.faults.FaultNames;
import clasDC.objects.CLASObject.ContainerType;
import faultrecordreader.FaultRecorderScaler;
import strategies.FaultRecordScalerStrategy;

public class FaultObjectClassifier {

	/** The network that is used as the underlying model. */
	private ComputationGraph network;
	private FaultObjectContainer container;

	/**
	 * Initializes the FaultClassifier using a saved model.
	 *
	 * @param fileName
	 *            The file that the model is loaded from.
	 */
	public FaultObjectClassifier(String fileName) throws IOException {
		InputStream is = getClass().getResourceAsStream(fileName);
		this.network = ModelSerializer.restoreComputationGraph(is);

	}

	public FaultObjectClassifier(String fileName, FaultObjectContainer container) throws IOException {
		this.container = container;
		InputStream is = getClass().getResourceAsStream(fileName);

		if (container.getClasObject().getContainerType().equals(ContainerType.CLASS)) {
			this.network = ModelSerializer.restoreMultiLayerNetwork(is).toComputationGraph();
		} else {
			this.network = ModelSerializer.restoreComputationGraph(is);
		}

	}

	public FaultObjectClassifier(String fileName, ContainerType container) throws IOException {
		InputStream is = getClass().getResourceAsStream(fileName);
		// System.out.println(fileName);
		// System.out.println(is);
		if (container.equals(ContainerType.CLASS)) {
			this.network = ModelSerializer.restoreMultiLayerNetwork(is).toComputationGraph();
		} else {
			this.network = ModelSerializer.restoreComputationGraph(is);
		}

		// || container.equals(ContainerType.MULTICLASS)
	}

	/**
	 * Initializes the FaultClassifier using a readily configured network.
	 *
	 * @param network
	 *            The network that serves as the underlying model of
	 *            FaultClassifier.
	 */
	public FaultObjectClassifier(ComputationGraph network) {
		this.network = network;
	}

	/**
	 * Initializes the FaultClassifier using a readily configured network.<br>
	 * Using the FaultObjectContainer
	 *
	 * @param network
	 *            The network that serves as the underlying model of
	 *            FaultClassifier.
	 */
	public FaultObjectClassifier(FaultObjectContainer container) {
		this.container = container;
		this.network = container.getModel();
	}

	/**
	 * Trains the underlying model.
	 * 
	 * @param numLabels
	 *            The number of classification labels
	 * @param batchSize
	 *            The size of each batch.
	 * @param batchNum
	 *            The amount of batches to pass through during training.
	 * @param epochs
	 *            The number of epochs to train
	 * @param recordReader
	 *            The FaultRecordReader to be used.
	 */

	public void train(int numLabels, int batchSize, int batchNum, int epochs, RecordReader recordReader,
			FaultRecordScalerStrategy strategy) {
		// set up the DatasetIterator
		DataSetIterator iterator = null;

		if (container.getClasObject().getContainerType().equals(ContainerType.CLASS)) {
			iterator = new RecordReaderDataSetIterator.Builder(recordReader, batchSize).classification(1, numLabels)
					.maxNumBatches(batchNum).preProcessor(new FaultRecorderScaler(strategy)).build();
		} else if (container.getClasObject().getContainerType().equals(ContainerType.OBJ)) {
			FaultNames desiredFault = container.getClasObject().getDesiredFault();
			if (desiredFault.equals(FaultNames.PIN_SMALL) || desiredFault.equals(FaultNames.PIN_BIG)
					|| desiredFault.equals(FaultNames.DEADWIRE) || desiredFault.equals(FaultNames.HOTWIRE)) {

				iterator = new RecordReaderDataSetIterator.Builder(recordReader, batchSize).regression(1)
						.maxNumBatches(batchNum).preProcessor(new FaultRecorderScaler(strategy)).build();
			} else {
				iterator = new RecordReaderDataSetIterator.Builder(recordReader, batchSize).classification(1, numLabels)
						.maxNumBatches(batchNum).preProcessor(new FaultRecorderScaler(strategy)).build();
				System.out.println(iterator.toString());
			}
		} else if (container.getClasObject().getContainerType().equals(ContainerType.MULTICLASS)) {
			iterator = new RecordReaderDataSetIterator.Builder(recordReader, batchSize).regression(1)
					.maxNumBatches(batchNum).preProcessor(new FaultRecorderScaler(strategy)).build();
		} else if (container.getClasObject().getContainerType().equals(ContainerType.SEG)) {
			iterator = new RecordReaderDataSetIterator.Builder(recordReader, batchSize).regression(1)
					.maxNumBatches(batchNum).preProcessor(new FaultRecorderScaler(strategy)).build();
		} else {
			throw new IllegalArgumentException("Object Type not known " + container.getClasObject().getContainerType());
		}
		// this trains the model on batchNum batches for the desired number of
		// epochs
		this.network.fit(iterator, epochs);
		iterator.reset();
	}

	/**
	 * Saves the underlying model to a file. The current state of the updater is
	 * not saved.
	 *
	 * @param fileName
	 *            The file that the model is saved to.
	 */
	public void save(String fileName) throws IOException {
		ModelSerializer.writeModel(this.network, new File(fileName), false);
	}

	/**
	 * Evaluates the model.
	 *
	 * @param batchSize
	 *            The size of each batch.
	 * @param batchNum
	 *            The amount of batches to pass through during evaluation.
	 * @param recordReader
	 *            The FaultRecordReader to be used.
	 *
	 * @return The results of the Evaluation.
	 */
	public Evaluation evaluate(int batchSize, int batchNum, RecordReader recordReader,
			FaultRecordScalerStrategy strategy) {
		// set up the DatasetIterator
		DataSetIterator iterator = new RecordReaderDataSetIterator.Builder(recordReader, batchSize)
				// currently there are 14 labels in the dataset
				.classification(1, 14).maxNumBatches(batchNum).preProcessor(new FaultRecorderScaler(strategy)).build();

		// perform the testing
		Evaluation evaluation = this.network.evaluate(iterator);

		// return the results
		return evaluation;
	}

	public Evaluation evaluate(int numLabels, int batchSize, int batchNum, RecordReader recordReader,
			FaultRecordScalerStrategy strategy) {
		// set up the DatasetIterator
		DataSetIterator iterator = new RecordReaderDataSetIterator.Builder(recordReader, batchSize)
				// currently there are 14 labels in the dataset
				.classification(1, numLabels).maxNumBatches(batchNum).preProcessor(new FaultRecorderScaler(strategy))
				.build();

		// perform the testing
		Evaluation evaluation = this.network.evaluate(iterator);

		// return the results
		return evaluation;
	}

	public INDArray evaluateNew(int batchSize, int batchNum, RecordReader recordReader,
			FaultRecordScalerStrategy strategy) {
		// set up the DatasetIterator
		DataSetIterator iterator = new RecordReaderDataSetIterator.Builder(recordReader, batchSize).regression(1)
				.maxNumBatches(batchNum).preProcessor(new FaultRecorderScaler(strategy)).build();

		// perform the testing
		INDArray evaluation = this.network.evaluate(iterator);

		// return the results
		return evaluation;
	}

	/**
	 * Set the listeners for the model. Use this to monitor training.
	 */
	public void setListeners(TrainingListener... listeners) {
		this.network.setListeners(listeners);
	}

	public Collection<TrainingListener> geTrainingListeners() {
		// return this.network.getTrainingListeners();
		return this.network.getListeners();
	}

	// public int[] predict(INDArray d) {
	// return this.network.predict(d);
	// }

	public INDArray output(INDArray d) {
		return this.network.outputSingle(d);
	}

	public INDArray[] outputArray(INDArray d) {
		return this.network.output(d);
	}

	public ComputationGraph getModel() {
		return this.network;
	}
}
