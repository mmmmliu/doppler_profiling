package com.huanlezhang.dtcdopplerillustrator;

import android.Manifest;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.Arrays;

/**
 * Illustrating Doppler effect
 *
 * @author  Huanle Zhang, University of California, Davis
 *          www.huanlezhang.com
 * @version 0.2
 * @since   2019-07-11
 */

public class MainActivity extends Activity {

    // private static final String TAG = "DTC MainActivity";

    private static final String[] PermissionStrings = {
            Manifest.permission.RECORD_AUDIO
    };
    private static final int Permission_ID = 1;

    private RadioGroup mRadioGroup;

    private boolean mIsSender;

    private ToggleButton mMainToggleBtn;




    private Paint mPaint;
    private Paint mPaintGrey;
    private Paint mPaintRed;
    private Paint mBaseLinePaint;
    // for sender
    PlaySound mPlaySound = new PlaySound();
    private final int[] FREQ_SOUND = new int[]{18500, 19500, 20500};   // emit 19 KHz sounds
    private Bitmap[] mBitmap = new Bitmap[FREQ_SOUND.length];
    private Canvas[] mCanvas = new Canvas[FREQ_SOUND.length];



    private ImageView[] mImageView = new ImageView[FREQ_SOUND.length];
    private int mImageViewWidth;
    private int mImageViewHeight;


    // for receiver
    private Handler[] mHandler = new Handler[FREQ_SOUND.length];

    private Runnable[] mDrawFFTRun = new DrawFFT[FREQ_SOUND.length];

