package org.prevayler.implementation.snapshot;

import org.prevayler.foundation.serialization.JavaSerializer;
import org.prevayler.foundation.serialization.Serializer;
import org.prevayler.implementation.PrevaylerDirectory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class GenericSnapshotManager {

	private Map _strategies;
	private String _primarySuffix;
	private PrevaylerDirectory _directory;
	private long _recoveredVersion;
	private Object _recoveredPrevalentSystem;

	public GenericSnapshotManager(Serializer serializer, Object newPrevalentSystem, PrevaylerDirectory directory)
			throws IOException, ClassNotFoundException {
		this(serializer, "snapshot", newPrevalentSystem, directory);
	}

	public GenericSnapshotManager(Serializer serializer, String suffix, Object newPrevalentSystem, PrevaylerDirectory directory)
			throws IOException, ClassNotFoundException {
		this(Collections.singletonMap(suffix, serializer), suffix, newPrevalentSystem, directory);
	}

	public GenericSnapshotManager(Map strategies, String primarySuffix, Object newPrevalentSystem, PrevaylerDirectory directory)
			throws IOException, ClassNotFoundException {
		for (Iterator iterator = strategies.keySet().iterator(); iterator.hasNext();) {
			String suffix = (String) iterator.next();
			PrevaylerDirectory.checkValidSnapshotSuffix(suffix);
		}

		if (!strategies.containsKey(primarySuffix)) {
			throw new IllegalArgumentException("Primary suffix '" + primarySuffix + "' does not appear in strategies map");
		}

		_strategies = strategies;
		_primarySuffix = primarySuffix;

		_directory = directory;
		_directory.produceDirectory();

		File latestSnapshot = _directory.latestSnapshot();
		_recoveredVersion = latestSnapshot == null ? 0 : PrevaylerDirectory.snapshotVersion(latestSnapshot);
		_recoveredPrevalentSystem = latestSnapshot == null
				? newPrevalentSystem
				: readSnapshot(latestSnapshot);
	}

	GenericSnapshotManager(Object newPrevalentSystem) {
		_strategies = Collections.singletonMap("snapshot", new JavaSerializer());
		_primarySuffix = "snapshot";
		_directory = null;
		_recoveredVersion = 0;
		_recoveredPrevalentSystem = newPrevalentSystem;
	}


	public Serializer primarySerializer() {
		return (Serializer) _strategies.get(_primarySuffix);
	}

	public Object recoveredPrevalentSystem() {
		return _recoveredPrevalentSystem;
	}

	public long recoveredVersion() {
		return _recoveredVersion;
	}

	public void writeSnapshot(Object prevalentSystem, long version) throws IOException {
		File tempFile = _directory.createTempFile("snapshot" + version + "temp", "generatingSnapshot");

		writeSnapshot(prevalentSystem, tempFile);

		File permanent = snapshotFile(version);
		permanent.delete();
		if (!tempFile.renameTo(permanent)) throw new IOException(
				"Temporary snapshot file generated: " + tempFile + "\nUnable to rename it permanently to: " + permanent);
	}

	private void writeSnapshot(Object prevalentSystem, File snapshotFile) throws IOException {
		OutputStream out = new FileOutputStream(snapshotFile);
		try {
			primarySerializer().writeObject(out, prevalentSystem);
		} finally {
			out.close();
		}
	}


	private File snapshotFile(long version) {
		return _directory.snapshotFile(version, _primarySuffix);
	}

	private Object readSnapshot(File snapshotFile) throws ClassNotFoundException, IOException {
		String suffix = snapshotFile.getName().substring(snapshotFile.getName().indexOf('.') + 1);
		if (!_strategies.containsKey(suffix)) throw new IOException(
				snapshotFile.toString() + " cannot be read; only " + _strategies.keySet().toString() + " supported");

		Serializer serializer = (Serializer) _strategies.get(suffix);
		FileInputStream in = new FileInputStream(snapshotFile);
		try {
			return serializer.readObject(in);
		} finally {
			in.close();
		}
	}

}