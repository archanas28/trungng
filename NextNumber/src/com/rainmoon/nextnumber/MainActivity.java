package com.rainmoon.nextnumber;

import java.util.ArrayList;

import org.javia.arity.Complex;
import org.javia.arity.Function;
import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;
import org.javia.arity.Util;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

/*
 * TODO:
 * - add message for lives
 * - revise the engine: too many additions (due to multiplication)
 * - add leader board
 * - add star for doubling the points
 * - add facebook sharing
 * - add game engine/factory
 * - add toggle button for  sound
 * - add clock/timer and score base on these (not now due to interactions)
 * - move calculator code to separate class? it needs to be an activity though?
 */

public class MainActivity extends Activity implements TextWatcher {

  static final String KEY_LEVEL = "level";
  static final String KEY_SCORE = "score";
  static final String KEY_TRIALS = "totalTrials";
  static final String KEY_POSITION = "guessPosition";
  static final String KEY_GAME = "game";
  static final String INFINITY = "Infinity";
  static final String INFINITY_UNICODE = "\u221e";

  static final int MAX_TRIALS = 6;
  static final int DEFAULT_LEVEL = 1;
  // default score 50 so that we can penalize for hint and refreshing
  static final int DEFAULT_SCORE = 50;
  static final int DEFAULT_TRIALS = 1;
  // start guessing after three numbers are more interesting
  // more reward if number guessed correctly early
  static final int DEFAULT_POSITION = 5;

  int mLevel; // current level
  int mScore; // current score
  // total trials for current level including the current one;
  // reset when advances to new level
  int mTotalTrials;
  // position of the number that user has to guess
  int mGuessPosition;
  int[] mGameSequence;

  TextView mLevelView;
  TextView mScoreView;
  TextView mSequence;
  EditText mAnswer;
  TextView mMessage;
  Button mBtnOk;
  ImageButton mHintButton;
  ImageButton mRefreshButton;

  InternalMediaPlayer mMediaPlayer;
  SequencesGenerator mGenerator;

  static Symbols symbols = new Symbols();
  static Function function;

  EditText mResult;
  EditText mInput;
  int nDigits = 0;
  KeyboardView mDigits;
  ArrayList<Function> auxFuncs = new ArrayList<Function>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    restoreGameStates(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupViews();
    mMediaPlayer = new InternalMediaPlayer(this);
    mGenerator = SequencesGenerator.getInstance();
    showGame();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mMediaPlayer.destroy();
  }