    private AnalyzeFrequency[] mFftAnalysis = new AnalyzeFrequency[FREQ_SOUND.length];
    private final int N_FFT_DOT = 4096;
    private float[] mCurArray = new float[N_FFT_DOT/2-1];
    private static final int FREQ_OFFSET_MAX = 50;  // maximum frequency range

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, PermissionStrings, Permission_ID);

        for(int i = 0; i < FREQ_SOUND.length; i++){
            mHandler[i] = new Handler();
            mDrawFFTRun[i] = new DrawFFT(i);
        }

        mRadioGroup = findViewById(R.id.radioGroup);

        mMainToggleBtn = findViewById(R.id.startToggleBtn);
        mMainToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToggleButton toggleButton = (ToggleButton) v;
                if (toggleButton.isChecked()) {
                    startMain();
                } else {
                    stopMain();
                }
            }
        });

        // set up the imageview

        mImageView[0] = findViewById(R.id.mainImageView1);
        mImageView[1] = findViewById(R.id.mainImageView2);
        mImageView[2] = findViewById(R.id.mainImageView3);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mImageViewWidth = displayMetrics.widthPixels - (int)(getResources().getDisplayMetrics().density*4.0+0.5);
        mImageViewHeight = mImageViewWidth / 2 ; // a square view

        for(int i = 0; i < FREQ_SOUND.length; i++){

            mBitmap[i] = Bitmap.createBitmap(mImageViewWidth, mImageViewHeight, Bitmap.Config.ARGB_4444);
            mCanvas[i] = new Canvas(mBitmap[i]);
            mCanvas[i].drawColor(Color.LTGRAY);
            mImageView[i].setImageBitmap(mBitmap[i]);
            mImageView[i].invalidate();

        }


        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setStrokeWidth(10);

        mPaintGrey = new Paint();
        mPaintGrey.setColor(Color.GRAY);
        mPaintGrey.setStrokeWidth(10);

        mPaintRed = new Paint();
        mPaintRed.setColor(Color.RED);
        mPaintRed.setStrokeWidth(10);


        mBaseLinePaint = new Paint();
        mBaseLinePaint.setColor(Color.BLUE);
        mBaseLinePaint.setStrokeWidth(5);
        TextView maxFreqText1 = findViewById(R.id.maxFreq1);
        maxFreqText1.setText(FREQ_OFFSET_MAX + "cm/s");
        TextView minFreqText1 = findViewById(R.id.minFreq1);
        minFreqText1.setText(-FREQ_OFFSET_MAX + " cm/s");


    }

    void startMain() {
        enableAllUI(false);
        mMainToggleBtn.setEnabled(true);
        int radioBtnId = mRadioGroup.getCheckedRadioButtonId();
        if (radioBtnId == R.id.senderRadio1) {
            // sender
            mIsSender = true;
            mPlaySound = new PlaySound();
            mPlaySound.mOutputFreq = FREQ_SOUND[0];
            mPlaySound.start();
        } else if(radioBtnId == R.id.senderRadio2){
            mIsSender = true;
            mPlaySound = new PlaySound();
            mPlaySound.mOutputFreq = FREQ_SOUND[1];
            mPlaySound.start();
        } else if(radioBtnId == R.id.senderRadio3){
            mIsSender = true;
            mPlaySound = new PlaySound();
            mPlaySound.mOutputFreq = FREQ_SOUND[2];
            mPlaySound.start();
        } else {
            // receiver
            mIsSender = false;
            for(int i = 0; i < FREQ_SOUND.length; i++){
                mFftAnalysis[i] = new AnalyzeFrequency(mHandler[0], mDrawFFTRun[i], FREQ_SOUND[i]);
                mFftAnalysis[i].start();
            }


        }
    }

    void stopMain() {
        enableAllUI(true);
        if (mIsSender) {
            if (mPlaySound != null) {
                mPlaySound.stop();
                mPlaySound = null;
            }
        } else {
            if (mFftAnalysis != null) {
                for(int i = 0; i < FREQ_SOUND.length; i++){
                    if(mFftAnalysis[i] != null){
                        mFftAnalysis[i].stop();
                        mFftAnalysis[i] = null;
                    }

                }
            }
            Arrays.fill(mCurArray, (float) 0.0);
        }
    }

    void enableAllUI(boolean enable) {
        // for radio group
        for (int i = 0; i < mRadioGroup.getChildCount(); i++) {
            mRadioGroup.getChildAt(i).setEnabled(enable);
        }

        mMainToggleBtn.setEnabled(enable);
    }

    // draw doppler on screen
    public class DrawFFT implements Runnable{

        int listIndex;
        DrawFFT(int _index){
            listIndex = _index;
        }
        @Override
        public void run() {
            if (mFftAnalysis[listIndex] == null) {
                return;
            }
            int scaleFFT = (mImageViewHeight/2) / FREQ_OFFSET_MAX;
            int N_CH_DOT = mFftAnalysis[listIndex].N_CH_DOT;
            int fftInterval = (int) (1.0 * mImageViewWidth / N_CH_DOT);
            int fftDisplayRange = fftInterval * N_CH_DOT;
            int index = (mFftAnalysis[listIndex].mChIndex - 1) % N_CH_DOT;

            mCanvas[listIndex].drawColor(Color.LTGRAY);

            // horizontal base line
            mCanvas[listIndex].drawLine(0, mImageViewHeight/2, mImageViewWidth, mImageViewHeight/2, mBaseLinePaint);

            for (int i = fftDisplayRange; i > fftInterval; i -= fftInterval) {
                mCanvas[listIndex].drawLine(i, mImageViewHeight / 2 - scaleFFT * mFftAnalysis[listIndex].mCh[(index + 2 * N_CH_DOT) % N_CH_DOT],
                        i - fftInterval, mImageViewHeight / 2 - scaleFFT * mFftAnalysis[listIndex].mCh[(index + 2 * N_CH_DOT - 1) % N_CH_DOT],
                        mPaintGrey);
                mCanvas[listIndex].drawLine(i, mImageViewHeight / 2 - scaleFFT * mFftAnalysis[listIndex].mChWindowMax[(index + 2 * N_CH_DOT) % N_CH_DOT],
                        i - fftInterval, mImageViewHeight / 2 - scaleFFT * mFftAnalysis[listIndex].mChWindowMax[(index + 2 * N_CH_DOT - 1) % N_CH_DOT],
                        mPaint);
                if(mFftAnalysis[listIndex].mChWindowDetect[(index + 2 * N_CH_DOT) % N_CH_DOT] != 0 && (mFftAnalysis[listIndex].mChWindowDetect[(index + 2 * N_CH_DOT - 1) % N_CH_DOT] != 0)){
                    mCanvas[listIndex].drawLine(i, mImageViewHeight / 2 - scaleFFT * mFftAnalysis[listIndex].mChWindowDetect[(index + 2 * N_CH_DOT) % N_CH_DOT],
                            i - fftInterval, mImageViewHeight / 2 - scaleFFT * mFftAnalysis[listIndex].mChWindowDetect[(index + 2 * N_CH_DOT - 1) % N_CH_DOT],
                            mPaintRed);
                }




                --index;
            }

            int mydata = mFftAnalysis[listIndex].mCh[(mFftAnalysis[listIndex].mChIndex - 1 + 2 * N_CH_DOT) % N_CH_DOT];

            String param = "action=dplSaveData&time=" + System.currentTimeMillis() + "&data=" + mydata;
            //SendDataToServerThread(param);


            mImageView[listIndex].invalidate();
        }
    }
    private void SendDataToServerThread(String param) {
        class OneShotTask implements Runnable {
            String str;
            OneShotTask(String p) { str = p; }
            public void run() {
                try {
                    MyHttpConnection.sendPOST(str);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Thread t = new Thread(new OneShotTask(param));
        t.start();
    }
}
