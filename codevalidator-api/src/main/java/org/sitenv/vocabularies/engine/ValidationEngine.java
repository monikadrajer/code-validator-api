package org.sitenv.vocabularies.engine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sitenv.vocabularies.data.Vocabulary;
import org.sitenv.vocabularies.data.VocabularyDataStore;
import org.sitenv.vocabularies.loader.Loader;
import org.sitenv.vocabularies.loader.LoaderManager;
import org.sitenv.vocabularies.watchdog.RepositoryWatchdog;

public class ValidationEngine {
	
	private static Logger logger = Logger.getLogger(ValidationEngine.class);

	public static synchronized boolean validateCode(String vocabulary, String code)
	{
		VocabularyDataStore ds = VocabularyDataStore.getInstance();
		
		if (ds != null && ds.getVocabulariesMap() != null) {
			Map<String, Map<String, Vocabulary>> activeMap = ds.getVocabulariesMap();
			
			if (activeMap != null) {
				Map<String,Vocabulary> vocabularyMap = activeMap.get(vocabulary.toUpperCase());
				
				for (String key : vocabularyMap.keySet()) 
				{
					Vocabulary vocab = vocabularyMap.get(key);
					if (vocab.getCodes().contains(code.toUpperCase()))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public static synchronized boolean validateDisplayName(String vocabulary, String displayName)
	{
		VocabularyDataStore ds = VocabularyDataStore.getInstance();
		
		if (ds != null && ds.getVocabulariesMap() != null) {
			Map<String, Map<String, Vocabulary>> activeMap = ds.getVocabulariesMap();
			
			if (activeMap != null) {
				Map<String,Vocabulary> vocabularyMap = activeMap.get(vocabulary.toUpperCase());
				
				for (String key : vocabularyMap.keySet()) 
				{
					Vocabulary vocab = vocabularyMap.get(key);
					if (vocab.getDisplayNames().contains(displayName.toUpperCase()))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public static RepositoryWatchdog initialize(String directory) throws IOException {
		Map<String, Map<String, Vocabulary>> vocabulariesMap = null;
		boolean recursive = true;

		logger.info("Registering Loaders...");
		// register Loaders
		registerLoaders();
		logger.info("Loaders Registered...");
		
		
		
		logger.info("Starting Watchdog...");
		RepositoryWatchdog watchdog = new RepositoryWatchdog(directory, recursive);
		watchdog.start();
		logger.info("Watchdog started...");
		
		logger.info("Loading vocabularies at: " + directory + "...");
		vocabulariesMap = loadDirectory(directory);
		logger.info("Vocabularies loaded...");
		
		
		// TODO: Perform Validation/Verification, if needed
		
		logger.info("Activating new Vocabularies Map...");
		VocabularyDataStore.getInstance().setVocabulariesMap(vocabulariesMap);
		logger.info("New vocabulary Map Activated...");
		
		return watchdog;
	}
	
	private static void registerLoaders() {
		try {
			Class.forName("org.sitenv.vocabularies.loader.snomed.SnomedLoader");
		} catch (ClassNotFoundException e) {
			// TODO: log4j
			logger.error("Error Initializing Loaders", e);
		}
	}
	
	public static Map<String, Map<String, Vocabulary>> loadDirectory(String directory) throws IOException
	{
		File dir = new File(directory);
		Map<String, Map<String, Vocabulary>> vocabulariesMap = null;
		
		if (dir.isFile())
		{
			logger.debug("Directory to Load is a file and not a directory");
			throw new IOException("Directory to Load is a file and not a directory");
		}
		else
		{
			File[] list = dir.listFiles();
			
			
			for (File file : list)
			{
				if (vocabulariesMap == null)
				{
					vocabulariesMap = new HashMap<String, Map<String, Vocabulary>>();
				}
				loadFiles(file, vocabulariesMap);
			}
		}
		
		return vocabulariesMap;
	}
	
	private static void loadFiles(File directory, Map<String, Map<String, Vocabulary>> vocabulariesMap) throws IOException
	{
		if (directory.isDirectory() && !directory.isHidden()) 
		{
			Map<String, Vocabulary> vocabularyMap = null;
			File[] filesToLoad = directory.listFiles();
			
			for (File loadFile : filesToLoad)
			{
				if (loadFile.isFile() && !loadFile.isHidden())
				{
					if (vocabularyMap == null)
					{
						vocabularyMap = new HashMap<String,Vocabulary>();
					}
					
					logger.debug("Building Loader for directory: " + directory.getName() + "...");
					Loader loader = LoaderManager.getInstance().buildLoader(directory.getName());
					logger.debug("Loader built...");
					
					logger.debug("Loading file: " + loadFile.getAbsolutePath() + "...");
					Vocabulary vocab = loader.load(loadFile);
					vocabularyMap.put(loadFile.getAbsolutePath(), vocab);
					logger.debug("File loaded...");
					
				}
			}
			
			vocabulariesMap.put(directory.getName().toUpperCase(), vocabularyMap);
		}
		
		

	}

}