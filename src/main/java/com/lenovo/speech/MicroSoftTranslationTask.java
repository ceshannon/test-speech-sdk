package com.lenovo.speech;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.microsoft.cognitiveservices.speech.CancellationReason;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig;
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer;

public class MicroSoftTranslationTask implements TranslationTask, Runnable {
	private static final Logger logger = LoggerFactory.getLogger(MicroSoftTranslationTask.class);
	
	private CountDownLatch latch = null;
	private PushAudioInputStream pushStream = null;
	private TranslationRecognizer recognizer = null;
	private Semaphore stopTranslationWithFileSemaphore;
	private TranslatorDataCallback callback = null;
	private String sid = null, sentenceId = "";
	private String apikey = "";
	private String region = "";
	public MicroSoftTranslationTask(String sid, String apikey, String region, TranslatorDataCallback callback) {
		latch = new CountDownLatch(1);
		pushStream = PushAudioInputStream.create();
		this.callback = callback;
		this.sid = sid;
		sentenceId = UUID.randomUUID().toString();
		stopTranslationWithFileSemaphore = new Semaphore(0);
		this.apikey = apikey;
		this.region = region;
	}

	public void enqueue(byte[] data) {
		try {
			latch.await();
			pushStream.write(data);
			if (data.length == 0) {
				pushStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		String from = "zh-CN";
		String to = "en-US";
		SpeechTranslationConfig config = null;
		try {
			config = SpeechTranslationConfig.fromSubscription(apikey, region);
		} catch (Exception e) {
		}
		config.setSpeechRecognitionLanguage(from);
		config.addTargetLanguage(to);
		AudioConfig audioInput = AudioConfig.fromStreamInput(pushStream);
		recognizer = new TranslationRecognizer(config, audioInput);
		// Subscribes to events.
		recognizer.recognizing.addEventListener((s, e) ->{
			try {
				Map<String, String> map = e.getResult().getTranslations();
				for (String element : map.keySet()) {
					System.out.println("    TRANSLATING into '" + element + "'': " + map.get(element));
				}
				String result = encodeResult(e.getResult().getText(), map.get(to.substring(0, 2)), 0);
				callback.onResult(sid, result);
			} catch (Exception el) {
			}
		});

		recognizer.recognized.addEventListener((s, e) -> {
			try {
			if (e.getResult().getReason() == ResultReason.TranslatedSpeech) {
				System.out.println("RECOGNIZED in '" + from + "': Text=" + e.getResult());
				System.out.println("RECOGNIZED in '" + from + "': Text=" + e.getResult().getText());

				Map<String, String> map = e.getResult().getTranslations();
				for (String element : map.keySet()) {
					System.out.println("    TRANSLATED into '" + element + "'': " + map.get(element));
				}
				String result = encodeResult(e.getResult().getText(), map.get(to.substring(0, 2)), 1);
				callback.onResult(sid, result);
			}
			if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
				System.out.println("RECOGNIZED: Text=" + e.getResult().getText());
				System.out.println("    Speech not translated.");
			} else if (e.getResult().getReason() == ResultReason.NoMatch) {
				System.out.println("NOMATCH: Speech could not be recognized.");
			}
			} catch (Exception el) {
				System.out.println("micro translation event handler error:" + el.getMessage());
			}
		});

		recognizer.canceled.addEventListener((s, e) -> {
			System.out.println("CANCELED: Reason=" + e.getReason());

			if (e.getReason() == CancellationReason.Error) {
				System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
				System.out.println("CANCELED: Did you update the subscription info?");
			}
			destroy();
		});

		recognizer.sessionStarted.addEventListener((s, e) -> {
			System.out.println("\nSession started event.");
			latch.countDown();
		});

		recognizer.sessionStopped.addEventListener((s, e) -> {
			System.out.println("\nSession stopped event.");

			// Stops translation when session stop is detected.
			System.out.println("\nStop translation.");
			destroy();
		});

		// Starts continuous recognition. Uses StopContinuousRecognitionAsync() to stop
		// recognition.
		System.out.println("Start translation...");
		try {
			recognizer.startContinuousRecognitionAsync().get();

			stopTranslationWithFileSemaphore.acquire();
			System.out.println("Stop translation...");
			// Stops translation.
			recognizer.stopContinuousRecognitionAsync().get();
			recognizer.close();
		} catch (InterruptedException e1) {
			System.out.println("translation interrupted..." + e1.getMessage());
		} catch (ExecutionException e1) {
			System.out.println("translation ececution error..." + e1.getMessage());
		} catch (Exception e1) {
			System.out.println("translation unkown error..." + e1.getMessage());
		}
	}
	
	public String encodeResult(String recognition, String translation, int isEnd) {
		JSONObject source = new JSONObject();
		source.put("language", "chinese");
		source.put("text", recognition);
		JSONObject target = new JSONObject();
		target.put("language", "english");
		target.put("text", translation);
		JSONObject ret = new JSONObject();
		ret.put("source", source);
		ret.put("target", target);
		ret.put("eof", isEnd);
		ret.put("sentenceId", sentenceId);
		if (isEnd == 1) {
			sentenceId = UUID.randomUUID().toString();
		}
		return ret.toJSONString();
	}

	@Override
	public void destroy() {
		stopTranslationWithFileSemaphore.release();
//		callback.onClose(sid);
	}

}
