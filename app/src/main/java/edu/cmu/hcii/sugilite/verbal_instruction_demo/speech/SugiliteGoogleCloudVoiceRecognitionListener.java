package edu.cmu.hcii.sugilite.verbal_instruction_demo.speech;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityService;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * @author toby
 * @date 3/15/19
 * @time 2:56 PM
 */
public class SugiliteGoogleCloudVoiceRecognitionListener implements SugiliteVoiceRecognitionListener {
    private String LOG_TAG = "VoiceRecognition";
    private Context context;
    private long lastStartListening = -1;
    private TextToSpeech tts;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private List<String> contextPhrases = new ArrayList<>();
    private AlertDialog progressDialog;
    private SugiliteGoogleCloudVoiceRecognitionListener sugiliteGoogleCloudVoiceRecognitionListener;

    //initiate voice recorder
    private GoogleVoiceRecorder mVoiceRecorder;
    private GoogleCloudSpeechService mSpeechService;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            // use a service ready callback to ensure that the service is ready
            mSpeechService = GoogleCloudSpeechService.from(binder);

            //notify the wait for the service to be ready
            synchronized (sugiliteGoogleCloudVoiceRecognitionListener) {
                sugiliteGoogleCloudVoiceRecognitionListener.notifyAll();
            }
            // mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };
    private final GoogleVoiceRecorder.Callback mVoiceCallback = new GoogleVoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            if (mSpeechService != null) {
                //update the context phrases
                mSpeechService.setContextPhrases(contextPhrases);
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null) {
                mSpeechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            stopListening();
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }

    };

    @Override
    public void setContextPhrases(String... contextPhrases) {
        this.contextPhrases.clear();
        this.contextPhrases.addAll(Arrays.asList(contextPhrases));
    }

    //the parent interface
    private SugiliteVoiceInterface sugiliteVoiceInterface;

    //this listener is used when results are ready
    private final GoogleCloudSpeechService.Listener mSpeechServiceListener =
            new GoogleCloudSpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        if (mVoiceRecorder != null) {
                            mVoiceRecorder.dismiss();
                        }
                        if (mSpeechService != null && mSpeechServiceListener != null){
                            mSpeechService.removeListener(mSpeechServiceListener);
                        }
                    }
                    if (!TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                    //return the result
                                    List<String> results = new ArrayList<>();
                                    results.add(text);
                                    if (sugiliteVoiceInterface != null) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //this callback is ran on the UI thread because it often involves UI actions
                                                sugiliteVoiceInterface.resultAvailableCallback(results, isFinal);
                                            }
                                        });
                                    }
                                    //TODO: add a progress bar
                            }
                        });
                    }
                }
            };

    public SugiliteGoogleCloudVoiceRecognitionListener(Context context, SugiliteVoiceInterface voiceInterface, TextToSpeech tts) {
        this.context = context;
        this.sugiliteVoiceInterface = voiceInterface;
        this.tts = tts;
        this.sugiliteGoogleCloudVoiceRecognitionListener = this;

        // Prepare Cloud Speech API
        ComponentName service = context.startService(new Intent(context, GoogleCloudSpeechService.class));
        context.bindService(new Intent(context, GoogleCloudSpeechService.class), mServiceConnection, BIND_AUTO_CREATE);

        // Check the permission for recording voices
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            //good to go!
        } else {
            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                Toast.makeText(context, "Need to enable the audio recording permission!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void setSugiliteVoiceInterface(SugiliteVoiceInterface sugiliteVoiceInterface) {
        this.sugiliteVoiceInterface = sugiliteVoiceInterface;
    }

    /**
     * called by the front end when need to start listening
     */
    @Override
    public void startListening() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop(null);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    while (mSpeechService == null) {
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSpeechService.addListener(mSpeechServiceListener);
                        mVoiceRecorder = new GoogleVoiceRecorder(context, mVoiceCallback);
                        mVoiceRecorder.start(new Runnable() {
                            @Override
                            public void run() {
                                showStatus(true);
                            }
                        });
                    }
                });

            }
        });
        t.start();
    }



    /**
     * called by the front end when need to stop listening
     */
    @Override
    public void stopListening(){
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop(new Runnable() {
                @Override
                public void run() {
                    showStatus(false);
                }
            });
        }
    }

    @Override
    public void speak(String content, String utteranceId, Runnable onDone){
        HashMap<String, String> params = new HashMap<String, String>();
        String originalUtteranceId = utteranceId;
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,utteranceId);
        if (tts != null) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (sugiliteVoiceInterface != null) {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (sugiliteVoiceInterface != null) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                sugiliteVoiceInterface.speakingStartedCallback();
                                            }
                                        });
                                    }
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    if (utteranceId.equals(originalUtteranceId)) {
                        try {
                            runOnUiThread(onDone);
                            if (sugiliteVoiceInterface != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        sugiliteVoiceInterface.speakingEndedCallback();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.out.println("TTS IS DONE: " + utteranceId);
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    if (sugiliteVoiceInterface != null) {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sugiliteVoiceInterface.speakingEndedCallback();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            tts.speak(content, TextToSpeech.QUEUE_ADD, params);
        } else {
            System.out.println("ERROR: TTS is null!");
        }
    }


    @Override
    public void stopTTS(){
        if(tts != null) {
            if (tts.isSpeaking()) {
                tts.stop();
                if (sugiliteVoiceInterface != null) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sugiliteVoiceInterface.speakingEndedCallback();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("ERROR: TTS is null!");
        }
    }

    private void runOnUiThread(Runnable runnable) {

        if(context instanceof Activity) {
            ((Activity) context).runOnUiThread(runnable);
        } else if (context instanceof SugiliteAccessibilityService) {
            ((SugiliteAccessibilityService) context).runOnUiThread(runnable);
        }
        else {
            throw new RuntimeException("no context available for running on ui thread");
        }
    }


    /**
     * update the status indicator from sugiliteVoiceInterface based on whether hearing voice or not
     * @param hearingVoice
     */
    private void showStatus(final boolean hearingVoice) {
        //TODO: change the listening indicator status depending on whether the listener is listening or not
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (sugiliteVoiceInterface != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (hearingVoice) {
                                sugiliteVoiceInterface.listeningStartedCallback();
                            } else {
                                sugiliteVoiceInterface.listeningEndedCallback();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void stopAllAndEndASRService() {
        // stop listening to voice
        stopListening();

        // stop TTS
        stopTTS();

        // stop Cloud Speech API
        if (mSpeechServiceListener != null) {
            mSpeechService.removeListener(mSpeechServiceListener);
        }

        if (mServiceConnection != null) {
            context.unbindService(mServiceConnection);
        }

        mSpeechService = null;

    }
}
