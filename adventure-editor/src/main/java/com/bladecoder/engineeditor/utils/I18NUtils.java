/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.Properties;

import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engineeditor.model.Project;

public class I18NUtils {
	private static final String SEPARATOR = "\t";
	private static final String TSV_EXT = ".tsv";
	private static final String PROPERTIES_EXT = ".properties";

	public static final void exportTSV(String projectPath, final String chapterId, String defaultLocale)
			throws FileNotFoundException, IOException {
		String modelPath = projectPath + Project.MODEL_PATH;
		File defaultChapter = new File(modelPath, chapterId + PROPERTIES_EXT);
		File outputFile = new File(modelPath, chapterId + TSV_EXT);

		// 1. Find all chapter properties
		File[] files = new File(modelPath).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				if (!arg1.endsWith(PROPERTIES_EXT) || !arg1.startsWith(chapterId + "_"))
					return false;

				return true;
			}
		});

		Properties props[] = new Properties[files.length + 1];

		props[0] = new Properties();
		props[0].load(new InputStreamReader(new FileInputStream(defaultChapter), I18N.ENCODING));

		for (int i = 1; i < props.length; i++) {
			props[i] = new Properties();
			props[i].load(new InputStreamReader(new FileInputStream(files[i - 1]), I18N.ENCODING));
		}

		// WRITE THE OUTPUT FILE
		BufferedWriter writer = null;

		writer = new BufferedWriter(new FileWriter(outputFile));

		String lang = defaultLocale;

		writer.write("KEY");

		// write header
		for (int i = 0; i < props.length; i++) {
			if (i != 0)
				lang = files[i - 1].getName().substring(files[i - 1].getName().lastIndexOf('_') + 1,
						files[i - 1].getName().lastIndexOf('.'));

			writer.write(SEPARATOR + lang);
		}

		writer.write("\n");

		for (Object key : props[0].keySet()) {
			writer.write((String) key);

			for (Properties p : props) {
				writer.write(SEPARATOR + p.getProperty((String) key, ""));
			}

			writer.write("\n");
		}

		writer.close();
	}

	public static final void importTSV(String projectPath, String chapterId, String defaultLocale)
			throws FileNotFoundException, IOException {
		String modelPath = projectPath + Project.MODEL_PATH;
		File inputFile = new File(modelPath, chapterId + TSV_EXT);

		try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
			// get header
			String line = br.readLine();

			if (line != null) {
				String[] langs = line.split(SEPARATOR);
				Properties props[] = new Properties[langs.length - 1];

				for (int i = 0; i < props.length; i++) {
					props[i] = new Properties();
				}

				// get keys and texts
				while ((line = br.readLine()) != null) {
					String[] values = line.split(SEPARATOR);
					String key = values[0];

					for (int i = 0; i < props.length; i++) {
						props[i].setProperty(key, values[i + 1]);
					}
				}

				// save properties
				for (int i = 0; i < props.length; i++) {

					String i18nFilename;

					if (i == 0) {
						i18nFilename = modelPath + "/" + chapterId + PROPERTIES_EXT;
					} else {
						i18nFilename = modelPath + "/" + chapterId + langs[i + 1] + PROPERTIES_EXT;
					}

					FileOutputStream os = new FileOutputStream(i18nFilename);
					Writer out = new OutputStreamWriter(os, I18N.ENCODING);
					props[i].store(out, chapterId);
				}
			}
		}
	}

	public static final void newLocale(String projectPath, final String chapterId, String defaultLocale,
			String newLocale) throws FileNotFoundException, IOException {
		String modelPath = projectPath + Project.MODEL_PATH;
		File defaultChapter = new File(modelPath, chapterId + PROPERTIES_EXT);
		File newChapter = new File(modelPath, chapterId + "_" + newLocale + PROPERTIES_EXT);

		Properties defaultProp = new Properties();
		Properties newProp = new Properties();

		defaultProp.load(new InputStreamReader(new FileInputStream(defaultChapter), I18N.ENCODING));

		for (Object key : defaultProp.keySet()) {
			newProp.setProperty((String) key, translatePhrase((String) defaultProp.get(key), defaultLocale, newLocale));
		}

		// save new .properties
		FileOutputStream os = new FileOutputStream(newChapter);
		Writer out = new OutputStreamWriter(os, I18N.ENCODING);
		newProp.store(out, chapterId);
	}

	public static final String translatePhrase(String phrase, String sourceLangCode, String destLangCode) throws UnsupportedEncodingException {
		// String query = MessageFormat.format(GOOGLE_TRANSLATE_URL, phrase,
		// sourceLangCode, destLangCode);
//		String query = GOOGLE_TRANSLATE_URL + "?q=" + phrase + "&source=" + sourceLangCode + "&target=" + destLangCode
//				+ "&key=" + GOOGLE_API_KEY;
		
		String query =  "https://translate.googleapis.com/translate_a/single?client=gtx&sl=" 
				+ sourceLangCode + "&tl=" + destLangCode + "&dt=t&q=" + URLEncoder.encode(phrase, "UTF-8");

		System.out.println(query);
		String result = HttpUtils.excuteHTTP(query, null);

		int idx1 = result.indexOf('"');
		int idx2 = result.substring(idx1 + 1).indexOf('"');

		String translatedText = result.substring(idx1 + 1, idx2);
		System.out.println("> TRANSLATED: " + translatedText);

		return translatedText;
	}

	public static void usage() {
		System.out.println("Usage:\n" + "\tI18NUtils tsv2properties project_path chapter_id default_locale"
				+ "\tI18NUtils properties2tsv project_path chapter_id default_locale"
				+ "\tI18NUtils newlocale project_path chapter_id default_locale new_locale");
	}

	public static final void main(String[] args) throws FileNotFoundException, IOException {

		if (args[0].equals("tsv2properties")) {
			if (args.length != 4) {
				usage();
				System.exit(-1);
			}

			importTSV(args[1], args[2], args[3]);
			System.out.println("Properties generated sucessfully.");
		} else if (args[0].equals("properties2tsv")) {
			if (args.length != 4) {
				usage();
				System.exit(-1);
			}

			exportTSV(args[1], args[2], args[3]);
			System.out.println(".tsv file generated sucessfully.");
		} else if (args[0].equals("newlocale")) {
			if (args.length != 5) {
				usage();
				System.exit(-1);
			}

			newLocale(args[1], args[2], args[3], args[4]);
			System.out.println(args[2] + "_" + args[4] + PROPERTIES_EXT + " generated sucessfully.");
		}
	}
}
