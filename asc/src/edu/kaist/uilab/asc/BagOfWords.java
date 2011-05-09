package edu.kaist.uilab.asc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kaist.uilab.asc.asum.StanfordPOSTagger;
import edu.kaist.uilab.asc.util.PorterStemmer;

public class BagOfWords {
	private Object keyToBag = new Object(); // For synchronization
	
	public Vector<String[]> replacePatternList = new Vector<String[]>();
	public Vector<String[]> collapsePatternList = new Vector<String[]>();
	public Vector<String> removeWordPatternList = new Vector<String>();
	
	public TreeSet<Word> stopWords = new TreeSet<Word>();
	public TreeSet<String> stopPOS = new TreeSet<String>();
	
	public String wordDelimiter = "[\\s]+";
	public int minWordLength = 1;
	public int maxWordLength = 1000;
	public int minWordOccur = 1;
	public int minWordDocFreq = 1;
	public int minDocLength = 1;
	public boolean useStemmer = false;
	public boolean usePOSTagger = false;
	public boolean caseSensitive = false;
	
	public Vector<TreeMap<Integer,Integer>> bag;
	private TreeMap<Word,Integer> wordIndex;
	public Vector<String> docList;
	private TreeMap<String, Integer> authorIndex;
	public Vector<Integer> authorIndexForDoc;
	
	StanfordPOSTagger tagger;
	
	public static void main (String [] args) throws Exception {
		String dir = "C:/datasets/asc/BagOfWords-5000-Refined";
		
		BagOfWords bow = new BagOfWords();
//		bow.addReplacePattern("(not|n't|without)[\\s]+(very|so|too|much|quite|even|that|as|as much|a|the|to|really|just)[\\s]+", " not");
//		bow.addReplacePattern("(not|n't|without|no)[\\s]+", " not");
		bow.addReplacePattern("[http|ftp]://[\\S]*", " ");
		bow.addReplacePattern("[?()<>!\\[\\].,~&;:'\"\\-/=*#]", " ");
		bow.removeWordPatternList.add(".*[^a-zA-Z].*");
		bow.minWordLength = 2;
		bow.maxWordLength = 30;
		bow.minWordOccur = 3;
		bow.minDocLength = 20;
		bow.useStemmer = true;
		bow.setStopWords(dir+"/Stopwords.txt");
		
		
		// Read Documents
/*		String [] subDirs = {"pos", "neg"};
		for (String subDir : subDirs) {
			File [] files = new File(dir+subDir+"/").listFiles();
			for (File file : files) {
				String fileName = file.getName();
				BufferedReader fileReader = new BufferedReader(new FileReader(file));
				String document = "", line;
				while ((line = fileReader.readLine()) != null) document += line;
				bow.build(document, fileName);
			}
		}
		bow.filterWords();
		
		BufferedReader fileReader = new BufferedReader(new FileReader(new File(dir+"/"+fileName)));
		String document;
		int cnt = 0;
		while ((document = fileReader.readLine()) != null && !document.equals("")) {
			bow.build(document, String.valueOf(cnt++));
		}
		bow.filterWords();
*/
		
		HashMap<String,String> ratings = new HashMap<String,String>();
		BufferedReader fileReader = new BufferedReader(new FileReader(new File(dir+"/Reviews.txt")));
		String docName;
		while ((docName = fileReader.readLine()) != null && !docName.equals("")) {
			String author = fileReader.readLine();
			ratings.put(docName, fileReader.readLine());
			String document = fileReader.readLine();
			bow.build(document, docName, author);
		}
		bow.filterWords();

		bow.writeOutFiles(dir);
		
		PrintWriter out = new PrintWriter(new FileWriter(new File(dir+"/Polarity.txt")));
		for (String doc : bow.docList) out.println(ratings.get(doc));
		out.close();
	}
	

	public BagOfWords() throws Exception {
		this(false, false);
	}
	
	public BagOfWords(boolean POSTagging, boolean stemming) throws Exception {
		this.usePOSTagger = POSTagging;
		this.useStemmer = stemming;
		initialize();
	}
	
	public void initialize() throws Exception {
		if (this.usePOSTagger) tagger = new StanfordPOSTagger();
		bag = new Vector<TreeMap<Integer,Integer>>();
		wordIndex = new TreeMap<Word,Integer>();
		authorIndexForDoc = new Vector<Integer>();
		authorIndex = new TreeMap<String,Integer>();
	}
	