  private void restoreGameStates(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      mLevel = savedInstanceState.getInt(KEY_LEVEL, DEFAULT_LEVEL);
      mScore = savedInstanceState.getInt(KEY_SCORE, DEFAULT_SCORE);
      mTotalTrials = savedInstanceState.getInt(KEY_TRIALS, DEFAULT_TRIALS);
      mGuessPosition = savedInstanceState
          .getInt(KEY_POSITION, DEFAULT_POSITION);
      mGameSequence = savedInstanceState.getIntArray(KEY_GAME);
    } else {
      mLevel = DEFAULT_LEVEL;
      mScore = DEFAULT_SCORE;
      mTotalTrials = DEFAULT_TRIALS;
      mGuessPosition = DEFAULT_POSITION;
    }
  }

  private void setupViews() {
    mLevelView = (TextView) findViewById(R.id.levelNumber);
    mScoreView = (TextView) findViewById(R.id.txtScore);
    mSequence = (TextView) findViewById(R.id.txtSequence);
    mMessage = (TextView) findViewById(R.id.txtMessage);
    mAnswer = (EditText) findViewById(R.id.txtAnswer);
    mBtnOk = (Button) findViewById(R.id.btnOK);
    mBtnOk.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        String answer = mAnswer.getText().toString();
        if (answer.length() > 0) {
          hideKeyboard();
          int result = 0;
          try {
            result = Integer.parseInt(answer);
          } catch (NumberFormatException e) {
            result = 0;
          }

          // game result
          if (result == mGameSequence[mGuessPosition]) {
            mMediaPlayer.playSound(R.raw.correct);
            updateScoreForCorrectAnswer();
            // advance to next level
            mLevel = mLevel + 1;
            mGuessPosition = DEFAULT_POSITION;
            mGameSequence = mGenerator.getGame(mLevel);
            clearText();
            showGame();
          } else {
            mMediaPlayer.playSound(R.raw.wrong);
            mTotalTrials = mTotalTrials + 1;
            if (mTotalTrials > MAX_TRIALS) {
              mMessage.setText(String.format("%s : %d",
                  getString(R.string.correct_answer),
                  mGameSequence[mGuessPosition]));
              mMessage.setVisibility(View.VISIBLE);
              // show score and leaderboard
              // new game?
            }
          }
        }
      }
    });

    mHintButton = (ImageButton) findViewById(R.id.btnHint);
    mHintButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mGuessPosition == SequencesGenerator.SEQ_LENGTH - 1) {
          // no hint for the last number
          mMessage.setText(R.string.no_hint);
          mMessage.setVisibility(View.VISIBLE);
        } else {
          mGuessPosition = mGuessPosition + 1;
          mScore = mScore - mScore / 4;
          showGame();
        }
      }
    });
    mRefreshButton = (ImageButton) findViewById(R.id.btnRefresh);
    mRefreshButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mScore = mScore / 2;
        mGuessPosition = DEFAULT_POSITION;
        mGameSequence = mGenerator.getGame(mLevel);
        showGame();
      }
    });

    // calculator
    mDigits = (KeyboardView) findViewById(R.id.digits);
    mDigits.init(KeyboardView.DIGITS, true, true);

    mInput = (EditText) findViewById(R.id.txtInput);
    mInput.addTextChangedListener(this);
    mInput.setEditableFactory(new CalculatorEditable.Factory());
    mInput.setInputType(0);
    mInput.requestFocus();
    mResult = (EditText) findViewById(R.id.txtResult);
  }

  // compute score for correct answer
  void updateScoreForCorrectAnswer() {
    mScore = mScore + mLevel * (100 / (mGuessPosition - DEFAULT_POSITION + 1));
  }

  void hideKeyboard() {
    InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
        InputMethodManager.HIDE_NOT_ALWAYS);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putInt(KEY_LEVEL, mLevel);
    outState.putInt(KEY_SCORE, mScore);
    outState.putInt(KEY_TRIALS, mTotalTrials);
    outState.putInt(KEY_POSITION, mGuessPosition);
    outState.putIntArray(KEY_GAME, mGameSequence);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  /**
   * Shows game.
   */
  void showGame() {
    mMessage.setVisibility(View.INVISIBLE);
    // show level and score
    mLevelView.setText(String.valueOf(mLevel));
    mScoreView.setText(String.valueOf(mScore));
    if (mGameSequence == null) {
      mGameSequence = mGenerator.getGame(mLevel);
    }
    mSequence.setText(Utils.getString(mGameSequence, mGuessPosition,
        SequencesGenerator.SEQ_LENGTH / 2 - 1));
  }

  // TextWatcher
  public void afterTextChanged(Editable s) {
    if (mInput != null && mInput.getText().length() > 0) {
      evaluate(mInput.getText().toString());
      mAnswer.setText(mResult.getText().toString());
    }
  }

  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
  }

  public void onTextChanged(CharSequence s, int start, int before, int count) {
  }

  static void log(String mes) {
    if (true) {
      Log.d("Calculator", mes);
    }
  }

  private String formatEval(Complex value) {
    if (nDigits == 0) {
      nDigits = getResultSpace();
    }
    String res = Util.complexToString(value, nDigits, 2);
    return res.replace(INFINITY, INFINITY_UNICODE);
  }

  private void evaluate(String text) {
    // log("evaluate " + text);
    if (text.length() == 0) {
      mResult.setEnabled(false);
      return;
    }

    auxFuncs.clear();
    int end = -1;
    do {
      text = text.substring(end + 1);
      end = text.indexOf(';');
      String slice = end == -1 ? text : text.substring(0, end);
      try {
        Function f = symbols.compile(slice);
        auxFuncs.add(f);
      } catch (SyntaxException e) {
        continue;
      }
    } while (end != -1);

    int size = auxFuncs.size();
    if (size == 0) {
      mResult.setEnabled(false);
      return;
    } else if (size == 1) {
      Function f = auxFuncs.get(0);
      int arity = f.arity();
      // Calculator.log("res " + f);
      if (arity == 1 || arity == 2) {
        mResult.setText(null);
      } else if (arity == 0) {
        mResult.setText(formatEval(f.evalComplex()));
        mResult.setEnabled(true);
      } else {
        mResult.setText("function");
        mResult.setEnabled(true);
      }
    }
  }

  private int getResultSpace() {
    int width = mResult.getWidth() - mResult.getTotalPaddingLeft()
        - mResult.getTotalPaddingRight();
    float oneDigitWidth = mResult.getPaint().measureText("5555555555") / 10f;
    return (int) (width / oneDigitWidth);
  }

  private StringBuilder oneChar = new StringBuilder(" ");

  void onKey(char key) {
    if (key == 'C') {
      clearText();
    } else {
      int cursor = mInput.getSelectionStart();
      oneChar.setCharAt(0, key);
      mInput.getText().insert(cursor, oneChar);
    }
  }

  void clearText() {
    mInput.setText("");
    mResult.setText("");
    mAnswer.setText("");
  }

}
