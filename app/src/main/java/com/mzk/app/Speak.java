package com.mzk.app;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class Speak {
    static TextToSpeech tts;
    static boolean ready = false;
    static String pending = null;

    public static void say(Context c, String t) {
        try {
            if (tts == null) {
                pending = t;
                tts = new TextToSpeech(c.getApplicationContext(), new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            ready = true;
                            try {
                                tts.setLanguage(new Locale("ar"));
                            } catch (Exception e) {
                            }
                            if (pending != null) {
                                tts.speak(pending, TextToSpeech.QUEUE_FLUSH, null, "mzk");
                                pending = null;
                            }
                        }
                    }
                });
            } else if (ready) {
                tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "mzk");
            }
        } catch (Exception e) {
        }
    }
}