	public void addReplacePattern(String pattern, String replace) {
		String [] rp = new String[2];
		rp[0] = new String(pattern);
		rp[1] = new String(replace);
		this.replacePatternList.add(rp);
	}
	
	public void addCollapsePattern(String pattern, String collapse) {
		String [] cp = new String[2];
		cp[0] = new String(pattern);
		cp[1] = new String(collapse);
		this.collapsePatternList.add(cp);
	}

	public void setStopWords (String path) throws Exception {
		stopWords.clear();
		BufferedReader stopwordsFile = new BufferedReader(new FileReader(new File(path)));
		String line;
		while ((line = stopwordsFile.readLine()) != null && !line.equals("")) {
//			Matcher m = Pattern.compile("^(.*)/([^/]*)$").matcher(line);
//			if (m.find())
//				stopWords.add(new Word(m.group(1), m.group(2)));
//			else
//				stopWords.add(new Word(line, null));
			stopWords.add(new Word(line, null));
		}
		stopwordsFile.close();
	}
	
	public void setStopPOS(String path) throws Exception {
		stopPOS.clear();
		BufferedReader stopPOSFile = new BufferedReader(new FileReader(new File(path)));
		String line;
		while ((line = stopPOSFile.readLine()) != null) {
			stopPOS.add(line);
		}
		stopPOSFile.close();
	}
	
	public TreeMap<Integer,Integer> build (String document, String documentName) throws Exception {
		return build(document, documentName, null);
	}
	
