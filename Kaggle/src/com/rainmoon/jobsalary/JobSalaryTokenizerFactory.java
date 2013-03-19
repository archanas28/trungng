package com.rainmoon.jobsalary;

import java.io.IOException;
import java.util.HashSet;

import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.TokenLengthTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

public class JobSalaryTokenizerFactory {
  static TokenizerFactory factory = null;

  private JobSalaryTokenizerFactory() throws IOException {
    factory = new RegExTokenizerFactory("[\\x2Da-zA-Z0-9]+");
    factory = new LowerCaseTokenizerFactory(factory);
    factory = new EnglishStopTokenizerFactory(factory);
    factory = new PorterStemmerTokenizerFactory(factory);
    factory = new TokenLengthTokenizerFactory(factory, 2, 15);
    factory = new StopTokenizerFactory(factory,
        new HashSet<String>(TextFiles.readUniqueLinesAsLowerCase("stopwords.txt")));
  }

  public static final TokenizerFactory getInstance() throws IOException {
    if (factory == null) {
      new JobSalaryTokenizerFactory();
    }
    return factory;
  }
}
