package com.lenovo.speech;

public interface TranslatorDataCallback {
	void onResult(String sid, String data);
	void onResult(String sid, byte[] data);
	void onClose(String sid);
}