	public TreeMap<Integer,Integer> build(String document, String documentName, String authorName) throws Exception {
		TreeMap<Word,Integer> wordCount = new TreeMap<Word,Integer>();
		Vector<Word> tmpWordList = new Vector<Word>();
		int numTotalWords = 0;
		
		// Replace patterns in replacePatternList
//		if (document.length() == 1) System.out.println(document);
		for (String [] rp : this.replacePatternList) {
			document = Pattern.compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(document).replaceAll(rp[1]);
		}
		
		// Tokenize
		if (this.usePOSTagger) {
			List<String> taggedSentenceList = tagger.tag(document);
			String taggedDocument = "";
			for (String taggedSentence : taggedSentenceList) {
				taggedDocument += taggedSentence + " ";
			}
			for (String [] cp : this.collapsePatternList) {
				taggedDocument = Pattern.compile(cp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(taggedDocument).replaceAll(cp[1]);
			}
			
			String [] taggedWords = taggedDocument.split(" ");
			for (String taggedWord : taggedWords) {
				if (taggedWord.trim().equals("")) continue;
				Matcher m = Pattern.compile("^(.*)/([^/]+)$").matcher(taggedWord);
				if (m.find())
					tmpWordList.add(new Word(m.group(1).replaceAll("\\/", "/"), m.group(2)));
				else
					System.err.println(taggedDocument);
			}
		}
		else {
			// Replace patterns in replacePatternList
			for (String [] rp : this.replacePatternList) {
				document = Pattern.compile(rp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(document).replaceAll(rp[1]);
			}
			for (String [] cp : this.collapsePatternList) {
				document = Pattern.compile(cp[0], Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(document).replaceAll(cp[1]);
			}

			String [] words = document.split(wordDelimiter);
			for (String word : words) tmpWordList.add(new Word(word));
		}

		
		// Filter
		PorterStemmer stemmer = new PorterStemmer();
		for (Word word : tmpWordList) {
			boolean invalidWord = false;
			
			if (word.value.length() < this.minWordLength || word.value.length() > this.maxWordLength) continue;
			if (!this.caseSensitive) word.value = word.value.toLowerCase();
			for (String removeWordPattern : removeWordPatternList)
				if (Pattern.matches(removeWordPattern, word.value)) { invalidWord = true; break; }
			if (invalidWord) continue;
			
			// Stemming
			if (this.useStemmer) {
				word.value = stemmer.stemming(word.value);
			}

			// Stop POS
			if (this.usePOSTagger) {
				if (stopPOS.contains(word.tag)) continue;
				
				// Change tag
				if (word.tag.startsWith("NN")) word.tag = "NN";
				else if (word.tag.startsWith("JJ") || word.tag.equals("DT")) word.tag = "JJ";
				else if (word.tag.startsWith("RB") || word.tag.equals("RP") || word.tag.equals("LS") || word.tag.equals("UH")) word.tag = "RB";
				else if (word.tag.startsWith("VB")) word.tag = "VB";
				else word.tag = "MISC";
			}
			
			// Stop words
			if (stopWords.contains(word)) continue;

			Integer count = wordCount.get(word);
			if (count == null) wordCount.put(word, 1);
			else wordCount.put(word, count+1);
			
			numTotalWords++;
		}
		
		if (numTotalWords < this.minDocLength) return null;
		
		// Store into the bag
		TreeMap<Integer,Integer> wordIndexCount = new TreeMap<Integer,Integer>();
		synchronized (this.keyToBag) {
			for (Word word : wordCount.keySet()) {
				Integer index = wordIndex.get(word);
				if (index == null) {
					index = getNumUniqueWords();
					wordIndex.put(word, index);
				}
				wordIndexCount.put(index, wordCount.get(word));
			}
			this.bag.add(wordIndexCount);
			this.docList.add(documentName);
			if (authorName != null) {
				Integer index = authorIndex.get(authorName); 
				if (index == null) {
					index = authorIndex.size();
					authorIndex.put(authorName, index);
				}
				this.authorIndexForDoc.add(index);
			}
		}
		return wordIndexCount;
	}
	
	public void filterWords () {
		System.out.println("Filtering Words...");

		TreeSet<Word> removeWords = new TreeSet<Word>();
		TreeSet<Integer> removeWordIndices = new TreeSet<Integer>();
		while (true) {
			int [] wordCount = getWordCount();
			int [] wordDocFreq = getWordDocFreq();
			int cntRemoveWords = 0;
			for (Word word : this.wordIndex.keySet()) {
				if (wordCount[wordIndex.get(word)] < minWordOccur || wordDocFreq[wordIndex.get(word)] < minWordDocFreq) {
					if (!removeWords.contains(word)) {
						removeWords.add(word);
						removeWordIndices.add(wordIndex.get(word));
						cntRemoveWords++;
					}
				}
			}

			if (cntRemoveWords == 0) break;
			
			TreeSet<Integer> removeDocList = new TreeSet<Integer>();
			Vector<TreeMap<Integer,Integer>> newBag = new Vector<TreeMap<Integer,Integer>>();
			for (int d = 0; d < this.bag.size(); d++) {
				TreeMap<Integer,Integer> document = this.bag.get(d);
				TreeMap<Integer,Integer> newDocument = new TreeMap<Integer,Integer>();
				for (Map.Entry<Integer, Integer> entry : document.entrySet()) {
					if (!removeWordIndices.contains(entry.getKey())) newDocument.put(entry.getKey(), entry.getValue());
				}
				if (newDocument.size() >= this.minDocLength) newBag.add(newDocument);
				else removeDocList.add(d);
			}
			this.bag = newBag;
			int numDeleted = 0;
			for (int d : removeDocList) {
				this.docList.removeElementAt(d-numDeleted);
				if (this.authorIndexForDoc.size() > 0) this.authorIndexForDoc.removeElementAt(d-numDeleted);
				numDeleted++;
			}
			assert(this.bag.size() == this.docList.size());
			
		}
		
		for (Word word : removeWords)
			this.wordIndex.remove(word);
		
		if (this.authorIndexForDoc.size() > 0) {
			TreeMap<String,Integer> newAuthorIndex = new TreeMap<String,Integer>();
			String [] authorList = getAuthorList();
			for (int doc = 0; doc < this.authorIndexForDoc.size(); doc++) {
				int index = this.authorIndexForDoc.get(doc);
				String author = authorList[index];
				if (newAuthorIndex.containsKey(author)) continue;
				int newIndex = newAuthorIndex.size();
				newAuthorIndex.put(author, newIndex);
				this.authorIndexForDoc.set(doc, newIndex);
			}
			this.authorIndex = newAuthorIndex;
		}

		sort();
	}
	
	public String [] getWordList () {
		String [] list = new String[getNumUniqueWords()];
		for (Word word : wordIndex.keySet()) {
			if (word.hasTag()) list[wordIndex.get(word)] = word.value+"/"+word.tag;
			else list[wordIndex.get(word)] = word.value;
		}
		return list;
	}
	
	public String [] getAuthorList () {
		String [] list = new String[this.authorIndex.keySet().size()];
		for (String key : this.authorIndex.keySet()) {
			list[this.authorIndex.get(key)] = key;
		}
		return list;
	}
	
	public int [] getWordCount () {
		int [] count = new int[getNumUniqueWords()];
		for (int i = 0; i < count.length; i++) count[i] = 0;
		for (TreeMap<Integer,Integer> document : this.bag) {
			for (Integer wordIndex : document.keySet()) {
				count[wordIndex] += document.get(wordIndex);
			}
		}
		return count;
	}
	
	public int [] getWordDocFreq() {
		int [] count = new int[getNumUniqueWords()];
		for (TreeMap<Integer,Integer> document : this.bag){
			for (Integer wordIndex : document.keySet())
				count[wordIndex]++;
		}
		return count;
	}
	
	public String [] getWordsPOS() {
		String [] pos = new String[getNumUniqueWords()];
		for (Map.Entry<Word,Integer> wordEntry : this.wordIndex.entrySet()) {
			pos[wordEntry.getValue()] = wordEntry.getKey().tag;
		}
		return pos;
	}
	
	public Vector<TreeMap<Integer,Integer>> getBagOfWords () {
		return this.bag;
	}
	
	public int getNumUniqueWords() {
		return this.wordIndex.size();
	}
	
	public int getNumDocs() {
		return this.bag.size();
	}
	
	public void sort () {
		System.out.println("Sorting Words...");
		HashMap<Integer,Integer> indexToIndex = new HashMap<Integer,Integer>();
		int newWordIndex = 0;
		for (Word word : this.wordIndex.keySet()) {
			indexToIndex.put(this.wordIndex.get(word), newWordIndex);
			this.wordIndex.put(word, newWordIndex++);
		}
		int numTotalWords = 0;
		Vector<TreeMap<Integer,Integer>> newBag = new Vector<TreeMap<Integer,Integer>>();
		for (TreeMap<Integer,Integer> wordsFreq : this.bag) {
			TreeMap<Integer,Integer> newWordsFreq = new TreeMap<Integer,Integer>();
			for (Integer oldIndex:wordsFreq.keySet()) {
				Integer newIndex = indexToIndex.get(oldIndex);
				if (newIndex != null) {
					int wordCount = wordsFreq.get(oldIndex);
					newWordsFreq.put(newIndex, wordCount);
					numTotalWords += wordCount;
				}
			}
			newBag.add(newWordsFreq);
		}
		this.bag = newBag;
	}
	
	public void empty () throws Exception {
		initialize();
	}
	
	public String toString() {
		String str = "# Unique Words: "+getNumUniqueWords()+"\n";
		str += "# Documents: "+this.bag.size()+"\n";
		str += "------------------------------------------\n";
		for (int i = 0; i < this.bag.size(); i++) {
			str += i+"";
			TreeMap<Integer,Integer> wordIndexCount = this.bag.get(i);
			for (Integer wordIndex:wordIndexCount.keySet())
				str += "\t"+wordIndex+"("+wordIndexCount.get(wordIndex)+")";
			str += "\n";
		}
		str += "------------------------------------------\n";
		return str;
	}
	
	public void writeOutFiles(String outDir) throws Exception {
		String [] wordList = this.getWordList();
		
		int [] wordCount = this.getWordCount();
		PrintWriter out = new PrintWriter(new FileWriter(new File(outDir+"/WordCount.csv")));
		for (int i = 0; i < wordList.length; i++) out.println("\""+wordList[i].replaceAll("\"", "\"\"")+"\","+wordCount[i]);
		out.close();
	
		out = new PrintWriter(new FileWriter(new File(outDir+"/WordList.txt")));
		for (String word:wordList) out.println(word);
		out.close();
		
		Vector<String> docList = this.docList;
		out = new PrintWriter(new FileWriter(new File(outDir+"/DocumentList.txt")));
		for (String doc : docList) out.println(doc);
		out.close();

		if (!this.authorIndex.isEmpty()) {
			String [] authorList = this.getAuthorList();
			out = new PrintWriter(new FileWriter(new File(outDir+"/AuthorList.txt")));
			for (String author : authorList) out.println(author);
			out.close();
			
			Vector<Integer> authors = this.authorIndexForDoc;
			out = new PrintWriter(new FileWriter(new File(outDir+"/Authors.txt")));
			for (Integer author : authors) out.println(author);
			out.close();
		}
		
		Vector<TreeMap<Integer,Integer>> bagOfWords = this.getBagOfWords();
		out = new PrintWriter(new FileWriter(new File(outDir+"/BagOfWords.txt")));
		for (TreeMap<Integer,Integer> words : bagOfWords) {
			int nTotalWords = 0;
			String str = "";
			for (Integer wordIndex : words.keySet()) {
				int wordCountPerDoc = words.get(wordIndex);
				str += wordIndex+" "+wordCountPerDoc+" ";
				nTotalWords += wordCountPerDoc;
			}
			out.println(words.keySet().size()+" "+nTotalWords+"\n"+str);
		}
		out.close();
	}
	
}
